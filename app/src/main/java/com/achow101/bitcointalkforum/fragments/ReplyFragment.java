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
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.SummaryPost;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ReplyFragment extends Fragment {

    private OnPostListener mListener;

    private String mReplyURL;
    private String mSessId;

    private PostReply mPostReply;
    private String mPostURL;
    private String mTopicID;
    private String mSC;

    private EditText mSubjectText;
    private EditText mMessageText;
    private ListView mTopicSummary;
    private Button mPost;
    private ProgressBar mProgressView;

    private TextView mSubjectLabel;
    private TextView mMessageLabel;
    private TextView mSummaryLabel;

    private GetReplyPage mGetReplyPage;

    public static ReplyFragment newInstance(String replyURL, String sessId) {
        ReplyFragment fragment = new ReplyFragment();
        Bundle args = new Bundle();
        args.putString("Reply URL", replyURL);
        args.putString("Session ID", sessId);
        fragment.setArguments(args);
        return fragment;
    }

    public ReplyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_reply, container, false);

        // Get reply url and sessid
        mReplyURL = getArguments().getString("Reply URL");
        mSessId = getArguments().getString("Session ID");

        // Get layout stuff
        mSubjectText = (EditText)v.findViewById(R.id.subject_text);
        mMessageText = (EditText)v.findViewById(R.id.message_text);
        mTopicSummary = (ListView)v.findViewById(R.id.topic_summary_list);
        mPost = (Button)v.findViewById(R.id.post_button);
        mProgressView = (ProgressBar)v.findViewById(R.id.progress);
        mSubjectLabel = (TextView)v.findViewById(R.id.subject_textview);
        mMessageLabel = (TextView)v.findViewById(R.id.message_textView);
        mSummaryLabel = (TextView)v.findViewById(R.id.topic_summary_textview);

        // getting data
        showProgress(true);
        mGetReplyPage = new GetReplyPage(mReplyURL, mSessId);
        mGetReplyPage.execute((Void) null);

        return v;
    }

    public void showProgress(final boolean show) {
        // Show and hide ui stuff
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mSubjectText.setVisibility(show ? View.GONE : View.VISIBLE);
        mMessageText.setVisibility(show ? View.GONE : View.VISIBLE);
        mTopicSummary.setVisibility(show ? View.GONE : View.VISIBLE);
        mPost.setVisibility(show ? View.GONE : View.VISIBLE);
        mProgressView.setVisibility(show ? View.GONE : View.VISIBLE);
        mSubjectLabel.setVisibility(show ? View.GONE : View.VISIBLE);
        mMessageLabel.setVisibility(show ? View.GONE : View.VISIBLE);
        mSummaryLabel.setVisibility(show ? View.GONE : View.VISIBLE);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPostListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPostListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class TopicSummaryAdapter implements ListAdapter
    {
        private List<SummaryPost> posts;

        public TopicSummaryAdapter(List<SummaryPost> posts)
        {
            this.posts = posts;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return posts.size();
        }

        @Override
        public Object getItem(int position) {
            return posts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if(v == null)
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.summary_post, null);
            }

            TextView posterInfo  = (TextView)v.findViewById(R.id.poster);
            TextView post = (TextView)v.findViewById(R.id.post);

            posterInfo.setText(posts.get(position).getPoster());
            post.setText(posts.get(position).getText());

            return v;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private class GetReplyPage extends AsyncTask<Void, Void, List<List<Object>>>
    {
        private String mReplyURL;
        private String mSessId;

        public GetReplyPage(String replyURL, String sessId)
        {
            this.mReplyURL = replyURL;
            this.mSessId = sessId;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... params) {
            List<List<Object>> out = new ArrayList<List<Object>>();
            List<Object> subjMess = new ArrayList<Object>();
            List<Object> postSummary = new ArrayList<Object>();
            List<Object> postData = new ArrayList<>();

            try {
                Document doc = Jsoup.connect(mReplyURL).cookie("PHPSESSID", mSessId).get();

                // retrieve subject
                String subject = doc.select("td > input[name=subject]").first().attr("value");

                // Retrieve any existing message (e.g. quotes)
                String message = doc.select("td > textarea.editor[name=message]").first().text();

                // Add subject and message to a list
                subjMess.add(subject);
                subjMess.add(message);

                // Get posters and posts for topic summary
                Elements posters = doc.select("table > tbody > tr.catbg");
                Elements posts = doc.select("table > tbody > tr.windowbg2");

                for(int i = 0; i < posts.size(); i++)
                {
                    Element poster = posters.get(i);
                    Element post = posts.get(i);

                    SummaryPost postObj = new SummaryPost(poster.text(), Html.fromHtml(post.html(), new ImageGetter(), null));
                    postSummary.add(postObj);
                }

                // Get post URL
                postData.add(doc.select("form#postmodify").attr("action"));

                // Get topic id
                postData.add(doc.select("td.windowbg > input[name=topic]").first().attr("value"));

                // Get secret key
                postData.add(doc.select("input[type=hidden][name=sc]").first().attr("value"));

            } catch (IOException e) {
                e.printStackTrace();
            }

            out.add(subjMess);
            out.add(postSummary);
            out.add(postData);

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
        protected void onPostExecute(List<List<Object>> result)
        {
            showProgress(false);
            mProgressView.setVisibility(View.GONE);
            mGetReplyPage = null;

            if(result.size() > 0)
            {
                final List<Object> subjMess = result.get(0);
                List<Object> posts = result.get(1);

                // Set subject and message texts initial text
                mSubjectText.setText((String)subjMess.get(0));
                mMessageText.setText((String)subjMess.get(1));

                // TODO: add stuff for quick icons like smileys and text formatting

                // Set topic summary list
                List<SummaryPost> summaryPosts = new ArrayList<>();
                for (Object post : posts) {
                    summaryPosts.add((SummaryPost) post);
                }
                TopicSummaryAdapter mAdp = new TopicSummaryAdapter(summaryPosts);
                mTopicSummary.setAdapter(mAdp);

                // Set the post url
                mPostURL = (String)result.get(2).get(0);
                mTopicID = (String)result.get(2).get(1);
                mSC = (String)result.get(2).get(2);


                // Set posting behavior
                mPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPostReply = new PostReply(mPostURL, mSubjectText.getText().toString(), mMessageText.getText().toString(), mSC, mTopicID, mSessId);
                        mPostReply.execute((Void)null);
                    }
                });

            }
            else
            {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private class PostReply extends AsyncTask<Void, Void, Void>
    {
        private String postURL;
        private String sc;
        private String subject;
        private String message;
        private String id;
        private String sessId;

        public PostReply(String postURL, String subject, String message, String sc, String id, String sessId)
        {
            this.postURL = postURL;
            this.sc = sc;
            this.subject = subject;
            this.message = message;
            this.id = id;
            this.sessId = sessId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Connection.Response res = Jsoup.connect(postURL)
                        .data("sc", sc)
                        .data("subject", subject)
                        .data("message", message)
                        .data("topic", id)
                        // TODO: add stuff for additional options for posting
                        .data("do_watch", "1")
                        .data("additional_options", "0")
                        .cookie("PHPSESSID", sessId)
                        .method(Connection.Method.POST)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public interface OnPostListener {
        public void onPostInteraction(Uri uri);
    }

}
