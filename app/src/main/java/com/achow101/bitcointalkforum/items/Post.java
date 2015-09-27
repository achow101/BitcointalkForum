package com.achow101.bitcointalkforum.items;

import android.graphics.Bitmap;
import android.text.Spanned;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Andy on 9/26/2015.
 */
public class Post {

    private Poster poster;
    private String postURL;
    private String postedTime;
    private Spanned postBody;
    private String subject;
    private long id;

    public Post(Poster poster, String postURL, String postedTime, String subject, Spanned postBody, long id)
    {
        this.poster = poster;
        this.postURL = postURL;
        this.postedTime = postedTime;
        this.postBody = postBody;
        this.subject = subject;
        this.id = id;
    }

    public Poster getPoster()
    {
        return poster;
    }

    public String getPostURL()
    {
        return postURL;
    }

    public Spanned getPostBody()
    {
        return postBody;
    }


    public String getSubject()
    {
        return subject;
    }

    public long getId()
    {
        return id;
    }

    public String getPostedTime()
    {
        return postedTime;
    }

}
