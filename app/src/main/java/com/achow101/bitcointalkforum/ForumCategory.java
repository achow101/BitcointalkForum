package com.achow101.bitcointalkforum;

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
