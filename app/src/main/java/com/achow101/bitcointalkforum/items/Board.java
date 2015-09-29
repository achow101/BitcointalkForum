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
