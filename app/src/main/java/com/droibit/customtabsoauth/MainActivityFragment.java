package com.droibit.customtabsoauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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

import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private TextView mTextView;
    private OkHttpClient mOkHttpClient;

    private PocketClient mPocket;
    private GithubClient mGithub;

    private Client mCurrent = null;

    public MainActivityFragment() {
    }

    /** {@inheritDoc} */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mOkHttpClient = new OkHttpClient();
        mPocket = new PocketClient(context, mOkHttpClient);
        mGithub = new GithubClient(context, mOkHttpClient);
    }

    /** {@inheritDoc} */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    /** {@inheritDoc} */
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
            @Override public void onClick(View v) {
                requestToken(mGithub);
            }
        });
    }

    public void onNewIntent(final Intent intent) {
        new AsyncTask<Void, Void, Map<String, String>>() {
            @Override protected Map<String, String> doInBackground(Void... params) {
                try {
                    return mCurrent.requestAccess(intent.getDataString());
                } catch(Exception e) {
                    Log.e("ERROR", "Error request access: ", e);
                }
                return null;
            }

            @Override protected void onPostExecute(Map<String, String> result) {
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
            @Override protected Uri doInBackground(Void... params) {
                try {
                    return mCurrent.requestToken();
                } catch (Exception e) {
                    Log.e("ERROR", "Error request token: ", e);
                }
                return null;
            }

            @Override protected void onPostExecute(Uri uri) {
                super.onPostExecute(uri);
                if (uri != null) {
                    //launchChrome(uri);
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
                .setToolbarColor(getResources().getColor(R.color.primary))
//                .setStartAnimations(getActivity(), R.anim.slide_in_right, R.anim.slide_out_left)
//                .setExitAnimations(getActivity(), R.anim.slide_in_left, R.anim.slide_out_right)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .build();
        tabsIntent.launchUrl(getActivity(), uri);
    }

    private void launchCustomTabsWithActionMenu(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, uri.toString());
        final PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);
        
        final Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_share);

        final CustomTabsIntent tabsIntent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(getResources().getColor(R.color.primary))
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back))
                .addMenuItem(getString(R.string.action_share), pendingIntent)
                .setActionButton(icon, getString(R.string.action_share), pendingIntent)
                .build();
        tabsIntent.launchUrl(getActivity(), uri);
    }

    private void launchChrome(Uri uri) {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    protected void setTextViewHTML(TextView textView) {
        final CharSequence text = textView.getText();
        final SpannableStringBuilder sb = new SpannableStringBuilder(text);
        final URLSpan[] urls = sb.getSpans(0, text.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(sb, span);
        }
        textView.setText(sb);
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            @Override public void onClick(View view) {
                launchCustomTabsWithActionMenu(Uri.parse(span.getURL()));
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }
}
