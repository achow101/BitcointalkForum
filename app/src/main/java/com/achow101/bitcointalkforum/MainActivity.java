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

package com.achow101.bitcointalkforum;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.achow101.bitcointalkforum.fragments.BoardTopicFragment;
import com.achow101.bitcointalkforum.fragments.HomeFragment;
import com.achow101.bitcointalkforum.fragments.MessagesFragment;
import com.achow101.bitcointalkforum.fragments.NavigationDrawerFragment;
import com.achow101.bitcointalkforum.fragments.ProfileFragment;
import com.achow101.bitcointalkforum.fragments.ReplyFragment;
import com.achow101.bitcointalkforum.fragments.TopicFragment;
import com.achow101.bitcointalkforum.fragments.UnreadPostListsFragment;
import com.achow101.bitcointalkforum.items.Board;
import com.achow101.bitcointalkforum.items.ForumCategory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        HomeFragment.GoToBoard,
        BoardTopicFragment.OnTopicListInteraction,
        UnreadPostListsFragment.OnUnreadListInteraction,
        TopicFragment.OnTopicInteraction,
        MessagesFragment.OnPMInteraction,
        ReplyFragment.OnPostListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    protected String sessId;
    private List<ForumCategory> mCategories;
    private List<Board> mBoards;
    private String mUsername;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the session id from intent
        Intent intent = getIntent();
        sessId = intent.getStringExtra("SESSION ID");
        mUsername = intent.getStringExtra("Username");

        // immediately start downloading pages for notifications
        downloadPagesTask task = new downloadPagesTask(sessId);
        task.execute((Void) null);

        restoreActionBar();

        // write session id to file
        try {
            FileOutputStream os = getApplicationContext().openFileOutput("sessionid.txt", Context.MODE_PRIVATE);
            os.write(sessId.getBytes());
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write username to file
        try {
            FileOutputStream os = getApplicationContext().openFileOutput("username.txt", Context.MODE_PRIVATE);
            os.write(mUsername.getBytes());
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            case 0:
                fragmentManager.beginTransaction().replace(R.id.container, HomeFragment.newInstance(sessId)).commit();
                break;
            // Unread posts
            case 1:
                fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=unread;start=0", sessId)).commit();
                break;
            // New replies
            case 2:
                fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=unreadreplies;start=0", sessId)).commit();
                break;
            // Watchlist
            case 3:
                fragmentManager.beginTransaction().replace(R.id.container, UnreadPostListsFragment.newInstance("https://bitcointalk.org/index.php?action=watchlist;start=0", sessId)).commit();
                break;
            // Profile
            case 4:
                fragmentManager.beginTransaction().replace(R.id.container, ProfileFragment.newInstance(sessId)).commit();
                break;
            // Messages
            case 5:
                fragmentManager.beginTransaction().replace(R.id.container, MessagesFragment.newInstance(1, sessId)).commit();
                break;
            // Logout
            case 6:
                sessId = null;
                Intent intentLogout = new Intent(this, LoginActivity.class);
                intentLogout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentLogout);
                finish();
                break;
        }

    }

    public void restoreActionBar() {
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("Bitcoin Forum - " + mUsername);
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
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        else if (id == R.id.action_about)
        {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
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

    @Override
    public void onPageSelected(String topicURL) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, TopicFragment.newInstance(topicURL, sessId)).commit();
    }

    @Override
    public void onReplySelected(String replyURL) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, ReplyFragment.newInstance(replyURL, sessId)).commit();
    }

    @Override
    public void onPMPageSelected(int page) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, MessagesFragment.newInstance(page, sessId)).commit();
    }

    @Override
    public void onPostInteraction(Uri uri) {

    }

    public void onResume()
    {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int seconds = Integer.parseInt(prefs.getString("notifications_sync_freq", "60"));
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, NotificationService.class);
        i.putExtra("SESSION ID", sessId);
        i.putExtra("Username", mUsername);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        am.cancel(pi);

        if(prefs.getBoolean("notifications_new_message", true))
        {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + seconds*1000, seconds*1000, pi);
        }
    }

    private class downloadPagesTask extends AsyncTask<Void, Void, Void>
    {
        private String sessId;

        public downloadPagesTask(String sessId)
        {
            this.sessId = sessId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Document watchlistDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=watchlist;start=0").cookie("PHPSESSID", sessId).get();
                FileOutputStream os = getApplicationContext().openFileOutput("watchlist.html", Context.MODE_PRIVATE);
                os.write(watchlistDoc.html().getBytes());
                os.close();

                Document unreadPostsDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=unread;start=0").cookie("PHPSESSID", sessId).get();
                os = getApplicationContext().openFileOutput("unreadposts.html", Context.MODE_PRIVATE);
                os.write(unreadPostsDoc.html().getBytes());
                os.close();

                Document unreadRepliesDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=unreadreplies;start=0").cookie("PHPSESSID", sessId).get();
                os = getApplicationContext().openFileOutput("unreadreplies.html", Context.MODE_PRIVATE);
                os.write(unreadRepliesDoc.html().getBytes());
                os.close();

                Document messagesDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=pm;f=inbox;sort=date;desc;start=0").cookie("PHPSESSID", sessId).get();
                os = getApplicationContext().openFileOutput("messages.html", Context.MODE_PRIVATE);
                os.write(messagesDoc.html().getBytes());
                os.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Done pages download");

            return null;
        }
    }
}
