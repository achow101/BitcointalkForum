package com.achow101.bitcointalkforum;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
 * Activities containing this fragment MUST implement the {@link OnListInteraction}
 * interface.
 */
public class BoardTopicFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnListInteraction mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    private ListAdapter mAdapter;
    private List<Board> mChildBoards;
    private List<Topic> mTopics;
    private String mBoardURL;
    private String mSessId;

    private GetTopics mGetTopicsTask = null;
    private ProgressBar mProgressView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boardtopic_list, container, false);

        // Get stuff for adapter
        mBoardURL = getArguments().getString("URL");
        mSessId = getArguments().getString("SessID");

        // Get the ListView
        mListView = (ListView) view.findViewById(R.id.topics_list);
        mProgressView = (ProgressBar) view.findViewById(R.id.topic_loading_progress);

        // Get the topics from Bitcointalk
        showProgress(true);
        mGetTopicsTask = new GetTopics(mSessId, mBoardURL, getArguments().getString("Category"));
        mGetTopicsTask.execute((Void) null);

        return view;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*try {
            mListener = (OnListInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnListInteraction");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
        }
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
    public interface OnListInteraction {

        public void onTopicSelected(String topicURL);
        public void onChildBoardSelected(String boardURL);
    }

    public class CustomListAdapter implements ListAdapter
    {
        private List<Topic> topics;
        private List<Board> childBoards;

        public CustomListAdapter(List<Topic> topics, List<Board> childBoards)
        {
            this.topics = topics;
            this.childBoards = childBoards;
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
            return childBoards.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == 1) {
                return childBoards;
            } else
            {
                return topics.get(position + 1);
            }
        }

        @Override
        public long getItemId(int position) {
            if(position == 0)
            {
                return 0;
            }
            else
            {
                return topics.get(position).getId();
            }
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null && getItemViewType(position) == 1) {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.topic_in_list_layout, null);
            }
            if(v == null && getItemViewType(position) == 0)
            {
                LayoutInflater infalInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.child_board_layout, null);
            }
            if(getItemViewType(position) == 1)
            {
                // Get layout stuff
                TextView topicSubject = (TextView)v.findViewById(R.id.topic_list_title);
                TextView topicStarter = (TextView)v.findViewById(R.id.topic_starter);
                TextView topicLastPost = (TextView)v.findViewById(R.id.topic_last_post);
                ImageButton goToLastPost = (ImageButton)v.findViewById(R.id.go_to_last_post_button);
                ImageView lockImage = (ImageView)v.findViewById(R.id.lock_image);
                ImageView stickyImage = (ImageView)v.findViewById(R.id.sticky_image);

                // Get topic
                Topic topic;
                if(childBoards.size() > 0)
                {
                    topic = topics.get(position - 1);
                }
                else
                {
                    topic = topics.get(position);
                }

                // Set stuff for subject
                topicSubject.setText(topic.getSubject());
                if(topic.hasUnreadPosts())
                {
                    topicSubject.setTypeface(null, Typeface.BOLD);
                }

                // Set setuff for topic starter
                topicStarter.setText("Started by: " + topic.getStarter());

                // Set stuff for last post info
                topicLastPost.setText("Last post: " + topic.getLastPost());

                // TODO: Set stuff for last post jump

                // Set image for locked or sticky
                stickyImage.setVisibility(topic.isSticky() ? View.GONE : View.VISIBLE);
                lockImage.setVisibility(topic.isLocked() ? View.GONE : View.VISIBLE);

            }
            else if(getItemViewType(position) == 0)
            {
                // TODO: Set stuff for child boards
            }



            return v;
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0)
            {
                return 0;
            }
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
            try {
                // Retrieve the page
                Document doc = Jsoup.connect(boardURL).cookie("PHPSESSID", mSessId).get();

                // Retrieve the body area of the page
                Element body = doc.getElementById("bodyarea");

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
                        Elements starters = div.select("td.windowbg2");

                        int numTopics = 0;
                        int numStickies = 0;
                        for(Element topic : stickyTopicCols)
                        {

                            if(topic.html().contains("<img src=\"https://bitcointalk.org/Themes/custom1/images/icons/show_sticky.gif\""))
                            {
                                String subject = topic.text();
                                int numReplies = 0;
                                int numViews = 0;
                                String starter;
                                boolean locked = false;
                                boolean hasUnread = false;

                                // Get replies data for stickies
                                for(Element replies : stickyTopicCols)
                                {
                                    if(replies.outerHtml().contains("<td class=\"windowbg3\" valign=\"middle\" width=\"4%\" align=\"center\">")) {
                                        numReplies = Integer.parseInt(replies.text());

                                        // Get views data for stickies
                                        for (Element views : stickyTopicCols) {
                                            if(views.outerHtml().contains("<td class=\"windowbg3\" valign=\"middle\" width=\"4%\" align=\"center\">")) {
                                                if (Integer.parseInt(views.text()) != numReplies) {
                                                    numViews = Integer.parseInt(views.text());
                                                }
                                            }
                                        }
                                    }
                                }

                                // set views and replies to proper ones
                                if(numReplies > numViews)
                                {
                                    int numRepliesTemp = numViews;
                                    numViews = numReplies;
                                    numReplies = numRepliesTemp;
                                }

                                // Get starter
                                starter = starters.get(numStickies).text();

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

                                // Create topic object and add to array
                                Topic topicObj = new Topic(subject, starter, numReplies, numViews, lastPostCols.get(numStickies).text(), true, locked, hasUnread, id);
                                topicObj.setURL(topicURL);
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
                                int numReplies = 0;
                                int numViews = 0;
                                String starter;
                                boolean locked = false;
                                boolean hasUnread = false;

                                // Get replies data for stickies
                                for(Element replies : nonStickyTopicCols)
                                {
                                    if(replies.outerHtml().contains("<td class=\"windowbg3\" valign=\"middle\" width=\"4%\" align=\"center\">")) {
                                        numReplies = Integer.parseInt(replies.text());

                                        // Get views data for stickies
                                        for (Element views : nonStickyTopicCols) {
                                            if (Integer.parseInt(views.text()) != numReplies) {
                                                numViews = Integer.parseInt(views.text());
                                            }
                                        }
                                    }
                                }

                                // set views and replies to proper ones
                                if(numReplies > numViews)
                                {
                                    int numRepliesTemp = numViews;
                                    numViews = numReplies;
                                    numReplies = numRepliesTemp;
                                }

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

                                // Create topic object and add to array
                                Topic topicObj = new Topic(subject, starter, numReplies, numViews, lastPostCols.get(numStickies).text(), true, locked, hasUnread, id);
                                topicObj.setURL(topicURL);
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

            return out;
        }

        @Override
        protected void onPostExecute(final List<List<Object>> result) {

            mGetTopicsTask = null;
            showProgress(false);

            if (result.size() > 0) {
                List<Object> childBoards = result.get(0);
                List<Object> topics = result.get(1);
                mChildBoards = new ArrayList<Board>();
                mTopics = new ArrayList<Topic>();

                for (Object childBoard : childBoards) {
                    mChildBoards.add((Board) childBoard);
                }

                for (Object topic : topics) {
                    mTopics.add((Topic) topic);
                }
            } else
            {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }

            CustomListAdapter mListAdp = new CustomListAdapter(mTopics, mChildBoards);
            mListView.setAdapter(mListAdp);

            mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast toast = Toast.makeText(getContext(), "CLICKICICICICI", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

}
