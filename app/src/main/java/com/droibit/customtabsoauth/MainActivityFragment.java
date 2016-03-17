package com.droibit.customtabsoauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.droibit.customtabsoauth.network.Client;
import com.droibit.customtabsoauth.network.GithubClient;
import com.droibit.customtabsoauth.network.PocketClient;
import com.squareup.okhttp.OkHttpClient;

import org.chromium.customtabsclient.shared.CustomTabActivityHelper;

import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements CustomTabActivityHelper.ConnectionCallback {

    private static final String sDevPageUrl = "https://developer.chrome.com/multidevice/android/customtabs";

    private TextView mTextView;
    private OkHttpClient mOkHttpClient;

    private PocketClient mPocket;
    private GithubClient mGithub;

    private Client mCurrent = null;

    private CustomTabActivityHelper mCustomTabActivityHelper;

    public MainActivityFragment() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mOkHttpClient = new OkHttpClient();
        mPocket = new PocketClient(context, mOkHttpClient);
        mGithub = new GithubClient(context, mOkHttpClient);

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        mCustomTabActivityHelper.setConnectionCallback(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextView = ((TextView) view.findViewById(android.R.id.text1));
        setTextViewHTML(mTextView);

        view.findViewById(R.id.oauth_pocket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestToken(mPocket);
            }
        });
        view.findViewById(R.id.oauth_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestToken(mGithub);
            }
        });
        view.findViewById(R.id.launch_nosession).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCustomTabsWithActionMenu(null, Uri.parse(sDevPageUrl));
            }
        });
        view.findViewById(R.id.launch_session).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCustomTabsWithActionMenu(mCustomTabActivityHelper.getSession(), Uri.parse(sDevPageUrl));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mCustomTabActivityHelper.bindCustomTabsService(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        mCustomTabActivityHelper.unbindCustomTabsService(getActivity());
    }

    public void onNewIntent(final Intent intent) {
        new AsyncTask<Void, Void, Map<String, String>>() {
            @Override
            protected Map<String, String> doInBackground(Void... params) {
                try {
                    return mCurrent.requestAccess(intent.getDataString());
                } catch (Exception e) {
                    Log.e("ERROR", "Error request access: ", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Map<String, String> result) {
                super.onPostExecute(result);
                if (result == null) {
                    Toast.makeText(getActivity(), "Failed...", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getActivity(), "Success: " + result.get(PocketClient.KEY_USER_NAME), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void requestToken(Client client) {
        mCurrent = client;

        new AsyncTask<Void, Void, Uri>() {
            @Override
            protected Uri doInBackground(Void... params) {
                try {
                    return mCurrent.requestToken();
                } catch (Exception e) {
                    Log.e("ERROR", "Error request token: ", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Uri uri) {
                super.onPostExecute(uri);
                if (uri != null) {
                    launchCustomTabs(uri);
                }
            }
        }.execute();
    }

    /**
     * https://developer.chrome.com/multidevice/android/customtabs
     *
     * @param uri
     */
    private void launchCustomTabs(Uri uri) {
        final CustomTabsIntent tabsIntent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .addDefaultShareMenuItem()
                .setToolbarColor(ContextCompat.getColor(getContext(), R.color.primary))
                .setStartAnimations(getContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(getContext(), android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .build();
        tabsIntent.launchUrl(getActivity(), uri);
    }

    private void launchCustomTabsWithActionMenu(@Nullable CustomTabsSession session, Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, uri.toString());
        final PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);

        final Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_share);

        final CustomTabsIntent tabsIntent = new CustomTabsIntent.Builder(session)
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(getContext(), R.color.primary))
                .setSecondaryToolbarColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark))
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .addMenuItem(getString(R.string.action_share), pendingIntent)
                .setActionButton(icon, getString(R.string.action_share), pendingIntent)
                .setStartAnimations(getContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(getContext(), android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .enableUrlBarHiding()
                .addToolbarItem(1, icon, getString(R.string.action_share), pendingIntent)
                .addToolbarItem(2, icon, getString(R.string.action_share), pendingIntent)
                .addToolbarItem(3, icon, getString(R.string.action_share), pendingIntent)
                .addToolbarItem(4, icon, getString(R.string.action_share), pendingIntent)
                .addToolbarItem(5, icon, getString(R.string.action_share), pendingIntent)
                .build();

//        tabsIntent.launchUrl(getActivity(), uri);
        CustomTabActivityHelper.openCustomTab(getActivity(), tabsIntent, uri, null);
    }

    private void launchChrome(Uri uri) {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    protected void setTextViewHTML(TextView textView) {
        final CharSequence text = textView.getText();
        final SpannableStringBuilder sb = new SpannableStringBuilder(text);
        final URLSpan[] urls = sb.getSpans(0, text.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(sb, span);
        }
        textView.setText(sb);
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                launchCustomTabsWithActionMenu(null, Uri.parse(span.getURL()));
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    @Override
    public void onCustomTabsConnected() {
        Log.d(BuildConfig.BUILD_TYPE, "Connected custom tabs service.");

        if (mCustomTabActivityHelper.mayLaunchUrl(Uri.parse(sDevPageUrl), null, null)) {
            Toast.makeText(getContext(), "Connected custom tabs service, and warm up OK.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCustomTabsDisconnected() {
        Log.d(BuildConfig.BUILD_TYPE, "Disconnected custom tabs service.");
    }
}
