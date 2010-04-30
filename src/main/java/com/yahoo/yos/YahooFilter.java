// Created by Sumit Shah on 7/02/09.
// Copyright (c) 2010 Yahoo! Inc. All rights reserved.
//
// The copyrights embodied in the content of this file are licensed under the BSD (revised) open source license.
package com.yahoo.yos;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpClient;
import net.oauth.server.OAuthServlet;
import net.oauth.signature.OAuthSignatureMethod;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is a servlet filter that assists in OAuth validation and authorization as well 
 * as some boilerplate when dealing with Yahoo! Open APIs.  Access and request tokens
 * are stored in cookies in a similar manner to the PHP SDK.
 *
 * @author Sam Pullara
 * @author Sumit Shah
 */
public class YahooFilter implements Filter {
    private final static Logger logger = LoggerFactory.getLogger(YahooFilter.class);

    private static enum SESSION_TYPE {
        YAHOO_YAP_SESSION_TYPE,
        YAHOO_OAUTH_AT_SESSION_TYPE,
        YAHOO_OAUTH_RT_SESSION_TYPE,
    }

    private Properties oauthConfig;
    private boolean redirect = false;
    private OAuthClient client;
    private OAuthServiceProvider provider;
    private OAuthConsumer consumer;
    private String callbackUrl;

    /**
     * Set the 'oauth' init parameter for this filter to change from the default
     * oauth.properties resource for configuration. The file should define:
     * <p/>
     * yos.consumerKey=...
     * yos.consumerSecret=...
     * oauth.requesttoken.url=https://api.login.yahoo.com/oauth/v2/get_request_token
     * oauth.requestauth.url=https://api.login.yahoo.com/oauth/v2/request_auth
     * oauth.accesstoken.url=https://api.login.yahoo.com/oauth/v2/get_token
     * oauth.callback.url=http://myapplication:8080/
     * <p/>
     * To access production OAuth services from Yahoo.  This filter relies on session
     * information stored by cookies to operate.
     *
     * @param filterConfig see upstream docs
     * @throws ServletException
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        String filename = filterConfig.getInitParameter("oauth");
        if (filename == null) {
            filename = "oauth.properties";
        }
        logger.debug("oauth properties file: {}", filename);
        oauthConfig = new Properties();
        try {
            oauthConfig.load(getClass().getResourceAsStream("/" + filename));
        } catch (IOException e) {
            throw new ServletException("Could not load oauth properties from resource: " + filename, e);
        }
        String redirectString = filterConfig.getInitParameter("redirect");
        // defaults to redirect if null, zero-length
        redirect = redirectString == null
                || redirectString.trim().length() <= 0
                || "true".equalsIgnoreCase(redirectString.trim());
        logger.debug("redirect if access token not found: {}", redirect);
        String oauthConnectionClass = filterConfig.getInitParameter("oauthConnectionClass");
        if (oauthConnectionClass == null) {
            oauthConnectionClass = "net.oauth.client.URLConnectionClient";
        }
        logger.debug("oauth client connection class: {}", oauthConnectionClass);
        try {
            client = new OAuthClient((HttpClient)Class.forName(oauthConnectionClass).newInstance());
        } catch (Exception cce) {
            throw new ServletException("unable to create OAuthClient from: " + oauthConnectionClass, cce);
        }
        provider = new OAuthServiceProvider(
                oauthConfig.getProperty("oauth.requesttoken.url", "https://api.login.yahoo.com/oauth/v2/get_request_token"),
                oauthConfig.getProperty("oauth.requestauth.url", "https://api.login.yahoo.com/oauth/v2/request_auth"),
                oauthConfig.getProperty("oauth.accesstoken.url", "https://api.login.yahoo.com/oauth/v2/get_token")
        );
        consumer = new OAuthConsumer(oauthConfig.getProperty("oauth.callback.url"), oauthConfig.getProperty("yos.consumerKey"), oauthConfig.getProperty("yos.consumerSecret"), provider);
        consumer.setProperty("oauth_signature_method", oauthConfig.getProperty("yos.oauth_signature_method", "HMAC-SHA1"));
        callbackUrl = oauthConfig.getProperty("oauth.callback.url", "");
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String yap_appid = getParam(request, "yap_appid");
        SESSION_TYPE sessionType;
        if ("POST".equals(request.getMethod()) && yap_appid != null && yap_appid.length() > 0) {
            sessionType = SESSION_TYPE.YAHOO_YAP_SESSION_TYPE;
        } else if (cookieExists(request.getCookies(), "yosdk_at")) {
            sessionType = SESSION_TYPE.YAHOO_OAUTH_AT_SESSION_TYPE;
        } else if (cookieExists(request.getCookies(), "yosdk_rt")) {
            sessionType = SESSION_TYPE.YAHOO_OAUTH_RT_SESSION_TYPE;
        } else {
            sessionType = null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("sessionType: {}", sessionType);
        }

        OAuthAccessor accessor = new OAuthAccessor(consumer);

        if (sessionType == null) {
            if (redirect) {
                if (logger.isDebugEnabled()) {
                    logger.debug("redirecting user to yahoo acquire access token");
                }
                redirectForAuthorization(accessor, request, response);
                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("inserting YahooSession suitable for 2-legged oauth calls into request attribute");
                }
                String appId = oauthConfig.getProperty("yos.appid");
                request.setAttribute("yahooSession", new YahooSession(client, consumer, null, appId));
            }
        } else if (sessionType == SESSION_TYPE.YAHOO_YAP_SESSION_TYPE) {
            if (logger.isDebugEnabled()) {
                logger.debug("inserting YahooSession suitable for 2-legged oauth calls into request attribute");
            }
            if (consumer.consumerKey == null || !consumer.consumerKey.equals(getParam(request, "yap_consumer_key"))) {
                logger.error("Consumer key from YAP does not match config.");
                clearSession(request, response);
                if (redirect) {
                    redirectForAuthorization(accessor, request, response);
                    return;
                }
            }
            try {
                OAuthSignatureMethod method = OAuthSignatureMethod.newMethod("HMAC-SHA1", accessor);
                OAuthMessage msg = OAuthServlet.getMessage(request, null);
                method.validate(msg);
            } catch (OAuthProblemException ex) {
                logger.error("Signature from YAP failed.", ex);
                clearSession(request, response);
                if (redirect) {
                    redirectForAuthorization(accessor, request, response);
                    return;
                }
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
            AccessToken at = new AccessToken();
            at.setKey(getParam(request, "yap_viewer_access_token"));
            at.setSecret(getParam(request, "yap_viewer_access_token_secret"));
            at.setGuid(getParam(request, "yap_viewer_guid"));
            at.setOwner(getParam(request, "yap_owner_guid"));
            at.setTokenExpires(-1);
            String appId = getParam(request, "yap_appid");
            YahooSession yahooSession = new YahooSession(client, consumer, at, appId);
            request.setAttribute("yahooSession", yahooSession);
        } else if (sessionType == SESSION_TYPE.YAHOO_OAUTH_AT_SESSION_TYPE) {
            long now = System.currentTimeMillis() / 1000;
            try {
                AccessToken accessToken = new AccessToken(cookie(request.getCookies(), "yosdk_at"));
                if (consumer.consumerKey == null || !consumer.consumerKey.equals(accessToken.getConsumer())) {
                    logger.error("Consumer key for token does not match the defined Consumer Key.  The Consumer Key has probably changed since the user last authorized the application.");
                    clearSession(request, response);
                    if (redirect) {
                        redirectForAuthorization(accessor, request, response);
                        return;
                    }
                }
                if (accessToken.getTokenExpires() >= 0 && logger.isDebugEnabled()) {
                    logger.debug("AT Expires in: {}", (accessToken.getTokenExpires() - now));
                }
                if (accessToken.getTokenExpires() >= 0 && (accessToken.getTokenExpires() - now) < 30) {
                    try {
                        accessTokenExpired(accessor, request, response, accessToken, filterChain);
                    } catch (OAuthException ex) {
                        if (ex instanceof OAuthProblemException) {
                            OAuthProblemException oape = (OAuthProblemException) ex;
                            String s = oape.getProblem() + oape.getParameters();
                            throw new ServletException(s, ex);
                        }
                        throw new ServletException(ex);
                    } catch (URISyntaxException ex) {
                        throw new ServletException(ex);
                    }
                    return;
                } else {
                    String appId = oauthConfig.getProperty("yos.appid");
                    YahooSession yahooSession = new YahooSession(client, consumer, accessToken, appId);
                    request.setAttribute("yahooSession", yahooSession);
                }
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        } else if (sessionType == SESSION_TYPE.YAHOO_OAUTH_RT_SESSION_TYPE) {
            try {
                RequestToken rt = new RequestToken(cookie(request.getCookies(), "yosdk_rt"));
                accessor.tokenSecret = rt.getSecret();

                String verifier = getParam(request, "oauth_verifier");
                if (logger.isDebugEnabled()) {
                  logger.debug("got oauth_verifier {}", verifier);
                }

                try {
                    if(logger.isDebugEnabled()) {
                        logger.error("request token found, fetching access token for user");
                    }
                    AccessToken at = fetchAccessToken(accessor, rt, verifier);
                    Cookie yosdk_at = at.getCookie();
                    Cookie yosdk_rt = new Cookie("yosdk_rt", "");
                    yosdk_at.setMaxAge(30 * 24 * 60 * 60);
                    yosdk_rt.setMaxAge(0);
                    response.addCookie(yosdk_at);
                    response.addCookie(yosdk_rt);
                    String appId = oauthConfig.getProperty("yos.appid");
                    YahooSession yahooSession = new YahooSession(client, consumer, at, appId);
                    request.setAttribute("yahooSession", yahooSession);
                } catch (URISyntaxException ex) {
                    throw new ServletException(ex);
                } catch (OAuthException ex) {
                    clearSession(request, response);
                    if (redirect) {
                        redirectForAuthorization(accessor, request, response);
                        return;
                    } else {
                        throw new ServletException(ex);
                    }
                }
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getParam(HttpServletRequest request, String key) {
        String param = request.getParameter(key);
        if (param == null) {
            return request.getHeader("x-" + key.replace("_", "-"));
        }
        return param;
    }

    private AccessToken fetchAccessToken(OAuthAccessor accessor, RequestToken requestToken, String verifier) throws IOException, URISyntaxException, OAuthException {
        List<OAuth.Parameter> params;
        if (requestToken.getSessionHandle() != null) {
            params = OAuth.newList("oauth_token", requestToken.getKey(), "oauth_session_handle", requestToken.getSessionHandle());
        } else {
            params = OAuth.newList("oauth_token", requestToken.getKey());
        }
        // Add the verifier which is required for OAuth1.0a
        if (verifier != null) {
          params.addAll(OAuth.newList("oauth_verifier", verifier));
        }
        OAuthMessage getTokenMsg = new OAuthMessage("GET", provider.accessTokenURL,  params);
        getTokenMsg.addRequiredParameters(accessor);
        OAuthMessage msg = client.invoke(getTokenMsg, ParameterStyle.QUERY_STRING);
        Map<String, String> map = OAuth.newMap(msg.getParameters());
        AccessToken at = new AccessToken();
        at.setKey(map.get("oauth_token"));
        at.setSecret(map.get("oauth_token_secret"));
        at.setGuid(map.get("xoauth_yahoo_guid"));
        at.setConsumer(accessor.consumer.consumerKey);
        at.setSessionHandle(map.get("oauth_session_handle"));
        long now = System.currentTimeMillis() / 1000;
        if (map.containsKey("oauth_expires_in")) {
            at.setTokenExpires(now + Long.parseLong(msg.getParameter("oauth_expires_in")));
        } else {
            at.setTokenExpires(-1);
        }
        if (map.containsKey("oauth_authorization_expires_in")) {
            at.setHandleExpires(now + Long.parseLong(map.get("oauth_authorization_expires_in")));
        } else {
            at.setHandleExpires(-1);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("setting access token expires in: {}", at.getTokenExpires());
            logger.debug("setting access token handle expires in: {}", at.getHandleExpires());
        }
        return at;
    }

    private void accessTokenExpired(OAuthAccessor accessor, HttpServletRequest request, HttpServletResponse response, AccessToken accessToken, FilterChain filterChain) throws IOException, ServletException, JSONException, OAuthException, URISyntaxException {
        if(logger.isDebugEnabled()) {
            logger.debug("access token expired, attempting to renew");
        }
        long now = System.currentTimeMillis() / 1000;
        if (accessToken.getHandleExpires() == -1 || (now < accessToken.getHandleExpires())) {
            RequestToken requestToken = new RequestToken();
            requestToken.setKey(accessToken.getKey());
            requestToken.setSessionHandle(accessToken.getSessionHandle());
            accessor.tokenSecret = accessToken.getSecret();
            AccessToken at = fetchAccessToken(accessor, requestToken, null);
            Cookie yosdk_at = at.getCookie();
            yosdk_at.setMaxAge(30 * 24 * 60 * 60);
            response.addCookie(yosdk_at);
            String appId = oauthConfig.getProperty("yos.appid");
            YahooSession yahooSession = new YahooSession(client, consumer, at, appId);
            request.setAttribute("yahooSession", yahooSession);
            filterChain.doFilter(request, response);
        } else {
            Cookie at = new Cookie("yosdk_at", "");
            at.setMaxAge(0);
            at.setMaxAge(0);
            response.addCookie(at);
            request.setAttribute("yahooSession", null);
            request.setAttribute("yahooRedirect", null);
            filterChain.doFilter(request, response);
            if (redirect) {
                redirectForAuthorization(accessor, request, response);
            }
        }
    }

    private void redirectForAuthorization(OAuthAccessor accessor, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            // get the request token
            List<OAuth.Parameter> callback = OAuth.newList(OAuth.OAUTH_CALLBACK, callbackUrl);
            //client.getRequestToken(accessor, null, callback);
            OAuthMessage message = client.getRequestTokenResponse(accessor, null, callback);
        } catch (URISyntaxException ex) {
            throw new ServletException(ex);
        } catch (OAuthException ex) {
            throw new ServletException(ex);
        }
        if (accessor.requestToken != null) {
            try {
                RequestToken rt = new RequestToken();
                rt.setKey(accessor.requestToken);
                rt.setSecret(accessor.tokenSecret);
                Cookie yosdk_rt = rt.getCookie();
                yosdk_rt.setMaxAge(600);
                response.addCookie(yosdk_rt);
            } catch (JSONException ex) {
                throw new ServletException(ex);
            }
        } else {
            throw new ServletException("Failed to create request token");
        }
        String redirectUrl = OAuth.addParameters(provider.userAuthorizationURL,
                "oauth_token", accessor.requestToken,
                "oauth_callback", callbackUrl);
        request.setAttribute("yahooRedirect", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    public void clearSession(HttpServletRequest req, HttpServletResponse res) {
        if(logger.isDebugEnabled()) {
            logger.debug("clear session requested");
        }
        Cookie at = new Cookie("yosdk_at", "");
        at.setMaxAge(0);
        Cookie rt = new Cookie("yosdk_rt", "");
        rt.setMaxAge(0);
        res.addCookie(at);
        res.addCookie(rt);
        req.setAttribute("yahooSession", null);
        req.setAttribute("yahooRedirect", null);
    }

    private boolean cookieExists(Cookie[] cookies, String cookieName) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName() != null && cookie.getName().equals(cookieName)) {
                    return true;
                }
            }
        }
        return false;
    }


    private Cookie cookie(Cookie[] cookies, String cookieName) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName() != null && cookie.getName().equals(cookieName)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public void destroy() {
        //empty
    }
}
