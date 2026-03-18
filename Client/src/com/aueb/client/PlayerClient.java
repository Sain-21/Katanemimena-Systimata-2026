package com.aueb.client;

import com.aueb.shared.Game;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class PlayerClient 
{
    public static void main(String[] args) 
    {
        String host = "localhost";
        int port = 1312;
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Welcome to CasinoLalo Search ===");
        
        while (true) 
        {
            System.out.print("\nEnter the name of the game you are looking for; (write 'exit' to exit): ");
            String gameName = scanner.nextLine();

            if (gameName.equalsIgnoreCase("exit")) 
            {
                break;
            }

            // Syndeomaste ston master gia na kanoume tin erotisi
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) 
                {

                // 1. Stelnoume to onoma tou paixnidiou (String)
                out.writeObject(gameName);
                out.flush();

                // 2. Perimenoume tin apantisi(Game)
                Object response = in.readObject();

                if (response instanceof Game) 
                {
                    Game foundGame = (Game) response;
                    System.out.println("FOUND! Game information:");
                    System.out.println("   - Name: " + foundGame.getGameName());
                    System.out.println("   - Provider: " + foundGame.getProviderName()); 
                    System.out.println("   - Jackpot: " + foundGame.getJackpot());       
                } 
                else if (response instanceof String)
                {
                    System.out.println("[X] " + response);
                }

            } 
            catch (Exception e) 
            {
                System.err.println("Failed to communicate with master: " + e.getMessage());
            }
        }
        scanner.close();
    }
}