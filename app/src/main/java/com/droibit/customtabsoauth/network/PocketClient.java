package com.droibit.customtabsoauth.network;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.droibit.customtabsoauth.R;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @auther kumagai
 */
public class PocketClient implements Client {

    private static final String URL_REQUEST = "https://getpocket.com/v3/oauth/request";
    private static final String URL_AUTHRIZE = "https://getpocket.com/v3/oauth/authorize";
    private static final String URL_OAUTH = "https://getpocket.com/auth/authorize";
    private static final String KEY_CONSUMER_KEY = "consumer_key";
    private static final String KEY_REQUEST_TOKEN = "request_token";
    private static final String KEY_REDIRECT_URI = "redirect_uri";
    private static final String KEY_CODE = "code";

    private static final String X_ACCEPT = "X-Accept";

    public final OkHttpClient mOkHttpClient;
    public final Context mContext;

    public PocketClient(Context context, OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
        mContext = context;
    }

    @Override
    public Uri requestToken() throws Exception {
        final String requestJson = new JSONObject()
                                    .put(KEY_CONSUMER_KEY, mContext.getString(R.string.pocket_consumer_key))
                                    .put(KEY_REDIRECT_URI, mContext.getString(R.string.pocket_redirect))
                                    .toString();
        final Request request = new Request.Builder()
                                    .addHeader(X_ACCEPT, CONTENT_JSON.toString())
                                    .url(URL_REQUEST)
                                    .post(RequestBody.create(CONTENT_JSON, requestJson))
                                    .build();

        final Response response = mOkHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            return null;
        }

        final JSONObject tokenResponseJson = new JSONObject(response.body().string());
        final String code = tokenResponseJson.get(KEY_CODE).toString();

        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                         .putString(KEY_CODE, code)
                         .apply();

        return Uri.parse(URL_OAUTH)
                .buildUpon()
                .appendQueryParameter(KEY_REQUEST_TOKEN, code)
                .appendQueryParameter(KEY_REDIRECT_URI, mContext.getString(R.string.pocket_redirect))
                .build();
    }

    @Override
    public Map<String, String> requestAccess(String callback) throws JSONException, IOException {
        final String requestToken = PreferenceManager.getDefaultSharedPreferences(mContext).getString(KEY_CODE, "");
        final String authRequestJson = new JSONObject()
                .put(KEY_CONSUMER_KEY, mContext.getString(R.string.pocket_consumer_key))
                .put(KEY_CODE, requestToken)
                .toString();
        final Request authRequest = new Request.Builder()
                .addHeader(X_ACCEPT, CONTENT_JSON.toString())
                .url(URL_AUTHRIZE)
                .post(RequestBody.create(CONTENT_JSON, authRequestJson))
                .build();
        final Response authResponse = mOkHttpClient.newCall(authRequest).execute();
        if (!authResponse.isSuccessful()) {
            return null;
        }

        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .remove(KEY_CODE)
                .apply();

        final JSONObject authResponseJson = new JSONObject(authResponse.body().string());
        final Map<String, String> user = new HashMap<>(2);
        user.put(KEY_ACCESS_TOKEN, authResponseJson.getString(KEY_ACCESS_TOKEN));
        user.put(KEY_USER_NAME, authResponseJson.getString(KEY_USER_NAME));
        return user;
    }
}
