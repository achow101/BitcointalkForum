package com.achow101.bitcointalkforum.items;

import java.net.URL;

/**
 * Created by Andy on 9/16/2015.
 */
public class Board {

    private String name;
    private String category;
    private int id;
    private int pos;
    private String url;

    public Board(String name, String category, int id, int pos)
    {
        this.name = name;
        this.category = category;
        this.id = id;
        this.pos = pos;
    }

    public String getName()
    {
        return name;
    }

    public String getCategory()
    {
        return category;
    }

    public int getId()
    {
        return id;
    }

    public int getPos()
    {
        return pos;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public String getURL()
    {
        return url;
    }

}
