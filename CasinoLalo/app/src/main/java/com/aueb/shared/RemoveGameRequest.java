package com.aueb.shared;
import java.io.Serializable;

public class RemoveGameRequest implements Serializable 
{
    private String gameName;

    public RemoveGameRequest(String gameName)
    {
        this.gameName = gameName;
    }

    public String getGameName()
    {
        return gameName;
    }
}
