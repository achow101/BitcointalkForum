/*
 * Copyright (c) 2015 Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.achow101.bitcointalkforum.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.achow101.bitcointalkforum.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileFragment extends Fragment {

    private String mSessId;

    private ProgressBar mProgressView;
    private TextView mProfileText;

    private GetProfile mGetProfile;

    public static ProfileFragment newInstance(String sessId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("Session ID", sessId);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void showProgress(final boolean show) {
        // Show and hide ui stuff
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileText.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        // Get session id
        mSessId = getArguments().getString("Session ID");

        // Get views
        mProgressView = (ProgressBar)v.findViewById(R.id.progressBar);
        mProfileText = (TextView)v.findViewById(R.id.profile_textview);

        // Get profile stuff
        showProgress(true);
        mGetProfile = new GetProfile(mSessId);
        mGetProfile.execute((Void) null);

        return v;
    }

    private class GetProfile extends AsyncTask<Void, Void, Spanned>
    {
        private String sessId;

        public GetProfile(String sessId)
        {
            this.sessId = sessId;
        }

        @Override
        protected Spanned doInBackground(Void... params) {
            Spanned out = null;

            try {
                Document doc = Jsoup.connect("https://bitcointalk.org/index.php?action=profile").cookie("PHPSESSID", sessId).get();

                // Get body div
                Element body = doc.select("div#bodyarea").first();

                // Get profile table
                Element profileBody = body.select("table.bordercolor").get(1);
                Element profile = profileBody.select("tbody > tr").get(1);

                // Get raw html
                Element sig = profile.select("tr").last();
                sig.remove();
                String profileHtml = profile.html();
                profileHtml = profileHtml.replaceAll("</tr>", "").replaceAll("<tr>", "<br>").replaceAll("Signature:", "");
                out = Html.fromHtml(profileHtml, new ImageGetter(), null);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return out;
        }

        private class ImageGetter implements Html.ImageGetter
        {

            @Override
            public Drawable getDrawable(String source) {
                // Download bitmap
                if(!source.contains("bitcointalk.org"))
                    source = "https://bitcointalk.org" + source;
                BitmapDrawable bmp = null;
                try {
                    URL url = new URL(source);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bmp = new BitmapDrawable(getResources(), input);
                    bmp.setBounds(0, 0, bmp.getIntrinsicWidth()*10, bmp.getIntrinsicHeight()*10);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                return bmp;
            }
        }

        @Override
        protected void onPostExecute(Spanned result)
        {
            mGetProfile = null;
            showProgress(false);

            mProfileText.setText(result);
            mProfileText.setMovementMethod(new ScrollingMovementMethod());
        }

        @Override
        protected void onCancelled() {
            mGetProfile = null;
            showProgress(false);
        }
    }
}
