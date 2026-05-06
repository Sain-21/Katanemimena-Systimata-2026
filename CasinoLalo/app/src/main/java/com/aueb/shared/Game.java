package com.aueb.shared;
import java.io.Serializable;
import java.util.HashSet;



public class Game implements Serializable
{
    private static final long serialVersionUID = 1L;
    private HashSet<String> voters = new HashSet<>();
    private String gameName;

    private String providerName;

    private double stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;

    private int jackpot;
    private double totalBets = 0;
    private double totalPayouts = 0;

    private static final double[] lowRiskTable = {0.0 , 0.0 , 0.0 , 0.1 , 0.5 , 1.0 , 1.1 , 1.3 , 2.0 , 2.5};
    private static final double[] medRiskTable = {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.5 , 1.0 , 1.5 , 2.5 , 3.5};
    private static final double[] highRiskTable = {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 1.0 , 2.0 , 6.5};

    public Game() {}

    public Game(String gameName, String providerName, double stars, int noOfVotes, String gameLogo,
                double minBet, double maxBet, String riskLevel, String hashKey) 
    {
        this.gameName = gameName;
        this.providerName = providerName;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.gameLogo = gameLogo;
        this.minBet = minBet;
        this.maxBet = maxBet;
        setRiskLevel(riskLevel);
        this.hashKey = hashKey;
    }

    public String getGameName() 
    {
        return gameName;
    }

    public String getProviderName() 
    {
        return providerName;
    }

    public double getStars() 
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

    public double getTotalBets() 
    {
        return totalBets;
    }

    public double getTotalPayouts() 
    {
        return totalPayouts;
    }

    public void setGameName(String gameName) 
    {
        this.gameName = gameName;
    }

    public void setProviderName(String providerName) 
    {
        this.providerName = providerName;
    }

    public void setStars(double stars) 
    {
        this.stars = stars;
    }

    public void setNoOfVotes(int noOfVotes) 
    {
        this.noOfVotes = noOfVotes;
    }

    public void setGameLogo(String gameLogo) 
    {
        this.gameLogo = gameLogo;
    }

    public void setMinBet(double minBet) 
    {
        this.minBet = minBet;
    }

    public void setMaxBet(double maxBet) 
    {
        this.maxBet = maxBet;
    }

    public void setRiskLevel(String riskLevel) 
    {
        if (riskLevel == null || riskLevel.trim().isEmpty()) 
        {
            this.riskLevel = "low";
        } 
        else 
        {
            this.riskLevel = riskLevel.toLowerCase();
        }

        if (this.riskLevel.equals("high")) this.jackpot = 40;
        else if (this.riskLevel.equals("medium")) this.jackpot = 20;
        else this.jackpot = 10;
    }

    public void setHashKey(String hashKey) 
    {
        this.hashKey = hashKey;
    }

    public void setTotalBets(double totalBets) 
    {
        this.totalBets = totalBets;
    }

    public void setTotalPayouts(double totalPayouts) 
    {
        this.totalPayouts = totalPayouts;
    }

    public synchronized void addPlay(double bet, double payout) 
    {
        this.totalBets += bet;
        this.totalPayouts += payout;
    }

    public synchronized boolean addRating(String playerName , int newStars) 
    {   
        if(voters.contains(playerName))
        {
            return false;
        }

        double currentTotal = this.stars * this.noOfVotes;
        this.noOfVotes++;
        this.stars = (currentTotal + newStars) / this.noOfVotes;
        voters.add(playerName);
        return true;
    }

    public double getProfit() 
    {
        return this.totalBets - this.totalPayouts;
    }

    public double getMultiplier(int i) 
    {
        if (this.riskLevel.equals("low")) 
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

    @Override
    public String toString() 
    {
        return "Game: " + gameName + " [" + getBetCategory() + "] | Jackpot: " + jackpot + "x | Risk: " + riskLevel;
    }
}