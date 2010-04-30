To obtain keys for your YOS application please visit:

http://developer.yahoo.com/dashboard/

================================================================================

To build:
Invoke mvn package

================================================================================


Quick start:

1) Copy yossdk-0.1.0.jar, and dependent jars to WEB-INF/lib
2) Add com.yahoo.yos.oauth.YahooFilter servlet filter to web.xml, for example:

<!DOCTYPE web-app PUBLIC
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
        "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
    <display-name>sample</display-name>
    <filter>
        <filter-name>YOSFilter</filter-name>
        <filter-class>com.yahoo.yos.YahooFilter</filter-class>

        <!-- 
        optional param -
        underlying oauth client class
        possible values:
            net.oauth.client.URLConnectionClient (default)
            net.oauth.client.httpclient3.HttpClient3
            net.oauth.client.httpclient4.HttpClient4
        -->
        <init-param>
            <param-name>oauthConnectionClass</param-name>
            <param-value>net.oauth.client.httpclient4.HttpClient4</param-value>
        </init-param>
        <!-- 
        optional param -
        redirect end-user if an access token is not found, set to false if you 
        are only making two-legged oauth calls e.g. oauth calls without an 
        access token to retrieve public information
        defauts to true
        -->
        <init-param>
            <param-name>redirect</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>

    <!--
    The URL where the filter is mapped to will redirect the user to Yahoo for
    authorization if an OAuth authorization token has not been obtained for the
    user.  Should correspond to your callback url
    -->

    <filter-mapping>
        <filter-name>YOSFilter</filter-name>
        <url-pattern>/login.jsp</url-pattern>
    </filter-mapping>
</web-app>

3) Sample JSP:

<%@ page import="com.yahoo.yos.YahooSession" %><%@ page import="net.oauth.OAuthProblemException" %><%@ page import="org.json.JSONObject" %><%
    YahooSession state = (YahooSession) request.getAttribute("yahooSession");
    if ("POST".equals(request.getMethod())) {
        String format = request.getParameter("format");
        try {
            if ("yql".equals(request.getParameter("form"))) {
                String method = request.getParameter("method");
                String url = request.getParameter("url");
                String callback = request.getParameter("callback");
                String query = request.getParameter("query");

                String[] params = null;
                if ("xml".equals(format)) {
                    response.setContentType("text/xml");
                    params = new String[] {"q", query, "format", format};
                } else {
                    response.setContentType("text/plain");
                    params = new String[] {"q", query, "format", format, "callback", callback};
                }
                String output = state.invokeString(url, method, params);
                out.println(output);
            } else if ("yap".equals(request.getParameter("form"))) {
                String method = request.getParameter("method");
                String url = request.getParameter("url");
                String content = request.getParameter("content");
                String[] params = {"format", format, "content", content};
                if ("xml".equals(format)) {
                    response.setContentType("text/xml");
                    out.println(state.invokeStringWithBody(url, method, content, params));
                } else {
                    response.setContentType("text/plain");
                    JSONObject object = new JSONObject(state.invokeStringWithBody(url, method, content, params));
                    out.println("JSON Object:");
                    out.println(object.toString(10));
                }
            } else {
                out.println("<html><body>unknown form</body></html>");
            }
        } catch (Exception e) {
            response.setContentType("text/plain");
            e.printStackTrace();
        }
    } else if ("GET".equals(request.getMethod()) && "true".equals(request.getParameter("clear"))) {
        state.clearSession(request, response);
    } else {
%>
<html>
<body>
<h1>YAP:</h1>

<a href="./test.jsp?clear=true">logout</a>

<br/>

<form name="yap" method="post" action="test.jsp">
    <input type="hidden" name="form" value="yap"/>
    Method:
    <select name="method">
        <option>GET</option>
        <option selected="true">PUT</option>
        <option>POST</option>
    </select>
    <br/>
    URL: <input name="url" type="text" size="75"
                value="http://social.yahooapis.com/v1/user/<%= state.getGUID() %>/presence/presence"/>
    <br/>
    Content: <textarea name="content" rows="10" cols="50">
    {
    "status": "Reading paper"
    }
</textarea>
    <br/>
    Format:
    <select name="format">
        <option>xml</option>
        <option>json</option>
    </select>
    <br/>
    <input type="submit"/>
</form>

<h1>YQL:</h1>

<form name="yql" method="post" action="test.jsp">
    <input type="hidden" name="form" value="yql"/>
    Method:
    <select name="method">
        <option>GET</option>
        <option>PUT</option>
        <option>POST</option>
    </select>
    <br/>
    URL: <input name="url" type="text" size="75" value="http://query.yahooapis.com/v1/yql"/>
    <br/>
    Query: <input name="query" type="text" size="75" value="select * from social.profile where guid=me"/>
    <br/>
    Format:
    <select name="format">
        <option>xml</option>
        <option>json</option>
    </select>
    <br/>
    Callback: <input name="callback" type="text" size="10" value="foo"/>
    <br/>
    <input type="submit"/>
</form>
</body>
</html>
<%
    }
%>


================================================================================




This project only accepts contributions licensed under the BSD open source license. See the Open Source Initiative's approved template below.
Each file submitted should contain the following information in the header:
 
// Created by [contributor]
// Copyright (c) [enter owning person entity, year]. All Rights Reserved.
// Licensed under the BSD (revised) open source license.
 
Here is the Open Source Initiative BSD License Template (http://opensource.org/licenses/bsd-license.php ):
 
-------------------------------------
Copyright (c) <YEAR>, <OWNER>
All rights reserved.
 
Redistribution and use of this software in source and binary forms, with
or without modification, are permitted provided that the following
conditions are met:
 
* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.
 
* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.
 
* Neither the name of <ORGANIZATION> nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
-------------------------------------
