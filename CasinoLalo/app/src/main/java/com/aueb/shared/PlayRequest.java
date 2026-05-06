package com.aueb.shared;
import java.io.Serializable;

public class PlayRequest implements Serializable
{
    private String playerName;
    private String gameName;
    private double betAmount;

    public PlayRequest(String playerName , String gameName , double betAmount)
    {
        this.playerName = playerName;
        this.betAmount = betAmount;
        this.gameName = gameName;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public String getGameName()
    {
        return gameName;
    }

    public double getBetAmount()
    {
        return betAmount;
    }
}