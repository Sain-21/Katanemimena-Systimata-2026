package com.aueb.shared;

import java.io.Serializable;

public class SearchRequest implements Serializable
{
    private String gameName;

    public SearchRequest(String gameName)
    {
        this.gameName = gameName;
    }

    public String getGameName()
    {
        return gameName;
    }
}