package com.achow101.bitcointalkforum.items;

import java.util.Date;

/**
 * Created by Andy on 9/17/2015.
 */
public class Topic {

    private String subject;
    private String starter;
    private int replies;
    private int views;
    private String lastPost;
    private boolean sticky;
    private boolean locked;
    private boolean hasUnread;
    private String URL;
    private long id;
    private String lastPostURL;
    public Topic(String subject, String starter, int replies, int views, String lastPost, boolean sticky, boolean locked, boolean hasUnread, long id) {
        this.subject = subject;
        this.starter = starter;
        this.replies = replies;
        this.views = views;
        this.lastPost = lastPost;
        this.sticky = sticky;
        this.locked = locked;
        this.hasUnread = hasUnread;
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public String getStarter() {
        return starter;
    }

    public int getReplies()
    {
        return replies;
    }

    public int getViews()
    {
        return views;
    }

    public String getLastPost()
    {
        return lastPost;
    }

    public boolean isSticky()
    {
        return sticky;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public boolean hasUnreadPosts()
    {
        return hasUnread;
    }

    public void setURL(String URL)
    {
        this.URL = URL;
    }

    public String getUrl()
    {
        return URL;
    }

    public long getId()
    {
        return id;
    }

    public void setLastPostURL(String lastPostURL)
    {
        this.lastPostURL = lastPostURL;
    }

    public String getLastPostURL()
    {
        return lastPostURL;
    }
}
