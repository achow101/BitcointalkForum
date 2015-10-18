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

import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.Post;
import com.achow101.bitcointalkforum.items.Poster;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    private int mPageNum;
    private String mSessId;

    private ListView mListView;
    private ProgressBar mProgressView;
    private Button mPrevButton;
    private Button mNextButton;
    private TextView mPageNumText;
    private Button newMessageButton;

    private GetPMs mGetPMsTask;

    private OnPMInteraction mListener;

    public static MessagesFragment newInstance(int page, String sessId) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putInt("Page", page);
        args.putString("Session ID", sessId);
        fragment.setArguments(args);
        return fragment;
    }

    public MessagesFragment() {
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
        View v = inflater.inflate(R.layout.fragment_topic, container, false);

        // Get arguments
        mPageNum = getArguments().getInt("Page");
        mSessId = getArguments().getString("Session ID");

        // Get layout stuff
        mProgressView = (ProgressBar) v.findViewById(R.id.posts_progress_bar);
        mListView = (ListView) v.findViewById(R.id.posts_list);
        mPrevButton = (Button) v.findViewById(R.id.prev_page_button);
        mNextButton = (Button) v.findViewById(R.id.next_page_button);

        // Reply button to new Button, New PM when clicked
        newMessageButton = (Button)v.findViewById(R.id.reply_button);
        newMessageButton.setText("New");
        newMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPMReplySelected("https://bitcointalk.org/index.php?action=pm;sa=send");
            }
        });

        // Set page number
        mPageNumText = (TextView)v.findViewById(R.id.page_num);
        mPageNumText.setText("Page " + mPageNum);

        // get pms
        showProgress(true);
        mGetPMsTask = new GetPMs("https://bitcointalk.org/index.php?action=pm;f=inbox;sort=date;desc;start=" + ((mPageNum - 1) * 20), mSessId);
        mGetPMsTask.execute((Void) null);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPMInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPMInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnPMInteraction {

        public void onPMPageSelected(int page);
        public void onPMReplySelected(String editURL);
    }

    public void showProgress(final boolean show) {
        // Show and hide ui stuff
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        mPrevButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mNextButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mPageNumText.setVisibility(show ? View.GONE : View.VISIBLE);
        newMessageButton.setVisibility(show ? View.GONE : View.VISIBLE);
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
            final Post post = posts.get(position);
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
                        break;
                    case "Guest": coins.setVisibility(View.GONE);
                        break;
                }
            }

            // Display the post
            TextView postText = (TextView)v.findViewById(R.id.post);
            postText.setText(post.getPostBody());
            postText.setMovementMethod(LinkMovementMethod.getInstance());

            // Get the buttons
            Button quoteButton = (Button) v.findViewById(R.id.quote_button);
            Button editButton = (Button) v.findViewById(R.id.edit_button);
            Button replyButton = (Button) v.findViewById(R.id.reply_button);
            Button deleteButton = (Button) v.findViewById(R.id.delete_button);

            // Hide edit button
            editButton.setVisibility(View.GONE);

            // Set delete listener
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeleteReply delRep = new DeleteReply(post.getDeleteURL(), mSessId);
                    delRep.execute((Void) null);
                }
            });

            // Set reply and quote listener
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onPMReplySelected(post.getEditURL());
                }
            });
            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onPMReplySelected(post.getQuoteURL());
                }
            });

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

    private class GetPMs extends AsyncTask<Void, Void, List<Post>>
    {
        private String topicURL;
        private String sessId;

        public GetPMs(String topicURL, String sessId)
        {
            this.topicURL = topicURL;
            this.sessId = sessId;
        }

        @Override
        protected List<Post> doInBackground(Void... params) {
            List<Post> posts = new ArrayList<Post>();

            try {
                Document doc = Jsoup.connect(topicURL).cookie("PHPSESSID", mSessId).get();

                if(topicURL.contains("start=0")) {
                    FileOutputStream os = getContext().openFileOutput("messages.html", Context.MODE_PRIVATE);
                    os.write(doc.html().getBytes());
                    os.close();
                }

                // Get body
                Elements body = doc.select("div#bodyarea");

                // Get the table with posts
                Element pmTable = body.select("form[name=pmFolder] > table[cellpadding=0]").first();

                // Get the elements with pms
                Elements pmIds = pmTable.select("tbody > tr > td[style=padding: 1px 1px 0 1px;] > a[name]");

                List<Long> ids = new ArrayList<Long>();
                for(Element id : pmIds)
                {
                    ids.add(Long.parseLong(id.attr("name").substring(3)));
                }

                Elements pmElements = pmTable.select("table[cellpadding=4] > tbody > tr:not(.windowbg):not(.windowbg2)");

                // Get data from each post
                for(int i = 0; i < pmElements.size(); i++)
                {
                    Element post = pmElements.get(i);

                    // Get element with posterInfo
                    Element posterInfo = post.select("td[style=overflow: hidden;]").first();

                    // Get poster name
                    String posterName = posterInfo.select("b > a[href]").text();

                    // Get poster rank and activity
                    String posterText = posterInfo.text();

                    String posterActivityStr = "";
                    String posterPersText = "";
                    if(!posterText.equals("Bitcoin Forum Guest")) {
                        posterActivityStr = posterText.substring(posterText.indexOf("Activity"), posterText.indexOf(" ", posterText.indexOf("Activity") + 10));
                        posterPersText = posterText.substring(posterText.indexOf(posterActivityStr) + posterActivityStr.length(), posterText.lastIndexOf("Trust"));
                    }
                    else
                    {
                        posterName = "Bitcoin Forum";
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
                    else if(posterText.contains("Guest"))
                        posterRank = "Guest";
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

                    // Get header element
                    Element pmHead = post.select("td.windowbg > table > tbody > tr > td[align=left]").first();

                    if(pmHead == null)
                        pmHead = post.select("td.windowbg2 > table > tbody > tr > td[align=left]").first();

                    // Get header right
                    Element pmHeadRight = post.select("td.windowbg > table > tbody > tr > td[align=right]").first();
                    if(pmHeadRight == null)
                        pmHeadRight = post.select("td.windowbg2 > table > tbody > tr > td[align=right]").first();

                    // Get quote reply and delete strings
                    String quoteURL = pmHeadRight.select("a[href]").first().attr("href");
                    String replyURL = pmHeadRight.select("a[href]").get(1).attr("href");
                    String deleteURL = pmHeadRight.select("a[href]").get(2).attr("href");

                    // Get subject and post times
                    String subject = pmHead.select("b").first().text();
                    String pmedTime = pmHead.text().replaceAll(subject, "");

                    // Get elements with post and header
                    Element pm = post.select("div.personalmessage").first();

                    // Get body of post
                    String postBodyStr = pm.html();
                    Spanned postBody = Html.fromHtml(postBodyStr, new ImageGetter(), null);

                    // Create post object
                    Post postObj = new Post(poster, pmedTime, subject, postBody, ids.get(i), quoteURL, replyURL, deleteURL);
                    posts.add(postObj);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return posts;
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
        protected void onPostExecute(List<Post> result)
        {
            showProgress(false);
            mGetPMsTask = null;

            if(mPageNum == 1)
            {
                mPrevButton.setClickable(false);
                mPrevButton.setVisibility(View.GONE);
                if (result.size() < 20) {
                    mNextButton.setClickable(false);
                    mNextButton.setVisibility(View.GONE);
                }
                else
                {
                    mNextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onPMPageSelected(mPageNum + 1);
                        }
                    });
                }
            }
            else {
                if (result.size() <= 20) {
                    mNextButton.setClickable(false);
                    mNextButton.setVisibility(View.GONE);
                    mPrevButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onPMPageSelected(mPageNum - 1);
                        }
                    });
                } else {
                    mPrevButton.setClickable(true);
                    mNextButton.setClickable(true);
                    mPrevButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onPMPageSelected(mPageNum - 1);
                        }
                    });
                    mNextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onPMPageSelected(mPageNum + 1);
                        }
                    });
                }
            }

            PostsListAdapter mAdapter  = new PostsListAdapter(result);
            mListView.setAdapter(mAdapter);
        }

        @Override
        protected void onCancelled() {
            mGetPMsTask = null;
            showProgress(false);
        }
    }

    private class DeleteReply extends AsyncTask<Void, Void, Void>
    {
        private String deleteURL;
        private String sessId;

        public DeleteReply(String deleteURL, String sessId)
        {
            this.deleteURL = deleteURL;
            this.sessId = sessId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Document deleteDoc = Jsoup.connect(deleteURL).cookie("PHPSESSID", sessId).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            mListener.onPMPageSelected(0);
        }
    }

}
