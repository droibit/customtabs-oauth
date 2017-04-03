package com.droibit.customtabsoauth.network;

import android.net.Uri;

import java.util.Map;

import okhttp3.MediaType;

public interface Client {

    String KEY_USER_NAME = "username";
    String KEY_ACCESS_TOKEN = "access_token";

    MediaType CONTENT_JSON = MediaType.parse("application/json");

    Uri requestToken() throws Exception;
    Map<String, String> requestAccess(String callback) throws Exception;
}
