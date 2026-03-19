package com.aueb.client;

import com.aueb.shared.Game;
import com.aueb.shared.SearchRequest;
import com.aueb.shared.PlayRequest;

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
        
        System.out.print("Enter your Name: ");
        String username = scanner.nextLine();

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
                out.writeObject(new SearchRequest(gameName));
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
                    
                    System.out.print("\nThelete na pontarete se auto to paixnidi? (nai/oxi): ");
                    String answer = scanner.nextLine();

                    if(answer.equalsIgnoreCase("nai"))
                    {
                        System.out.print("Posa xrhmata thelete na pontarete? ");
                        double bet = Double.parseDouble(scanner.nextLine());

                        try(Socket playSocket = new Socket(host , port);
                            ObjectOutputStream oos = new ObjectOutputStream(playSocket.getOutputStream());
                            ObjectInputStream ois = new ObjectInputStream(playSocket.getInputStream()))
                        {
                            oos.writeObject(new com.aueb.shared.PlayRequest(username , foundGame.getGameName(), bet));
                            oos.flush();

                            Object playResponse = ois.readObject();
                            System.out.println("=== Apotelesma === " + playResponse);
                        }
                        catch (Exception e)
                        {
                            System.err.println("Error sto pontarisma: " + e.getMessage());
                        }
                    }
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