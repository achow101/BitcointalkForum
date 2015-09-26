package com.achow101.bitcointalkforum.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.Board;
import com.achow101.bitcointalkforum.items.ForumCategory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private List<ForumCategory> mCategories;
    private List<Board> mBoards;
    private GetHomePage mGetHomePageDataTask = null;
    private String mSessId;
    private ProgressBar mProgressView;
    private ExpandableListView mExpListView;
    private GoToBoard mBoardCallback;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HomeFragment newInstance(String sessId) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("sessid", sessId);
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSessId = getArguments().getString("sessid");
        mProgressView = (ProgressBar) rootView.findViewById(R.id.loading_progress);

        mExpListView = (ExpandableListView) rootView.findViewById(R.id.homeExpList);

        // Get data for hompage
        showProgress(true);
        mGetHomePageDataTask = new GetHomePage(mSessId);
        mGetHomePageDataTask.execute((Void) null);

        return rootView;
    }

    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        try {
            mBoardCallback = (GoToBoard) activity;
        }
        catch(ClassCastException e)
        {
            throw new ClassCastException(activity.toString() + " must implement GoToBoard");
        }


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

            mExpListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mExpListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mExpListView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mExpListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class GetHomePage extends AsyncTask<Void, Void, List<List<Object>>> {

        private String mSessId;

        public GetHomePage(String sessId) {
            mSessId = sessId;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... params) {
            ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            List<Object> categories = new ArrayList<Object>();
            List<Object> boards = new ArrayList<Object>();
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                try {
                    // Get homepage of Bitcointalk
                    Document doc = Jsoup.connect("https://bitcointalk.org/index.php").cookie("PHPSESSID", mSessId).get();
                    //Document doc = Jsoup.connect("https://bitcointalk.org/index.php").get();

                    // Get category elements
                    Elements cats = doc.getElementsByClass("tborder");
                    for (int catNum = 0; catNum < cats.size(); catNum++) {
                        Element cat = cats.get(catNum);
                        // Get the category header and text
                        Elements headers = cat.getElementsByClass("catbg2");
                        String category = headers.text();
                        if (category.length() != 0) {
                            List<Board> boardTitlesList = new ArrayList<Board>();

                            // Get all of the board elements on this category
                            Elements boardClass = cat.getElementsByClass("windowbg2");
                            for (Element board : boardClass) {
                                // Get elements containing board titles
                                Elements boardTitles = board.getElementsByTag("a");
                                for (Element boardTitle : boardTitles) {
                                    // Get the title from each board
                                    String boardStr = boardTitle.outerHtml();
                                    if (boardStr.contains("index.php?board=")) {
                                        // Create board obj
                                        int startInx = boardStr.indexOf("board=") + 6;
                                        int endInx = startInx + 1;
                                        Board boardObj = new Board(boardTitle.text(), category, Integer.parseInt(boardStr.substring(startInx, endInx)), boardTitlesList.size());

                                        // Get URL and set it in the object
                                        boardObj.setURL(boardTitle.attr("href"));

                                        // Add board to list
                                        boards.add(boardObj);
                                        boardTitlesList.add(boardObj);
                                    }
                                }
                            }

                            // Create forum category object
                            categories.add(new ForumCategory(category, boardTitlesList, boardTitlesList.size(), catNum - 1));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            List<List<Object>> out = new ArrayList<List<Object>>();
            out.add(categories);
            out.add(boards);

            return out;
        }

        @Override
        protected void onPostExecute(final List<List<Object>> result) {
            mGetHomePageDataTask = null;
            showProgress(false);

            if (result.size() > 0) {
                List<Object> categories = result.get(0);
                List<Object> boards = result.get(1);
                mCategories = new ArrayList<ForumCategory>();
                mBoards = new ArrayList<Board>();

                // Convert each object in list to category and add to global list
                for (Object category : categories) {
                    mCategories.add((ForumCategory) category);
                }

                // convert each object in list to board and add to global list
                for (Object board : boards) {
                    mBoards.add((Board) board);
                }
            } else {
                Toast toast = Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG);
                toast.show();
            }

            // Setup Expandable Listview for home
            ExpandableListAdapter mExpListAdp = new ExpandableListAdapter(getContext(), mCategories, mBoards);
            mExpListView.setAdapter(mExpListAdp);

            // Expand everything
            mExpListView.expandGroup(0);
            mExpListView.expandGroup(1);
            mExpListView.expandGroup(2);
            mExpListView.expandGroup(3);
            mExpListView.expandGroup(4);

            // Set the click listener
            mExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                    // Retrieve the URL for the board
                    Board board = mCategories.get(groupPosition).getBoards().get(childPosition);
                    String boardURL = board.getURL();

                    // Replace this fragment with one for the board
                    mBoardCallback.OnBoardSelected(boardURL, board.getName());

                    return true;
                }
            });
        }

        @Override
        protected void onCancelled() {
            mGetHomePageDataTask = null;
            showProgress(false);
        }
    }

    public static class ExpandableListAdapter extends BaseExpandableListAdapter {
        Context context;
        List<ForumCategory> categories;
        List<Board> boards;

        public ExpandableListAdapter(Context context, List<ForumCategory> categories, List<Board> boards) {
            this.context = context;
            this.categories = categories;
            this.boards = boards;
        }

        @Override
        public int getGroupCount() {
            return categories.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            int out = 0;
            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    out = cat.getBoards().size();
                    break;
                }
            }
            return out;
        }

        @Override
        public Object getGroup(int groupPosition) {
            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    return cat;
                }
            }
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    for (Board board : cat.getBoards()) {
                        if (board.getPos() == childPosition) {
                            return board;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    for (Board board : cat.getBoards()) {
                        if (board.getPos() == childPosition) {
                            return board.getId();
                        }
                    }
                }
            }
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = infalInflater.inflate(R.layout.category_layout, null);
            }

            TextView catTitle = (TextView) v.findViewById(R.id.cat_title);

            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    catTitle.setText(cat.getName());
                    break;
                }
            }

            return v;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.board_list_layout, parent, false);
            }

            TextView boardName = (TextView) v.findViewById(R.id.board_title);

            for (ForumCategory cat : categories) {
                if (cat.getId() == groupPosition) {
                    for (Board board : cat.getBoards()) {
                        if (board.getPos() == childPosition) {
                            boardName.setText(board.getName());
                            break;
                        }
                    }
                }
            }
            return v;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    public interface GoToBoard
    {
        public void OnBoardSelected(String boardURL, String category);
    }
}