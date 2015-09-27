package com.achow101.bitcointalkforum.items;

import android.graphics.Bitmap;

/**
 * Created by Andy on 9/26/2015.
 */
public class Poster {

    private String name;
    private Bitmap avatar;
    private String personalText;
    private String activityStr;
    private String rank;
    private String specialPosition = null;

    public Poster(String name, Bitmap avatar, String personalText, String activityStr, String rank)
    {
        this.name = name;
        this.avatar = avatar;
        this.personalText = personalText;
        this.activityStr = activityStr;
        this.rank = rank;
    }

    public boolean isSpecial()
    {
        return specialPosition != null;
    }

    public void setSpecialPos(String specialPosition)
    {
        this.specialPosition = specialPosition;
    }

    public String getSpecialPos()
    {
        return specialPosition;
    }

    public String getName()
    {
        return name;
    }

    public Bitmap getAvatar()
    {
        return avatar;
    }

    public String getPersonalText()
    {
        return personalText;
    }

    public String getActivityStr()
    {
        return activityStr;
    }

    public String getRank()
    {
        return rank;
    }
}
