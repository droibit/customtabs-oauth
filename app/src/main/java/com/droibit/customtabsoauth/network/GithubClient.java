package com.droibit.customtabsoauth.network;

import android.content.Context;
import android.net.Uri;

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
 * Created by kumagai on 2015/08/19.
 */
public class GithubClient implements Client {

    private static final String URL_AUTH = "https://github.com/login/oauth/authorize";
    private static final String URL_ACCESS = "https://github.com/login/oauth/access_token";

    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_CLIENT_SECRET = "client_secret";
    private static final String KEY_REDIRECT_URI = "redirect_uri";
    private static final String KEY_CODE = "code";
    private static final String KEY_SCOPE = "scope";

    private static final String ACCEPT = "Accept";

    public final Context mContext;
    public final OkHttpClient mOkHttpClient;

    public GithubClient(Context context, OkHttpClient okHttpClient) {
        mContext = context;
        mOkHttpClient = okHttpClient;
    }

    public Uri requestToken() {
        return Uri.parse(URL_AUTH)
                .buildUpon()
                .appendQueryParameter(KEY_CLIENT_ID, mContext.getString(R.string.github_client_id))
                .appendQueryParameter(KEY_REDIRECT_URI, mContext.getString(R.string.github_redirect))
                .appendQueryParameter(KEY_SCOPE, "user")
                .build();
    }

    @Override
    public Map<String, String> requestAccess(String callback) throws IOException, JSONException {
        final Uri uri = Uri.parse(callback);
        final String code = uri.getQueryParameter(KEY_CODE);

        final String bodyString = String.format("%s=%s&%s=%s&%s=%s&%s=%s",
                KEY_CLIENT_ID, mContext.getString(R.string.github_client_id),
                KEY_CLIENT_SECRET, mContext.getString(R.string.github_client_secret),
                KEY_CODE, code,
                KEY_SCOPE, "user");
        final RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), bodyString);
        final Request request = new Request.Builder()
                                    .addHeader(ACCEPT, CONTENT_JSON.toString())
                .url(URL_ACCESS)
                                    .post(requestBody)
                                    .build();

        final Response response = mOkHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            return null;
        }
        final JSONObject responseJson = new JSONObject(response.body().string());
        final Map<String, String> user = new HashMap<>(2);
        user.put(KEY_USER_NAME, responseJson.getString(KEY_SCOPE));
        user.put(KEY_ACCESS_TOKEN, responseJson.getString(KEY_ACCESS_TOKEN));

        return user;
    }
}
