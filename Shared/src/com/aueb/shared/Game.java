package com.aueb.shared;

import java.io.Serializable;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;
    
    private String betCategory;
    private int jackpot;

    // constructor
    public Game(String gameName, String providerName, int stars, int noOfVotes, String gameLogo, double minBet, double maxBet, String riskLevel, String hashKey) {
        this.gameName = gameName;
        this.providerName = providerName;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.gameLogo = gameLogo;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.riskLevel = riskLevel.toLowerCase();
        this.hashKey = hashKey;
        
        // katigoria bet
        if (minBet >= 5.0) this.betCategory = "$$$";
        else if (minBet >= 1.0) this.betCategory = "$$";
        else this.betCategory = "$";

        // jackpot
        if (this.riskLevel.equals("high")) this.jackpot = 40;
        else if (this.riskLevel.equals("medium")) this.jackpot = 20;
        else this.jackpot = 10;
    }

    //getters
    public String getGameName() 
    {
        return gameName;
    }

    public String getProviderName() 
    {
        return providerName;
    }

    public int getStars() 
    {
        return stars;
    }

    public int getNoOfVotes() 
    {
        return noOfVotes;
    }

    public String getGameLogo() 
    {
        return gameLogo;
    }

    public double getMinBet() 
    {
        return minBet;
    }

    public double getMaxBet() 
    {
        return maxBet;
    }

    public String getRiskLevel() 
    {
        return riskLevel;
    }

    public String getHashKey() 
    {
        return hashKey;
    }

    public String getBetCategory()
    {
        return betCategory;
    }

    public int getJackpot() 
    {
        return jackpot;
    }
}