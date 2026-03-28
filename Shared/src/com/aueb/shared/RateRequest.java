package com.aueb.shared;

import java.io.Serializable;

public class RateRequest implements Serializable 
{
    private String gameName;
    private int stars;
    private String playerName;
    
    public RateRequest(String gameName, String playerName , int stars) 
    {
        this.gameName = gameName;
        this.stars = stars;
        this.playerName = playerName;
    }

    public String getGameName() 
    {
        return gameName;
    }
    
    public void setGameName(String gameName) 
    {
        this.gameName = gameName;
    }

    public int getStars() 
    {
        return stars;
    }

    public void setStars(int stars) 
    {
        this.stars = stars;
    }

    public String getPlayerName()
    {
        return playerName;
    }
   
}