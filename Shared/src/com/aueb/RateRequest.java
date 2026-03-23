package com.aueb;

import java.io.Serializable;

public class RateRequest implements Serializable 
{
    private String gameName;
    private int stars;
    
    public RateRequest(String gameName, int stars) 
    {
        this.gameName = gameName;
        this.stars = stars;
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
   
}