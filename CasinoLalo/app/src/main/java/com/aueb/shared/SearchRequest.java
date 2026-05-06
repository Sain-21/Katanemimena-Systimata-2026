package com.aueb.shared;
import java.io.Serializable;

public class SearchRequest implements Serializable 
{
    private String playerID;
    private String gameName;
    private double minStars;
    private String riskLevel;
    private String betLimit;
    private String requestId;

    public SearchRequest(String playerID, double minStars, String riskLevel, String betLimit) 
    {
        this.playerID = playerID;
        this.minStars = minStars;
        this.riskLevel = riskLevel;
        this.betLimit = betLimit;
        this.gameName = null;
        this.requestId = "REQ_" + System.currentTimeMillis();
    }

    public SearchRequest(String gameName) 
    {
        this.gameName = gameName;
        this.requestId = "REQ_" + System.currentTimeMillis();
    }

    public String getRequestId()
    {
        return requestId;
    }

    public String getGameName() 
    { 
        return gameName; 
    }

    public double getMinStars() 
    { 
        return minStars; 
    }

    public String getRiskLevel() 
    { 
        return riskLevel; 
    }
    
    public String getBetLimit() 
    { 
        return betLimit; 
    }
}