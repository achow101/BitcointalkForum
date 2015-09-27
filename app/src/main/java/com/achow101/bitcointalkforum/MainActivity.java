package com.achow101.bitcointalkforum;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.achow101.bitcointalkforum.fragments.BoardTopicFragment;
import com.achow101.bitcointalkforum.fragments.HomeFragment;
import com.achow101.bitcointalkforum.fragments.NavigationDrawerFragment;
import com.achow101.bitcointalkforum.fragments.TopicFragment;
import com.achow101.bitcointalkforum.fragments.UnreadPostListsFragment;
import com.achow101.bitcointalkforum.items.Board;
import com.achow101.bitcointalkforum.items.ForumCategory;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        HomeFragment.GoToBoard,
        BoardTopicFragment.OnTopicListInteraction,
        UnreadPostListsFragment.OnUnreadListInteraction {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    protected String sessId;
    private List<ForumCategory> mCategories;
    private List<Board> mBoards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the session id from intent
        Intent intent = getIntent();
        sessId = intent.getStringExtra("SESSION ID");


        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        switch(position)
        {
            // Home
            case 0: fragmentManager.beginTransaction().replace(R.id.container, HomeFragment.newInstance(sessId)).commit();
                break;
            // Unread posts
            case 1: fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=unread;start=0", sessId)).commit();
                break;
            // New replies
            case 2: fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=unreadreplies;start=0", sessId)).commit();
                break;
            // Watchlist
            case 3: fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=watchlist;start=0", sessId)).commit();
                break;
            // Profile
            case 4://TODO: Create fragment for this
                break;
            // Messages
            case 5: //TODO: Create fragment for this
                break;
            // Logout
            case 7:
                Intent intentLogout = new Intent(this,LoginActivity.class);
                intentLogout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentLogout);
                finish();
                break;
        }

    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO: Need to add caching forum pages

    @Override
    public void OnBoardSelected(String boardURL, String category) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, BoardTopicFragment.newInstance(boardURL, sessId, category)).commit();
    }

    @Override
    public void onTopicSelected(String topicURL) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, TopicFragment.newInstance(topicURL, sessId)).commit();
    }

    @Override
    public void onPrevNextPageSelected(String boardURL) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance(boardURL, sessId)).commit();
    }

    @Override
    public void onChildBoardSelected(String boardURL, String category) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, BoardTopicFragment.newInstance(boardURL, sessId, category)).commit();
    }
}
