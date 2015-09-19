package com.achow101.bitcointalkforum;

import java.util.Date;

/**
 * Created by Andy on 9/17/2015.
 */
public class Topic {

    private String subject;
    private String starter;
    private int replies;
    private int views;
    private String lastPoster;
    private Date lastPostDate;

    public Topic(String subject, String starter, int replies, int views, String lastPoster, Date lastPostDate) {
        this.subject = subject;
        this.starter = starter;
        this.replies = replies;
        this.views = views;
        this.lastPoster = lastPoster;
        this.lastPostDate = lastPostDate;
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

    public String getLastPoster()
    {
        return lastPoster;
    }

    public Date getLastPostDate()
    {
        return lastPostDate;
    }
}
