// Created by Sumit Shah on 7/02/09.
// Copyright (c) 2010 Yahoo! Inc. All rights reserved.
//
// The copyrights embodied in the content of this file are licensed under the BSD (revised) open source license.
package com.yahoo.yos;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.testng.annotations.Test;
import org.mockito.Mockito;

public class YahooFilterTest {

    @Test
    public void testDefaultInitNoRedirect() throws Exception {
        YahooFilter filter = new YahooFilter();
        FilterConfig filterConfig = Mockito.mock(FilterConfig.class);
        Mockito.when(filterConfig.getInitParameter("redirect")).thenReturn("false");
        filter.init(filterConfig);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        Mockito.verify(req).setAttribute(Mockito.eq("yahooSession"), Mockito.any(YahooSession.class));
    }

    @Test
    public void testHttpClient3InitNoRedirect() throws Exception {
        YahooFilter filter = new YahooFilter();
        FilterConfig filterConfig = Mockito.mock(FilterConfig.class);
        Mockito.when(filterConfig.getInitParameter("oauthConnectionClass")).thenReturn("net.oauth.client.httpclient3.HttpClient3");
        Mockito.when(filterConfig.getInitParameter("redirect")).thenReturn("false");
        filter.init(filterConfig);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        Mockito.verify(req).setAttribute(Mockito.eq("yahooSession"), Mockito.any(YahooSession.class));
    }

    @Test
    public void testHttpClient4InitNoRedirect() throws Exception {
        YahooFilter filter = new YahooFilter();
        FilterConfig filterConfig = Mockito.mock(FilterConfig.class);
        Mockito.when(filterConfig.getInitParameter("oauthConnectionClass")).thenReturn("net.oauth.client.httpclient4.HttpClient4");
        Mockito.when(filterConfig.getInitParameter("redirect")).thenReturn("false");
        filter.init(filterConfig);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        Mockito.verify(req).setAttribute(Mockito.eq("yahooSession"), Mockito.any(YahooSession.class));
    }
}
