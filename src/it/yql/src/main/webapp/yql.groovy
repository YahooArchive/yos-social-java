/*
// Created by Sumit Shah on 7/02/09.
// Copyright (c) 2010 Yahoo! Inc. All rights reserved.
//
// The copyrights embodied in the content of this file are licensed under the BSD (revised) open source license.
*/
def query = request.getParameter("query")
def yahooSession = request.getAttribute("yahooSession")
def params = ["q", query, "format", "xml"] as String[]
def url = "http://query.yahooapis.com/v1/yql";
response.setContentType("text/xml");
println(yahooSession.invokeString(url, "GET", params));
