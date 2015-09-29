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
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by Andy on 9/26/2015.
 */
public class Poster {

    private String name;
    private BitmapDrawable avatar;
    private String personalText;
    private String activityStr;
    private String rank;
    private String specialPosition = null;

    public Poster(String name, BitmapDrawable avatar, String personalText, String activityStr, String rank)
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

    public BitmapDrawable getAvatar()
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
