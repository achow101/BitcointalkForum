package com.achow101.bitcointalkforum.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
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
import com.achow101.bitcointalkforum.items.Board;
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
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTopicListInteraction}
 * interface.
 */
public class BoardTopicFragment extends Fragment {

    private OnTopicListInteraction mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    private List<Board> mChildBoards;
    private List<Topic> mTopics;
    private String mBoardURL;
    private String mSessId;

    private GetTopics mGetTopicsTask = null;
    private ProgressBar mProgressView;

    private Button mPrevButton;
    private Button mNextButton;

    private TextView mPageNumText;

    public static BoardTopicFragment newInstance(String boardURL, String sessId, String category) {
        BoardTopicFragment fragment = new BoardTopicFragment();
        Bundle args = new Bundle();
        args.putString("URL", boardURL);
        args.putString("SessID", sessId);
        args.putString("Category", category);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BoardTopicFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boardtopic_list, container, false);

        // Get stuff for adapter
        mBoardURL = getArguments().getString("URL");
        mSessId = getArguments().getString("SessID");
        int pageNum = (Integer.parseInt(mBoardURL.substring(mBoardURL.indexOf(".", mBoardURL.indexOf("board=")) + 1)) / 40) + 1;

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
        mGetTopicsTask = new GetTopics(mSessId, mBoardURL, getArguments().getString("Category"));
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTopicListInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTopicListInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTopicListInteraction {

        public void onTopicSelected(String topicURL);
        public void onChildBoardSelected(String boardURL, String category);
    }

    public class CustomListAdapter implements ListAdapter
    {
        private List<Topic> topics;
        private List<Board> childBoards;
        private List<Object> everything;

        public CustomListAdapter(List<Topic> topics, List<Board> childBoards)
        {
            this.topics = topics;
            this.childBoards = childBoards;
            everything = new ArrayList<Object>();
            everything.addAll(childBoards);
            everything.addAll(topics);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = null;
            if(position < childBoards.size())
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.board_list_layout, null);

                TextView topicSubject = (TextView)v.findViewById(R.id.board_title);
                topicSubject.setText(childBoards.get(position).getName());
            }
            else
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
                final Topic topic = topics.get(position - childBoards.size());

                // Set stuff for subject
                topicSubject.setText(topic.getSubject());
                if(topic.hasUnreadPosts())
                {
                    topicSubject.setTypeface(null, Typeface.BOLD);
                }
                else
                {
                    topicSubject.setTypeface(null, Typeface.NORMAL);
                }

                // Set setuff for topic starter
                topicStarter.setText("Started by: " + topic.getStarter());

                // Set stuff for last post info
                topicLastPost.setText("Last post: " + topic.getLastPost());

                // Set onclicklistener for last post button
                goToLastPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast toast = Toast.makeText(getContext(), "Go To Last Post Clicked. URL: " + topic.getLastPostURL(), Toast.LENGTH_LONG);
                        toast.show();
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
            return v;
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
            return childBoards.size() + topics.size();
        }

        @Override
        public Object getItem(int position) {
            return everything.get(position);
        }

        @Override
        public long getItemId(int position) {
            if(position < childBoards.size())
            {
                return childBoards.get(position).getId();
            }
            else
            {
                return topics.get(position - childBoards.size()).getId();
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            if(position < childBoards.size())
            {
                return 0;
            }
            else
            {
                return 1;
            }
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

    private class GetTopics extends AsyncTask<Void, Void, List<List<Object>>>
    {
        private String mSessId;
        private String boardURL;
        private String mCategory;

        public GetTopics(String sessId, String boardURL, String category)
        {
            this.mSessId = sessId;
            this.boardURL = boardURL;
            this.mCategory = category;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... params)
        {
            List<Object> topics = new ArrayList<Object>();
            List<Object> childBoards = new ArrayList<Object>();
            List<Object> nextPrevPageURLS = new ArrayList<Object>();
            try {
                // Retrieve the page
                Document doc = Jsoup.connect(boardURL).cookie("PHPSESSID", mSessId).get();

                // Retrieve the body area of the page
                Element body = doc.getElementById("bodyarea");

                // Get prev and next page URLs
                Elements prevnexts = body.select("#toppages > span.prevnext > a.navPages");
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

                // Get the divs for Child Boards and topics
                Elements bodyDivs = body.select("div.tborder");

                // Search through bodyDivs for stuff
                for(Element div : bodyDivs)
                {
                    // Gets child board div
                    if(div.outerHtml().contains("Child Boards"))
                    {
                        // Get Elements with Child board titiles
                        Elements boardElements = div.select("td.windowbg2");

                        // Get data from each Child Board element
                        for(Element boardElement : boardElements)
                        {
                            // Get Stuff with links to boards
                            Elements boardTitles = boardElement.select("a[href]");

                            // Find actual board titles
                            for(Element boardTitle : boardTitles) {
                                String boardStr = boardTitle.outerHtml();
                                if (boardStr.contains("index.php?board="))
                                {
                                    int startInx = boardStr.indexOf("board=") + 6;
                                    int endInx = startInx + 1;
                                    Board board = new Board(boardTitle.text(), mCategory, Integer.parseInt(boardStr.substring(startInx, endInx)), childBoards.size());
                                    board.setURL(boardTitle.attr("href"));
                                    childBoards.add(board);
                                }
                            }
                        }
                    }

                    // Gets topics div
                    else if(div.outerHtml().contains("Subject"))
                    {
                        // Get Stickies
                        Elements stickyTopicCols = div.select("td.windowbg3");

                        // Get Other topics
                        Elements nonStickyTopicCols = div.select("td.windowbg");

                        // Get last post column
                        Elements lastPostCols = div.select("td.lastpostcol");

                        // Get starters
                        Elements starters = div.select("[class=windowbg2]");

                        int numTopics = 0;
                        int numStickies = 0;
                        for(Element topic : stickyTopicCols)
                        {

                            if(topic.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/show_sticky.gif\""))
                            {
                                String subject = topic.text();

                                // Get only the subject
                                if(subject.contains("«"))
                                {
                                    subject = subject.substring(0, subject.indexOf(" «"));
                                }

                                int numReplies = 0;
                                int numViews = 0;
                                boolean locked = false;
                                boolean hasUnread = false;

                                // Get starter
                                String starter = starters.get(numStickies).text();

                                // Check locked
                                if(topic.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/quick_lock.gif\""))
                                {
                                    locked = true;
                                }

                                // Check for new
                                if(topic.html().contains("<img class=\"newimg\" src=\"https://bitcointalk.org/Themes/custom1/images/english/new.gif\" alt=\"New\">"))
                                {
                                    hasUnread = true;
                                }

                                // Get id
                                long id = Long.parseLong(topic.html().substring(topic.html().indexOf("topic=") + 6, topic.html().indexOf(".", topic.html().indexOf("topic="))));

                                // Get URL
                                String topicURL = topic.select("span > a[href]").get(0).attr("href");

                                // Get Last post URL
                                String lastPostURL = lastPostCols.get(numStickies).select("a[href]").attr("href");

                                // Create topic object and add to array
                                Topic topicObj = new Topic(subject, starter, numReplies, numViews, lastPostCols.get(numStickies).text(), true, locked, hasUnread, id);
                                topicObj.setURL(topicURL);
                                topicObj.setLastPostURL(lastPostURL);
                                topics.add(topicObj);
                                numStickies++;
                            }
                        }

                        // Get non sticky topics
                        numTopics = numStickies;
                        for(Element topic : nonStickyTopicCols)
                        {
                            if(topic.html().contains("msg_"))
                            {
                                String subject = topic.text();

                                // Get only the subject
                                if(subject.contains("«"))
                                {
                                    subject = subject.substring(0, subject.indexOf(" «"));
                                }

                                int numReplies = 0;
                                int numViews = 0;
                                String starter;
                                boolean locked = false;
                                boolean hasUnread = false;

                                // Get starter
                                starter = starters.get(numTopics).text();

                                // Check locked
                                if(topic.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/quick_lock.gif\""))
                                {
                                    locked = true;
                                }

                                // Check for new
                                if(topic.html().contains("<img class=\"newimg\" src=\"https://bitcointalk.org/Themes/custom1/images/english/new.gif\" alt=\"New\">"))
                                {
                                    hasUnread = true;
                                }

                                // Get id
                                long id = Long.parseLong(topic.html().substring(topic.html().indexOf("topic=") + 6, topic.html().indexOf(".", topic.html().indexOf("topic="))));

                                // Get URL
                                String topicURL = topic.select("span > a[href]").get(0).attr("href");

                                // Get Last post URL
                                String lastPostURL = lastPostCols.get(numTopics).select("a[href]").attr("href");

                                // Create topic object and add to array
                                Topic topicObj = new Topic(subject, starter, numReplies, numViews, lastPostCols.get(numTopics).text(), false, locked, hasUnread, id);
                                topicObj.setURL(topicURL);
                                topicObj.setLastPostURL(lastPostURL);
                                topics.add(topicObj);
                                numTopics++;
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            List<List<Object>> out = new ArrayList<List<Object>>();
            out.add(childBoards);
            out.add(topics);
            out.add(nextPrevPageURLS);

            return out;
        }

        @Override
        protected void onPostExecute(final List<List<Object>> result) {

            mGetTopicsTask = null;
            showProgress(false);

            if (result.size() > 0) {
                List<Object> childBoards = result.get(0);
                List<Object> topics = result.get(1);
                final List<Object> prevNextURLs = result.get(2);
                mChildBoards = new ArrayList<Board>();
                mTopics = new ArrayList<Topic>();

                for (Object childBoard : childBoards) {
                    mChildBoards.add((Board) childBoard);
                }

                for (Object topic : topics) {
                    mTopics.add((Topic) topic);
                }

                if(mBoardURL.contains(".0"))
                {
                    mPrevButton.setClickable(false);
                    mPrevButton.setVisibility(View.GONE);
                    mNextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onChildBoardSelected((String) prevNextURLs.get(0), mCategory);
                        }
                    });
                }
                else {
                    if (mTopics.size() < 40) {
                        mNextButton.setClickable(false);
                        mNextButton.setVisibility(View.GONE);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onChildBoardSelected((String) prevNextURLs.get(0), mCategory);
                            }
                        });
                    } else {
                        mPrevButton.setClickable(true);
                        mNextButton.setClickable(true);
                        mPrevButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onChildBoardSelected((String) prevNextURLs.get(0), mCategory);
                            }
                        });
                        mNextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mListener.onChildBoardSelected((String) prevNextURLs.get(1), mCategory);
                            }
                        });
                    }
                }

            } else
            {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }

            CustomListAdapter mListAdp = new CustomListAdapter(mTopics, mChildBoards);
            mListView.setAdapter(mListAdp);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position < mChildBoards.size()) {
                        mListener.onChildBoardSelected(mChildBoards.get(position).getURL(), mChildBoards.get(position).getCategory());
                    } else {
                        mListener.onTopicSelected(mTopics.get(position - mChildBoards.size()).getUrl());
                    }
                }
            });

        }

        @Override
        protected void onCancelled() {
            mGetTopicsTask = null;
            showProgress(false);
        }
    }

}
