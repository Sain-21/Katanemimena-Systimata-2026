package com.aueb.shared;
import java.io.Serializable;

public class SearchRequest implements Serializable 
{
    private String playerID;
    private String gameName; // apeuthias anazitisi
    private double minStars;
    private String riskLevel;
    private String betLimit;

    // Constructor gia anazitisi me filtra
    public SearchRequest(String playerID, double minStars, String riskLevel, String betLimit) 
    {
        this.playerID = playerID;
        this.minStars = minStars;
        this.riskLevel = riskLevel;
        this.betLimit = betLimit;
        this.gameName = null;
    }

    // Constructor gia anazitisi mono me onoma(gia tin playAction)
    public SearchRequest(String gameName) 
    {
        this.gameName = gameName;
    }

    // Getters
    public String getGameName() { return gameName; }
    public double getMinStars() { return minStars; }
    public String getRiskLevel() { return riskLevel; }
    public String getBetLimit() { return betLimit; }
}