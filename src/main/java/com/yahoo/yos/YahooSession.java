// Created by Sumit Shah on 7/02/09.
// Copyright (c) 2010 Yahoo! Inc. All rights reserved.
//
// The copyrights embodied in the content of this file are licensed under the BSD (revised) open source license.
package com.yahoo.yos;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Required properties:
 * yos.consumerKey     - your OAuth consumer key
 * yos.consumerSecret  - your OAuth consumer secret
 * <p/>
 * Optional properties:
 * yos.appid   - if you need the AppID for an API call
 * oauth.*.url - if you are using different end points than the standard YOS endpoints
 *
 * @author Sam Pullara
 * @author Sumit Shah
 */
public class YahooSession implements Serializable {

    private static final long serialVersionUID = 6409879252229261080L;
    private final static Logger logger = LoggerFactory.getLogger(YahooSession.class);
    private AccessToken accessToken;
    private String applicationId;
    private OAuthClient client;
    private OAuthAccessor accessor;

    public YahooSession(OAuthClient client, OAuthConsumer consumer, AccessToken accessToken, String applicationId) {
        this.accessToken = accessToken;
        this.applicationId = applicationId;
        this.client = client;
        this.accessor = new OAuthAccessor(consumer);
        if (accessToken != null) {
            accessor.accessToken = accessToken.getKey();
            accessor.tokenSecret = accessToken.getSecret();
        }
    }

    public InputStream invokeStream(String url, String httpMethod, String... params) throws IOException, OAuthException, URISyntaxException {
        return invoke(url, httpMethod, params).getBodyAsStream();
    }

    public String invokeString(String url, String httpMethod, String... params) throws IOException, OAuthException, URISyntaxException {
        return invoke(url, httpMethod, params).readBodyAsString();
    }

    public InputStream invokeStreamWithBody(String url, String httpMethod, String body, String... params) throws IOException, OAuthException, URISyntaxException {
        return invoke(url, httpMethod, body, params).getBodyAsStream();
    }

    public String invokeStringWithBody(String url, String httpMethod, String body, String... params) throws IOException, OAuthException, URISyntaxException {
        return invoke(url, httpMethod, body, params).readBodyAsString();
    }

    private OAuthMessage invoke(String url, String httpMethod, String... params) throws IOException, OAuthException, URISyntaxException {
        return client.invoke(accessor, httpMethod, url, OAuth.newList(params));
    }

    private OAuthMessage invoke(String url, String httpMethod, final String body, String... params) throws IOException, OAuthException, URISyntaxException {
        OAuthMessage msg = new OAuthMessage(httpMethod, url, OAuth.newList(params)) {

            @Override
            public InputStream getBodyAsStream() throws IOException {
                return new ByteArrayInputStream(body.getBytes("utf-8"));
            }
        };
        msg.addRequiredParameters(accessor);
        try {
            return client.invoke(msg, ParameterStyle.QUERY_STRING);
        } catch (OAuthException oe) {
            return client.invoke(msg, ParameterStyle.AUTHORIZATION_HEADER);
        }
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("clear session requested");
        }
        Cookie at = new Cookie("yosdk_at", "");
        at.setMaxAge(0);
        Cookie rt = new Cookie("yosdk_rt", "");
        rt.setMaxAge(0);
        response.addCookie(at);
        response.addCookie(rt);
        request.setAttribute("yahooSession", null);
    }

    /**
     * Returns the current users GUID.  There is a one to one mapping of the
     * GUID to a yahoo account, e.g. foo@yahoo.com or bar@ymail.com.  Returns null
     * if the sdk is used to only act upon behalf of the application rather than
     * the user.  See 2-legged versus 3-legged calls.
     *
     * @return the GUID
     */
    public String getGUID() {
        if (accessToken != null) {
            return accessToken.getGuid();
        } else {
            return null;
        }
    }

    /**
     * Returns the appid specified in the properties file.
     *
     * @return the appid
     */
    public String getAppID() {
        return applicationId;
    }
}
