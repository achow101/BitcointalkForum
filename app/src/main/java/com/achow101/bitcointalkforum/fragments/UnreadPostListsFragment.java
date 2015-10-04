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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.Topic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnUnreadListInteraction}
 * interface.
 */
public class UnreadPostListsFragment extends Fragment {
    private OnUnreadListInteraction mListener;

    private String mListURL;
    private String mSessId;

    private GetTopics mGetTopicsTask = null;
    private ProgressBar mProgressView;

    private Button mPrevButton;
    private Button mNextButton;

    private ListView mListView;

    private TextView mPageNumText;

    public static UnreadPostListsFragment newInstance(String listURL, String sessID) {
        UnreadPostListsFragment fragment = new UnreadPostListsFragment();
        Bundle args = new Bundle();
        args.putString("List URL", listURL);
        args.putString("Session ID", sessID);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UnreadPostListsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnUnreadListInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnUnreadListInteraction");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boardtopic_list, container, false);

        // Get stuff for adapter
        mSessId = getArguments().getString("Session ID");
        mListURL = getArguments().getString("List URL");
        int pageNum = (Integer.parseInt(mListURL.substring(mListURL.indexOf("start=") + 6)) / 40) + 1;

        // Get the ListView
        mListView = (ListView) view.findViewById(R.id.topics_list);
        mProgressView = (ProgressBar) view.findViewById(R.id.topic_loading_progress);

        // get buttons
        mPrevButton = (Button)view.findViewById(R.id.prev_topic_page);
        mNextButton = (Button)view.findViewById(R.id.next_topic_page);

        // Set page number text
        mPageNumText = (TextView)view.findViewById(R.id.page_num);
        mPageNumText.setText("Page " + pageNum);

        // Get the topics from Bitcointalk
        showProgress(true);
        mGetTopicsTask = new GetTopics(mListURL, mSessId);
        mGetTopicsTask.execute((Void) null);

        return view;
    }

    /**
     * Shows the progress UI and hides list and buttons
     */
    public void showProgress(final boolean show) {
        // Show and hide ui stuff
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        mPrevButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mNextButton.setVisibility(show ? View.GONE : View.VISIBLE);
        mPageNumText.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class UnreadPostsListAdapter implements ListAdapter
    {
        private List<Topic> mTopics;

        public UnreadPostsListAdapter(List<Topic> topics)
        {
            this.mTopics = topics;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            if(!mTopics.isEmpty())
                return mTopics.size();
            else
                return 1;
        }

        @Override
        public Object getItem(int position) {
            if(mTopics.isEmpty())
                return null;
            else
                return mTopics.get(position);
        }

        @Override
        public long getItemId(int position) {
            if(mTopics.isEmpty())
                return 1;
            else
                return mTopics.get(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = null;
            if(getItemViewType(position) == 0)
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.topic_in_list_layout, null);

                // Get layout stuff
                TextView topicSubject = (TextView)v.findViewById(R.id.topic_list_title);
                TextView topicStarter = (TextView)v.findViewById(R.id.topic_starter);
                TextView topicLastPost = (TextView)v.findViewById(R.id.topic_last_post);
                ImageButton goToLastPost = (ImageButton)v.findViewById(R.id.go_to_last_post_button);
                ImageView lockImage = (ImageView)v.findViewById(R.id.lock_image);
                ImageView stickyImage = (ImageView)v.findViewById(R.id.sticky_image);

                // Reset stuff
                lockImage.setVisibility(View.INVISIBLE);
                stickyImage.setVisibility(View.INVISIBLE);

                // Get topic
                final Topic topic = mTopics.get(position);

                // set stuff for topic subject
                topicSubject.setText(topic.getSubject());

                // Set stuff for topic starter
                topicStarter.setText("Started by: " + topic.getStarter());

                // Set stuff for last post info
                topicLastPost.setText("Last post: " + topic.getLastPost());

                // Set onclicklistener for last post button
                goToLastPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onTopicSelected(topic.getLastPostURL());
                    }
                });

                // Set image for locked or sticky
                if(topic.isLocked())
                {
                    lockImage.setVisibility(View.VISIBLE);
                }
                if(topic.isSticky())
                {
                    stickyImage.setVisibility(View.VISIBLE);
                }
            }
            else if(getItemViewType(position) == 1)
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.category_layout, null);

                TextView title = (TextView)v.findViewById(R.id.cat_title);
                title.setText("No New Topics");
            }

            return v;
        }

        @Override
        public int getItemViewType(int position) {
            if(mTopics.size() > 0)
                return 0;
            else
                return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private class GetTopics extends AsyncTask<Void, Void, List<List<Object>>> {
        private String mSessId;
        private String mListURL;

        public GetTopics(String listURL, String sessId) {
            this.mSessId = sessId;
            this.mListURL = listURL;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... params) {

            List<Object> topics = new ArrayList<Object>();
            List<Object> nextPrevPageURLS = new ArrayList<Object>();

            try {
                // Retrieve the page
                Document doc = Jsoup.connect(mListURL).cookie("PHPSESSID", mSessId).get();

                // Retrieve the body area of the page
                Element body = doc.getElementById("bodyarea");

                // Get prev and next page URLs
                Elements prevnexts = body.select("span.prevnext > a.navPages");
                for(Element prevnext : prevnexts)
                {
                    switch(prevnext.text())
                    {
                        case "«":
                            if(nextPrevPageURLS.size() == 0)
                                nextPrevPageURLS.add(prevnext.attr("href"));
                            else if(nextPrevPageURLS.size() > 0 && !nextPrevPageURLS.get(nextPrevPageURLS.size() - 1).equals(prevnext.attr("href")))
                                nextPrevPageURLS.add(prevnext.attr("href"));
                            break;
                        case "»":
                            if(nextPrevPageURLS.size() == 0)
                                nextPrevPageURLS.add(prevnext.attr("href"));
                            else if(nextPrevPageURLS.size() > 0 && !nextPrevPageURLS.get(nextPrevPageURLS.size() - 1).equals(prevnext.attr("href")))
                                nextPrevPageURLS.add(prevnext.attr("href"));
                            break;
                    }
                }

                // Get the topics
                Elements allElements = body.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td");
                List<String> subjects = new ArrayList<String>();
                List<String> starters = new ArrayList<String>();
                List<Integer> repliesCount = new ArrayList<Integer>();
                List<Integer> viewCount = new ArrayList<Integer>();
                List<String> lastPoster = new ArrayList<String>();
                List<Boolean> stickies = new ArrayList<Boolean>();
                List<Boolean> locked = new ArrayList<Boolean>();
                List<Long> ids = new ArrayList<Long>();
                List<String> topicURLs = new ArrayList<String>();
                List<String> lastPostURLs = new ArrayList<String>();
                for(Element elem : allElements)
                {
                    if(elem.html().contains(".0;topicseen\">"))
                    {
                        String subject = elem.text();

                        // Get only the subject
                        if(subject.contains("«"))
                        {
                            subject = subject.substring(0, subject.indexOf(" «"));
                        }
                        subjects.add(subject);

                        // Check for sticky
                        if(elem.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/show_sticky.gif\""))
                        {
                            stickies.add(true);
                        }
                        else
                        {
                            stickies.add(false);
                        }

                        // Check for locked
                        if(elem.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/quick_lock.gif\""))
                        {
                            locked.add(true);
                        }
                        else
                        {
                            locked.add(false);
                        }

                        // Get the id
                        long id = Long.parseLong(elem.html().substring(elem.html().indexOf("topic=") + 6, elem.html().indexOf(".", elem.html().indexOf("topic="))));
                        ids.add(id);

                        // Get the URLs
                        topicURLs.add(elem.select("a[href]").get(0).attr("href"));
                    }
                    else if(elem.html().contains("title=\"View the profile of"))
                    {
                        starters.add(elem.text());
                    }
                    else if(elem.outerHtml().contains("<td class=\"windowbg\" valign=\"middle\" width=\"4%\" align=\"center\">")) {
                        int number = Integer.parseInt(elem.text());
                        if (repliesCount.size() == viewCount.size())
                        {
                            repliesCount.add(number);
                        }
                        else if(repliesCount.size() > viewCount.size())
                        {
                            viewCount.add(number);
                        }
                    }
                    else if(elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">"))
                    {
                        lastPoster.add(elem.text());
                        lastPostURLs.add(elem.select("a[href]").attr("href"));
                    }
                }

                for(int i = 0; i < subjects.size(); i++) {
                    Topic topicObj = new Topic(subjects.get(i), starters.get(i), repliesCount.get(i), viewCount.get(i), lastPoster.get(i), stickies.get(i), locked.get(i), true, ids.get(i));
                    topicObj.setURL(topicURLs.get(i));
                    topicObj.setLastPostURL(lastPostURLs.get(i));
                    topics.add(topicObj);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            List<List<Object>> out = new ArrayList<List<Object>>();
            out.add(topics);
            out.add(nextPrevPageURLS);

            return out;
        }

        @Override
        protected void onPostExecute(final List<List<Object>> result) {

            mGetTopicsTask = null;
            showProgress(false);

            final List<Topic> topics = new ArrayList<Topic>();

            if (result.size() > 0) {
                List<Object> topicObjs = result.get(0);
                final List<Object> prevNextURLs = result.get(1);

                for (Object topic : topicObjs) {
                    topics.add((Topic) topic);
                }

                if(mListURL.contains("start=0"))
                {
                    mPrevButton.setClickable(false);
                    mPrevButton.setVisibility(View.GONE);
                    mNextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onPrevNextPageSelected((String) prevNextURLs.get(0));
                        }
                    });
                }
                else {
                    if (topics.size() < 40) {
                        mNextButton.setClickable(false);
                        mNextButton.setVisibility(View.GONE);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPrevNextPageSelected((String) prevNextURLs.get(0));
                            }
                        });
                    } else {
                        mPrevButton.setClickable(true);
                        mNextButton.setClickable(true);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPrevNextPageSelected((String) prevNextURLs.get(0));
                            }
                        });
                        mNextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onPrevNextPageSelected((String) prevNextURLs.get(1));
                            }
                        });
                    }
                }

                UnreadPostsListAdapter mListAdp = new UnreadPostsListAdapter(topics);
                mListView.setAdapter(mListAdp);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        mListener.onTopicSelected(topics.get(position).getUrl());
                    }
                });

            } else
            {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }

        }

        @Override
        protected void onCancelled() {
            mGetTopicsTask = null;
            showProgress(false);
        }
    }

    public interface OnUnreadListInteraction
    {
        public void onTopicSelected(String topicURL);
        public void onPrevNextPageSelected(String listURL);
    }

}
