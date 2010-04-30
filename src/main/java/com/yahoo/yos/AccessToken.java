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

public class AccessToken {

    private String key;
    private String secret;
    private String guid;
    private String owner;
    private long tokenExpires;
    private String sessionHandle;
    private String consumer;
    private long handleExpires;

    public AccessToken() {
    }

    public AccessToken(Cookie cookie) throws UnsupportedEncodingException, JSONException {
        JSONObject json = new JSONObject(new String(Base64.decodeBase64(cookie.getValue().getBytes("UTF-8")), "UTF-8"));
        setKey(json.optString("key", null));
        setSecret(json.optString("secret", null));
        setGuid(json.optString("guid", null));
        setOwner(json.optString("owner", null));
        setTokenExpires(json.optLong("tokenExpires", -1));
        setHandleExpires(json.optLong("handleExpires", -1));
        setSessionHandle(json.optString("sessionHandle", null));
        setConsumer(json.optString("consumer", null));
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

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getTokenExpires() {
        return tokenExpires;
    }

    public void setTokenExpires(long tokenExpires) {
        this.tokenExpires = tokenExpires;
    }

    public String getSessionHandle() {
        return sessionHandle;
    }

    public void setSessionHandle(String sessionHandle) {
        this.sessionHandle = sessionHandle;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public long getHandleExpires() {
        return handleExpires;
    }

    public void setHandleExpires(long handleExpires) {
        this.handleExpires = handleExpires;
    }

    public Cookie getCookie() throws UnsupportedEncodingException, JSONException {
        return new Cookie("yosdk_at", new String(Base64.encodeBase64(toJSONObject().toString().getBytes("UTF-8")), "UTF-8"));
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("key", key);
        object.put("secret", secret);
        object.put("guid", guid);
        object.put("owner", owner);
        object.put("tokenExpires", tokenExpires);
        object.put("sessionHandle", sessionHandle);
        object.put("consumer", consumer);
        object.put("handleExpires", handleExpires);
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
