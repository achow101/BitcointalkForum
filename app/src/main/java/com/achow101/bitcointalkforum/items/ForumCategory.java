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

import com.achow101.bitcointalkforum.items.Board;

import java.util.List;

/**
 * Created by Andy on 9/16/2015.
 */
public class ForumCategory {

    private String name;
    private List<Board> boards;
    private int numBoards;
    private int id;

    public ForumCategory(String name, List<Board> boards, int numBoards, int id)
    {
        this.name = name;
        this.boards = boards;
        this.numBoards = numBoards;
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public List<Board> getBoards()
    {
        return boards;
    }

    public int getNumBoards()
    {
        return numBoards;
    }

    public int getId()
    {
        return id;
    }
}
