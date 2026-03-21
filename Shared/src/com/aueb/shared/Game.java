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
    private double totalBets = 0;
    private double totalPayouts = 0;

    private static final double[] lowRiskTable = {0.0 , 0.0 , 0.0 , 0.1 , 0.5 , 1.0 , 1.1 , 1.3 , 2.0 , 2.5};
    private static final double[] medRiskTable = {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.5 , 1.0 , 1.5 , 2.5 , 3.5};
    private static final double[] highRiskTable = {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 1.0 , 2.0 , 6.5};

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
        if (this.minBet >= 5.0) return "$$$";
        if (this.minBet >= 1.0) return "$$";
        return "$";
    }

    public int getJackpot() 
    {
        return jackpot;
    }

    public synchronized void addPlay(double bet , double payout)
    {
        this.totalBets += bet;
        this.totalPayouts += payout;
    }

    public double getProfit()
    {
        return totalBets - totalPayouts;
    }

    public double getMultiplier(int i)
    {
        if(this.riskLevel.equals("low"))
        {
            return lowRiskTable[i];
        }
        else if (this.riskLevel.equals("medium"))
        {
            return medRiskTable[i];
        }
        else
        {
            return highRiskTable[i];
        }
    }

    public double getTotalBets()
    {
        return totalBets;
    }

    public double getTotalPayouts()
    {
        return totalPayouts;
    }

    @Override
    public String toString()
    {
        return "Game: " + gameName + " [" + betCategory + "] | Jackpot: " + jackpot + "x | Risk: " + riskLevel; 
    }
}