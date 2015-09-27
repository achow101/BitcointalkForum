package com.achow101.bitcointalkforum.fragments;

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
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
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
        int pageNum = (Integer.parseInt(mTopicURL.substring(mTopicURL.indexOf(".", mTopicURL.indexOf("topic=")) + 1)) / 40) + 1;

        // Get layout stuff
        mProgressView = (ProgressBar) v.findViewById(R.id.posts_progress_bar);
        mListView = (ListView) v.findViewById(R.id.posts_list);
        mPrevButton = (Button) v.findViewById(R.id.prev_page_button);
        mNextButton = (Button) v.findViewById(R.id.next_page_button);

        // Set page number
        mPageNumText = (TextView)v.findViewById(R.id.page_num);
        mPageNumText.setText("Page " + pageNum);

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
            avatar.setImageBitmap(poster.getAvatar());

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

            TextView postText = (TextView)v.findViewById(R.id.post);
            postText.setText(post.getPostBody());

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

    private class GetPosts extends AsyncTask<Void, Void, List<Post>>
    {
        private String topicURL;
        private String sessId;

        public GetPosts(String topicURL, String sessId)
        {
            this.topicURL = topicURL;
            this.sessId = sessId;
        }

        @Override
        protected List<Post> doInBackground(Void... params) {
            List<Post> posts = new ArrayList<Post>();

            try {
                Document doc = Jsoup.connect(topicURL).cookie("PHPSESSID", mSessId).get();

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
                    Bitmap avatar = null;
                    if(!avatarImgs.isEmpty())
                    {
                        // Get avatar element data
                        Element avatarImg = avatarImgs.first();
                        String avatarURL = avatarImg.absUrl("src");

                        // Download bitmap
                        InputStream in = new java.net.URL(avatarURL).openStream();
                        avatar = BitmapFactory.decodeStream(in);
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
                    TextNode postBodyNode = TextNode.createFromEncoded(headerAndPost.select(".post").first().html(), headerAndPost.select(".post").first().baseUri());
                    String postBodyStr = postBodyNode.getWholeText();
                    Spanned postBody = Html.fromHtml(postBodyStr, new ImageGetter(), null);

                    // Create post object
                    Post postObj = new Post(poster, postURL, postedTime, subject, postBody, id);
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
                InputStream in = null;
                try {
                    in = new java.net.URL(source).openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap img = BitmapFactory.decodeStream(in);

                BitmapDrawable bmp = new BitmapDrawable(getResources(), img);
                return bmp;
            }
        }

        @Override
        protected void onPostExecute(List<Post> result)
        {
            mGetPostsTask = null;
            showProgress(false);

            PostsListAdapter mAdapter  = new PostsListAdapter(result);
            mListView.setAdapter(mAdapter);
        }
    }

}
