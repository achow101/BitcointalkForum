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
    private String postedTime;
    private Spanned postBody;
    private String subject;
    private long id;

    public Post(Poster poster, String postedTime, String subject, Spanned postBody, long id)
    {
        this.poster = poster;
        this.postedTime = postedTime;
        this.postBody = postBody;
        this.subject = subject;
        this.id = id;
    }

    public Poster getPoster()
    {
        return poster;
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
