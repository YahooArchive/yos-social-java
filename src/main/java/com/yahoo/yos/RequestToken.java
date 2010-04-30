// Created by Sumit Shah on 7/02/09.
// Copyright (c) 2010 Yahoo! Inc. All rights reserved.
//
// The copyrights embodied in the content of this file are licensed under the BSD (revised) open source license.
package com.yahoo.yos;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestToken {
    private String key;
    private String secret;
    private String sessionHandle;

    public RequestToken() {
    }

    public RequestToken(Cookie cookie) throws UnsupportedEncodingException, JSONException {
        JSONObject json = new JSONObject(new String(Base64.decodeBase64(cookie.getValue().getBytes("UTF-8")), "UTF-8"));
        setKey(json.optString("key", null));
        setSecret(json.optString("secret", null));
        setSessionHandle(json.optString("sessionHandle", null));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSessionHandle() {
        return sessionHandle;
    }

    public void setSessionHandle(String sessionHandle) {
        this.sessionHandle = sessionHandle;
    }

    public Cookie getCookie() throws UnsupportedEncodingException, JSONException {
        return new Cookie("yosdk_rt", new String(Base64.encodeBase64(toJSONObject().toString().getBytes("UTF-8")), "UTF-8"));
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("key", key);
        object.put("secret", secret);
        object.put("sessionHandle", sessionHandle);
        return object;
    }

    @Override
    public String toString() {
        try {
            return toJSONObject().toString();
        } catch (Exception e) {
            return super.toString();
        }
    }

}
