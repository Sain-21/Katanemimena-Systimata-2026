package com.aueb.shared;
import java.io.Serializable;

public class SyncRequest implements Serializable 
{
    private String type; // "PLAY" ή "RATE"
    private String gameName;
    private String playerName;
    private double betAmount;
    private double payout;
    private int stars;

    // Κατασκευαστής για τον συγχρονισμό του Play
    public SyncRequest(String type, String gameName, String playerName, double betAmount, double payout) 
    {
        this.type = type;
        this.gameName = gameName;
        this.playerName = playerName;
        this.betAmount = betAmount;
        this.payout = payout;
    }

    // Κατασκευαστής για τον συγχρονισμό του Rate
    public SyncRequest(String type, String gameName, String playerName, int stars) 
    {
        this.type = type;
        this.gameName = gameName;
        this.playerName = playerName;
        this.stars = stars;
    }

    public String getType() { return type; }
    public String getGameName() { return gameName; }
    public String getPlayerName() { return playerName; }
    public double getBetAmount() { return betAmount; }
    public double getPayout() { return payout; }
    public int getStars() { return stars; }
}