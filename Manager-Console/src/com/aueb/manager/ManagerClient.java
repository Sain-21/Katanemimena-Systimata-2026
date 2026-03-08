package com.aueb.manager;

import com.aueb.shared.Game;
import java.io.*;
import java.net.*;

public class ManagerClient 
{
    public static void main(String[] args) 
    {
        String host = "localhost";
        int port = 1312;

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) 
            {

            System.out.println("Connected to Master Server!");

            // 1. Δημιουργούμε ένα δοκιμαστικό παιχνίδι
            Game testGame = new Game(
                "Book of Dead", "Play'n GO", 5, 1200, 
                "logo.png", 0.10, 100.0, "high", "secret123"
            );

            // 2. Το στέλνουμε στον Master
            System.out.println("Sending game: " + testGame.getGameName());
            out.writeObject(testGame);
            out.flush();

            System.out.println("Game sent successfully!");

        } 
        catch (IOException e) 
        {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}