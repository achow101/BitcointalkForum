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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.Board;
import com.achow101.bitcointalkforum.items.Post;
import com.achow101.bitcointalkforum.items.Poster;
import com.achow101.bitcointalkforum.items.Topic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TopicFragment extends Fragment {

    private String mTopicURL;
    private String mSessId;

    private ProgressBar mProgressView;

    private Button mPrevButton;
    private Button mNextButton;

    private ListView mListView;

    private TextView mPageNumText;

    private List<Post> mPosts;

    private GetPosts mGetPostsTask;

    private OnTopicInteraction mListener;

    public static TopicFragment newInstance(String topicURL, String sessId) {
        TopicFragment fragment = new TopicFragment();
        Bundle args = new Bundle();
        args.putString("Topic URL", topicURL);
        args.putString("Session ID", sessId);
        fragment.setArguments(args);
        return fragment;
    }

    public TopicFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_topic, container, false);

        // Get arguments stuff
        mTopicURL = getArguments().getString("Topic URL");
        mSessId = getArguments().getString("Session ID");

        // Get layout stuff
        mProgressView = (ProgressBar) v.findViewById(R.id.posts_progress_bar);
        mListView = (ListView) v.findViewById(R.id.posts_list);
        mPrevButton = (Button) v.findViewById(R.id.prev_page_button);
        mNextButton = (Button) v.findViewById(R.id.next_page_button);

        // Set page number
        mPageNumText = (TextView)v.findViewById(R.id.page_num);

        // get posts
        showProgress(true);
        mGetPostsTask = new GetPosts(mTopicURL, mSessId);
        mGetPostsTask.execute((Void) null);

        return v;
    }

    public void showProgress(final boolean show) {
        // Show and hide ui stuff
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        mPrevButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mNextButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mPageNumText.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTopicInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTopicInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class PostsListAdapter implements ListAdapter
    {
        private List<Post> posts;

        public PostsListAdapter(List<Post> posts)
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
            return posts.get(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null)
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.post_layout, null);
            }

            // Get post and poster
            Post post = posts.get(position);
            Poster poster = post.getPoster();

            // Get post text views
            TextView subjectTitle = (TextView)v.findViewById(R.id.subject_title);
            TextView postTime = (TextView)v.findViewById(R.id.post_time);

            // Set post text views
            subjectTitle.setText(post.getSubject());
            postTime.setText(post.getPostedTime());

            // Get poster text views
            TextView posterTxt = (TextView)v.findViewById(R.id.poster);
            TextView rank = (TextView)v.findViewById(R.id.rank);
            TextView personalText = (TextView)v.findViewById(R.id.personal_text);
            TextView activity = (TextView)v.findViewById(R.id.activity);
            TextView specialPos = (TextView)v.findViewById(R.id.spec_pos);

            // set text for poster
            posterTxt.setText(poster.getName());
            rank.setText(poster.getRank());
            activity.setText(poster.getActivityStr());
            personalText.setText(poster.getPersonalText());

            // Get and set poster avatar image
            ImageView avatar = (ImageView)v.findViewById(R.id.avatar);
            avatar.setImageDrawable(poster.getAvatar());

            // Set special position text
            if(poster.isSpecial())
                specialPos.setText(poster.getSpecialPos());
            else
            {
                specialPos.setText("");
                specialPos.setVisibility(View.GONE);
            }

            // Set rank coins
            ImageView coins = (ImageView)v.findViewById(R.id.coins);
            if(poster.isSpecial() && !poster.getSpecialPos().equals("Staff"))
            {
                switch(poster.getSpecialPos())
                {
                    case "Administrator": coins.setImageResource(R.drawable.admin);
                        break;
                    case "Global Moderator": coins.setImageResource(R.drawable.global_mod);
                        break;
                    case "Founder": coins.setImageResource(R.drawable.founder);
                        break;
                    case "Moderator": coins.setImageResource(R.drawable.moderator);
                        break;
                    case "Donator": coins.setImageResource(R.drawable.donator);
                        break;
                    case "VIP": coins.setImageResource(R.drawable.vip);
                        break;
                }
            }
            else
            {
                switch (poster.getRank())
                {
                    case "Brand New": coins.setImageResource(R.drawable.coin);
                        break;
                    case "Newbie": coins.setImageResource(R.drawable.coin);
                        break;
                    case "Jr. Member": coins.setImageResource(R.drawable.coin);
                        break;
                    case "Member": coins.setImageResource(R.drawable.member);
                        break;
                    case "Full Member": coins.setImageResource(R.drawable.full);
                        break;
                    case "Sr. Member": coins.setImageResource(R.drawable.sr);
                        break;
                    case "Hero Member": coins.setImageResource(R.drawable.hero);
                        break;
                    case "Legendary": coins.setImageResource(R.drawable.legendary);
                }
            }

            // Display the post
            TextView postText = (TextView)v.findViewById(R.id.post);
            postText.setText(post.getPostBody());
            postText.setMovementMethod(LinkMovementMethod.getInstance());

            return v;
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
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

    private class GetPosts extends AsyncTask<Void, Void, List<List<Object>>>
    {
        private String topicURL;
        private String sessId;

        public GetPosts(String topicURL, String sessId)
        {
            this.topicURL = topicURL;
            this.sessId = sessId;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... params) {
            List<Object> posts = new ArrayList<Object>();
            List<Object> nextPrevPageURLS = new ArrayList<Object>();
            List<Object> pageNums = new ArrayList<Object>();

            try {
                Document doc = Jsoup.connect(topicURL).cookie("PHPSESSID", mSessId).get();

                // Get prev and next page URLs
                Elements prevnexts = doc.select("span.prevnext > a.navPages");
                for(Element prevnext : prevnexts)
                {
                    switch(prevnext.text())
                    {
                        case "«": nextPrevPageURLS.add(prevnext.attr("href"));
                            break;
                        case "»": nextPrevPageURLS.add(prevnext.attr("href"));
                            break;
                    }
                }

                // Get navPages
                Elements navPages = doc.select("td.middletext[valign=bottom][style=padding-bottom: 4px;] > a.navPages");
                int lastPage = 0;

                // For one page topics
                if(navPages.isEmpty())
                {
                    pageNums.add(1);
                    pageNums.add(1);
                }

                // for multipage topics and page is in middle
                for(Element navPage : navPages)
                {
                    int thisPage = Integer.parseInt(navPage.text());
                    if(thisPage - 1 != lastPage) {
                        pageNums.add(thisPage - 1);
                        pageNums.add(Integer.parseInt(navPages.last().text()));
                        break;
                    }
                    else
                    {
                        lastPage++;
                    }
                }

                // for last page of topic
                if(pageNums.isEmpty())
                {
                    pageNums.add(lastPage + 1);
                    pageNums.add(lastPage + 1);
                }

                // Get table with all the posts
                Elements postsTable = doc.select("div#bodyarea > form > table.bordercolor");

                // Get first post of page and its class name
                String postClassName = postsTable.select("tr").get(0).className();

                // Get all posts on page
                Elements postElements = postsTable.select("tr." + postClassName);

                // Get data from each post
                for(Element post : postElements)
                {
                    // Get element with posterInfo
                    Element posterInfo = post.select("td.poster_info").first();

                    // Get poster name
                    String posterName = posterInfo.select("b > a[href]").text();

                    // Get poster rank and activity
                    String posterText = posterInfo.text();
                    String posterActivityStr = posterText.substring(posterText.indexOf("Activity"), posterText.indexOf(" ", posterText.indexOf("Activity") + 10));
                    String posterPersText;
                    if(posterText.contains("Ignore")) {
                        posterPersText = posterText.substring(posterText.indexOf(posterActivityStr) + posterActivityStr.length(), posterText.lastIndexOf("Ignore"));
                    }
                    else
                    {
                        posterPersText = posterText.substring(posterText.indexOf(posterActivityStr) + posterActivityStr.length());
                    }

                    // Get rank
                    String posterRank = "Brand New";
                    if(posterText.contains("Brand New"))
                        posterRank = "Brand New";
                    else if(posterText.contains("Newbie"))
                        posterRank = "Newbie";
                    else if(posterText.contains("Jr. Member"))
                        posterRank = "Jr. Member";
                    else if(posterText.contains("Full Member"))
                        posterRank = "Full Member";
                    else if(posterText.contains("Sr. Member"))
                        posterRank = "Sr. Member";
                    else if(posterText.contains("Hero Member"))
                        posterRank = "Hero Member";
                    else if(posterText.contains("Legendary"))
                        posterRank = "Legendary";
                    else if(posterText.contains("Member"))
                        posterRank = "Member";
                    // get special positions
                    String specialPosition = null;
                    if(posterText.contains("Staff"))
                        specialPosition = "Staff";
                    else if(posterText.contains("Global Moderator"))
                        specialPosition = "Global Moderator";
                    else if(posterText.contains("Moderator"))
                        specialPosition = "Moderator";
                    else if(posterText.contains("Administrator"))
                        specialPosition = "Administrator";
                    else if(posterText.contains("Founder"))
                        specialPosition = "Founder";
                    else if(posterText.contains(" Donator"))
                        specialPosition = "Donator";
                    else if(posterText.contains("VIP"))
                        specialPosition = "VIP";


                    // Get poster avatar
                    Elements avatarImgs = posterInfo.select("img.avatar");
                    BitmapDrawable avatar = null;
                    if(!avatarImgs.isEmpty())
                    {
                        // Get avatar element data
                        Element avatarImg = avatarImgs.first();
                        String avatarURL = avatarImg.absUrl("src");

                        // Download avatar
                        URL url = new URL(avatarURL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        avatar = new BitmapDrawable(getResources(), input);
                        avatar.setBounds(0, 0, avatar.getIntrinsicWidth()*100, avatar.getIntrinsicHeight()*100);
                    }

                    // Create poster object
                    Poster poster = new Poster(posterName, avatar, posterPersText, posterActivityStr, posterRank);
                    if(specialPosition != null)
                        poster.setSpecialPos(specialPosition);

                    // Get elements with post and header
                    Element headerAndPost = post.select("td.td_headerandpost").first();

                    // Get subject and post times
                    String subject = headerAndPost.select("table > tbody > tr > td > div > a[href]").first().text();
                    String postURL = headerAndPost.select("table > tbody > tr > td > div > a[href]").first().attr("href");
                    long id = Long.parseLong(postURL.substring(postURL.indexOf("#msg") + 4));
                    String postedTime = headerAndPost.select("table > tbody > tr > td > div.smalltext").first().text();

                    // TODO: add the parsing of quotes and images

                    // Get body of post
                    String postBodyStr = headerAndPost.select(".post").first().html();
                    Spanned postBody = Html.fromHtml(postBodyStr, new ImageGetter(), null);

                    // Create post object
                    Post postObj = new Post(poster, postedTime, subject, postBody, id);
                    posts.add(postObj);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            List<List<Object>> out = new ArrayList<List<Object>>();
            out.add(posts);
            out.add(nextPrevPageURLS);
            out.add(pageNums);

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
            mGetPostsTask = null;
            showProgress(false);

            if (result.size() > 0) {
                List<Object> postObjs = result.get(0);
                final List<Object> prevNextURLs = result.get(1);
                List<Post> posts = new ArrayList<Post>();
                List<Object> pageNums = result.get(2);

                for (Object post : postObjs) {
                    posts.add((Post) post);
                }

                if(mTopicURL.contains(".0"))
                {
                    mPrevButton.setClickable(false);
                    mPrevButton.setVisibility(View.GONE);
                    if (posts.size() < 20) {
                        mNextButton.setClickable(false);
                        mNextButton.setVisibility(View.GONE);
                    }
                    else
                    {
                        mNextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPageSelected((String) prevNextURLs.get(0));
                            }
                        });
                    }
                }
                else {
                    if (posts.size() < 20) {
                        mNextButton.setClickable(false);
                        mNextButton.setVisibility(View.GONE);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPageSelected((String) prevNextURLs.get(0));
                            }
                        });
                    } else {
                        mPrevButton.setClickable(true);
                        mNextButton.setClickable(true);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPageSelected((String) prevNextURLs.get(0));
                            }
                        });
                        mNextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPageSelected((String) prevNextURLs.get(1));
                            }
                        });
                    }
                }

                // Set page numbers
                mPageNumText.setText("Page " + pageNums.get(0) + "/" + pageNums.get(1));

                PostsListAdapter mAdapter  = new PostsListAdapter(posts);
                mListView.setAdapter(mAdapter);

            } else
            {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }

        }

        @Override
        protected void onCancelled() {
            mGetPostsTask = null;
            showProgress(false);
        }
    }

    public interface OnTopicInteraction {

        public void onPageSelected(String topicURL);
    }

}
