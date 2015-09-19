package com.achow101.bitcointalkforum;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

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
    private List<Board> childBoards;
    private List<Topic> topics;
    private String mBoardURL;
    private String mSessId;

    public static BoardTopicFragment newInstance(String boardURL, String sessId) {
        BoardTopicFragment fragment = new BoardTopicFragment();
        Bundle args = new Bundle();
        args.putString("URL", boardURL);
        args.putString("SessID", sessId);
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
        mListView = (ListView) view.findViewById(android.R.id.list);

        // Get the topics from Bitcointalk


        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnListInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnListInteraction");
        }
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

        public CustomListAdapter(List<Topic> topics)
        {
            this.topics = topics;
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
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
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
            return null;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 0;
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
                        // TODO: Get topics Stuff
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

        }
    }

}
