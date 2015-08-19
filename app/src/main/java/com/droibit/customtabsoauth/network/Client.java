package com.droibit.customtabsoauth.network;

import android.net.Uri;

import com.squareup.okhttp.MediaType;

import java.util.Map;

/**
 * Created by kumagai on 2015/08/19.
 */
public interface Client {

    String KEY_USER_NAME = "username";
    String KEY_ACCESS_TOKEN = "access_token";

    MediaType CONTENT_JSON = MediaType.parse("application/json");

    Uri requestToken() throws Exception;
    Map<String, String> requestAccess(String callback) throws Exception;
}
