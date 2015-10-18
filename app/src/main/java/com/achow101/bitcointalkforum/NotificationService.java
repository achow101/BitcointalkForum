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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;

import com.achow101.bitcointalkforum.MainActivity;
import com.achow101.bitcointalkforum.R;
import com.achow101.bitcointalkforum.items.Post;
import com.achow101.bitcointalkforum.items.Poster;
import com.achow101.bitcointalkforum.items.Topic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NotificationService extends Service {

    private PowerManager.WakeLock mWakeLock;
    private String sound;
    private boolean vibrate;

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService");
        mWakeLock.acquire();

        // get sessid and username from intent
        String sessId = intent.getStringExtra("SESSION ID");
        String mUsername = intent.getStringExtra("Username");

        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) {
            stopSelf();
            return;
        }

        // Get preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Check that notifications are wanted
        boolean getNotifs = prefs.getBoolean("notifications_new_message", true);

        // do stuff if notifications are wanted
        if(getNotifs)
        {
            // get remaining booleans
            boolean getWatchlist = prefs.getBoolean("notifications_watchlist", true);
            boolean getUnreadposts = prefs.getBoolean("notifications_unreadposts", false);
            boolean getUnreadreplies = prefs.getBoolean("notifications_unreadreplies", false);
            boolean getMessages = prefs.getBoolean("notifications_messages", true);

            // Get notification settings stuff
            sound = prefs.getString("notifications_new_message_ringtone", "content://settings/system/notification_sound");
            vibrate = prefs.getBoolean("notifications_new_message_vibrate", true);

            // do the actual work, in a separate thread
            new PollTask(getWatchlist, getUnreadposts, getUnreadreplies, getMessages, sessId, mUsername).execute();
        }
    }

    private class PollTask extends AsyncTask<Void, Void, List<List<Object>>> {

        private boolean getWatchlist;
        private boolean getUnreadPosts;
        private boolean getUnreadReplies;
        private boolean getMessages;
        private String sessId;
        private String mUsername;

        public PollTask(boolean getWatchlist, boolean getUnreadPosts, boolean getUnreadReplies, boolean getMessages, String sessId, String mUsername) {

            this.getWatchlist = getWatchlist;
            this.getUnreadPosts = getUnreadPosts;
            this.getUnreadReplies = getUnreadReplies;
            this.getMessages = getMessages;
            this.sessId = sessId;
            this.mUsername = mUsername;

        }

        @Override
        protected List<List<Object>> doInBackground(Void... params) {

            System.out.println("Begin notifications fetch");

            List<List<Object>> out = new ArrayList<List<Object>>();
            List<Object> watchlistNotifTopics = new ArrayList<Object>();
            List<Object> unreadPostNotifTopics = new ArrayList<Object>();
            List<Object> unreadRepliesNotifTopics = new ArrayList<Object>();
            List<Object> messageNotif = new ArrayList<Object>();

            // Check watchlist
            if (getWatchlist) {
                try {

                    // Read the local file and get the first lastpostcol data
                    FileInputStream fis = getApplicationContext().openFileInput("watchlist.html");
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    Document localDoc = Jsoup.parse(sb.toString());
                    Element localBody = localDoc.getElementById("bodyarea");
                    Elements localWindowbg2Elements = localBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> localLastPostCols = new ArrayList<Element>();
                    for (Element elem : localWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            localLastPostCols.add(elem);
                        }
                    }



                    // Get the page and get the first lastpostcol data
                    Document remoteDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=watchlist;start=0").cookie("PHPSESSID", sessId).get();
                    Element remoteBody = remoteDoc.getElementById("bodyarea");
                    Elements remoteWindowbg2Elements = remoteBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> remoteLastPostCols = new ArrayList<Element>();
                    for (Element elem : remoteWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            remoteLastPostCols.add(elem);
                        }
                    }

                    // write remoteDoc to file
                    try {
                        FileOutputStream os = getApplicationContext().openFileOutput("watchlist.html", Context.MODE_PRIVATE);
                        os.write(remoteDoc.html().getBytes());
                        os.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // If they are the not same, get everything
                    if (!localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty() && !localLastPostCols.get(0).text().equals(remoteLastPostCols.get(0).text())) {
                        List<Topic> localTopics = getTopicsFromBody(localBody);
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        watchlistNotifTopics.addAll(remoteTopics);

                        // Compare all the posts and find the ones different in the downloaded data
                        for (Topic localTopic : localTopics) {
                            for (Topic remoteTopic : remoteTopics) {
                                if (remoteTopic.getLastPostURL().equals(localTopic.getLastPostURL())) {
                                    watchlistNotifTopics.remove(remoteTopic);
                                }
                            }
                        }
                    }
                    else if(localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty())
                    {
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        watchlistNotifTopics.addAll(remoteTopics);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Check Unreadposts
            if (getUnreadPosts) {
                try {

                    // Read the local file and get the first lastpostcol data
                    FileInputStream fis = getApplicationContext().openFileInput("unreadposts.html");
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    Document localDoc = Jsoup.parse(sb.toString());
                    Element localBody = localDoc.getElementById("bodyarea");
                    Elements localWindowbg2Elements = localBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> localLastPostCols = new ArrayList<Element>();
                    for (Element elem : localWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            localLastPostCols.add(elem);
                        }
                    }

                    // Get the page and get the first lastpostcol data
                    Document remoteDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=unread;start=0").cookie("PHPSESSID", sessId).get();
                    Element remoteBody = remoteDoc.getElementById("bodyarea");
                    Elements remoteWindowbg2Elements = remoteBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> remoteLastPostCols = new ArrayList<Element>();
                    for (Element elem : remoteWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            remoteLastPostCols.add(elem);
                        }
                    }

                    // write remoteDoc to file
                    try {
                        FileOutputStream os = getApplicationContext().openFileOutput("unreadposts.html", Context.MODE_PRIVATE);
                        os.write(remoteDoc.html().getBytes());
                        os.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // If they are the not same, get everything
                    if (!localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty() && !localLastPostCols.get(0).text().equals(remoteLastPostCols.get(0).text())) {
                        List<Topic> localTopics = getTopicsFromBody(localBody);
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        unreadPostNotifTopics.addAll(remoteTopics);

                        // Compare all the posts and find the ones different in the downloaded data
                        for (Topic localTopic : localTopics) {
                            for (Topic remoteTopic : remoteTopics) {
                                if (remoteTopic.getLastPostURL().equals(localTopic.getLastPostURL())) {
                                    unreadPostNotifTopics.remove(remoteTopic);
                                }
                            }
                        }
                    }
                    else if(localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty())
                    {
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        unreadPostNotifTopics.addAll(remoteTopics);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Check Unread Replies
            if (getUnreadReplies) {
                try {

                    // Read the local file and get the first lastpostcol data
                    FileInputStream fis = getApplicationContext().openFileInput("unreadreplies.html");
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    Document localDoc = Jsoup.parse(sb.toString());
                    Element localBody = localDoc.getElementById("bodyarea");
                    Elements localWindowbg2Elements = localBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> localLastPostCols = new ArrayList<Element>();
                    for (Element elem : localWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            localLastPostCols.add(elem);
                        }
                    }

                    // Get the page and get the first lastpostcol data
                    Document remoteDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=unreadreplies;start=0").cookie("PHPSESSID", sessId).get();
                    Element remoteBody = remoteDoc.getElementById("bodyarea");
                    Elements remoteWindowbg2Elements = remoteBody.select("table.bordercolor > tbody > tr > td > table.bordercolor > tbody > tr:not(titlebg) > td.windowbg2");
                    List<Element> remoteLastPostCols = new ArrayList<Element>();
                    for (Element elem : remoteWindowbg2Elements) {
                        if (elem.outerHtml().contains("<td class=\"windowbg2\" valign=\"middle\" width=\"22%\">")) {
                            remoteLastPostCols.add(elem);
                        }
                    }

                    // write remoteDoc to file
                    try {
                        FileOutputStream os = getApplicationContext().openFileOutput("unreadreplies.html", Context.MODE_PRIVATE);
                        os.write(remoteDoc.html().getBytes());
                        os.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // If they are the not same, get everything
                    if (!localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty() && !localLastPostCols.get(0).text().equals(remoteLastPostCols.get(0).text())) {
                        List<Topic> localTopics = getTopicsFromBody(localBody);
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        unreadRepliesNotifTopics.addAll(remoteTopics);

                        // Compare all the posts and find the ones different in the downloaded data
                        for (Topic localTopic : localTopics) {
                            for (Topic remoteTopic : remoteTopics) {
                                if (remoteTopic.getLastPostURL().equals(localTopic.getLastPostURL())) {
                                    unreadRepliesNotifTopics.remove(remoteTopic);
                                }
                            }
                        }
                    }
                    else if(localLastPostCols.isEmpty() && !remoteLastPostCols.isEmpty())
                    {
                        List<Topic> remoteTopics = getTopicsFromBody(remoteBody);

                        unreadRepliesNotifTopics.addAll(remoteTopics);
                    }


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Check messages
            if (getMessages)
            {
                try {

                    // Read the local file and get the first lastpostcol data
                    FileInputStream fis = getApplicationContext().openFileInput("messages.html");
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    Document localDoc = Jsoup.parse(sb.toString());
                    Element localBody = localDoc.select("div#bodyarea").first();
                    Elements localDateElements = localBody.select("table > tbody > tr > td > form > table.bordercolor > tbody > tr.windowbg > td");
                    String localFirstDate = localDateElements.get(1).text();

                    // Get the page and get the first lastpostcol data
                    Document remoteDoc = Jsoup.connect("https://bitcointalk.org/index.php?action=pm;f=inbox;sort=date;desc;start=0").cookie("PHPSESSID", sessId).get();
                    Element remoteBody = remoteDoc.select("div#bodyarea").first();
                    Elements remoteDateElements = remoteBody.select("table > tbody > tr > td > form > table.bordercolor > tbody > tr.windowbg > td");
                    String remoteFirstDate = remoteDateElements.get(1).text();

                    // write remoteDoc to file
                    try {
                        FileOutputStream os = getApplicationContext().openFileOutput("messages.html", Context.MODE_PRIVATE);
                        os.write(remoteDoc.html().getBytes());
                        os.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // If they are the not same, get everything
                    if (!remoteFirstDate.equals(localFirstDate)) {
                        List<Post> localTopics = getMessagesFromDoc(localDoc);
                        List<Post> remoteTopics = getMessagesFromDoc(remoteDoc);

                        messageNotif.addAll(remoteTopics);

                        // Compare all the posts and find the ones different in the downloaded data
                        for (Post localTopic : localTopics) {
                            for (Post remoteTopic : remoteTopics) {
                                if (remoteTopic.getPostedTime().equals(localTopic.getPostedTime())) {
                                    messageNotif.remove(remoteTopic);
                                }
                            }
                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            out.add(watchlistNotifTopics);
            out.add(unreadPostNotifTopics);
            out.add(unreadRepliesNotifTopics);
            out.add(messageNotif);

            return out;
        }

        private List<Post> getMessagesFromDoc(Document doc)
        {
            List<Post> posts = new ArrayList<Post>();

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
                String replyURL = pmHeadRight.select("a[href]").get(0).attr("href");
                String deleteURL = pmHeadRight.select("a[href]").get(1).attr("href");

                // Get subject and post times
                String subject = pmHead.select("b").first().text();
                String pmedTime = pmHead.text().replaceAll(subject, "");
                pmedTime = pmedTime.substring(pmedTime.indexOf("on:") + 3, pmedTime.indexOf("»"));

                // Get elements with post and header
                Element pm = post.select("div.personalmessage").first();

                // Get body of post
                String postBodyStr = pm.html();
                Spanned postBody = Html.fromHtml(postBodyStr);

                // Create post object
                Post postObj = new Post(poster, pmedTime, subject, postBody, ids.get(i), quoteURL, replyURL, deleteURL);
                posts.add(postObj);
            }

            return posts;
        }

        private List<Topic> getTopicsFromBody(Element body)
        {
            List<Topic> topics = new ArrayList<Topic>();
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

            return topics;
        }

        @Override
        protected void onPostExecute(List<List<Object>> result) {

            int numMessages = 0;
            for(List<Object> list : result)
            {
                if(!list.isEmpty())
                {
                    numMessages += list.size();
                }
            }
            if (numMessages > 0) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher));

                // Set the title String with number of messages
                mBuilder.setContentTitle(numMessages + " Messages");

                // Set the content of notification to be title of first of each
                String text = "";
                for(int i = 0; i < result.size(); i++)
                {
                    List<Object> list = result.get(i);
                    if(!list.isEmpty())
                    {
                        if(i == 3)
                        {
                            text = text + ((Post)list.get(0)).getSubject() + "\n";
                        }
                        else
                        {
                            text = text + ((Topic)list.get(0)).getSubject() + "\n";
                        }
                    }
                }
                mBuilder.setContentText(text);
                mBuilder.setSound(Uri.parse(sound));
                if(vibrate)
                    mBuilder.setVibrate(new long[] {0, 400, 50, 200});

                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                resultIntent.putExtra("SESSION ID", sessId);
                resultIntent.putExtra("Username", mUsername);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                // mId allows you to update the notification later on.
                mNotificationManager.notify(1, mBuilder.build());
            }

            stopSelf();
        }
    }

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }
}
