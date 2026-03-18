package com.aueb.manager;

import com.aueb.shared.Game;
import com.aueb.shared.RemoveGameRequest;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

public class ManagerClient 
{
    public static void main(String[] args) 
    {
        String host = "localhost";
        int port = 1312; //port
        Scanner sc = new Scanner(System.in);
        while(true)
        {
            System.out.println("\n============== Manager Menu ==============");
            System.out.println("1. Add games from JSON");
            System.out.println("2. Remove game");
            System.out.println("3. Exit");

            int choice = sc.nextInt();
            sc.nextLine();

            if(choice == 1)
            {
                // read game.json
                List<Game> gamesToSend = readGamesFromJson("C:\\download\\Katanemimena-Systimata-2026\\Resources\\game.json");
        
                System.out.println("[CLIENT] : Found " + gamesToSend.size() + " games in JSON. Sending...\n");

                // send game to master
                for (Game g : gamesToSend) 
                {
                    try (Socket socket = new Socket("localhost", 1312);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) 
                    {
                
                        System.out.println("[CLIENT] : Sending game: " + g.getGameName());
                
                        out.writeObject(g);
                        out.flush();//send games
                
                        Thread.sleep(200); 
                
                    } catch (Exception e) {
                        System.err.println("Error sending " + g.getGameName() + ": " + e.getMessage());
                    }
                }
            }
            else if(choice == 2)
            {
                System.out.println("Give game name: ");
                String name = sc.nextLine();
                try(Socket socket = new Socket(host , port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
                {
                    out.writeObject(new RemoveGameRequest(name));
                    out.flush();

                    System.out.println("[MANAGER] Response: " + in.readObject());
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            else 
            {
                break;
            }
        }
    }


    //Gemini

    private static List<Game> readGamesFromJson(String filePath) {
        List<Game> games = new ArrayList<>();
        try {
            
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
            
        
            Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(jsonContent);

            while (matcher.find()) {
                String objStr = matcher.group(1);
                
              
                String name = extractValue(objStr, "gameName");
                String provider = extractValue(objStr, "provider"); 
                int stars = Integer.parseInt(extractValue(objStr, "stars")); 
                int votes = Integer.parseInt(extractValue(objStr, "noOfVotes"));
                String logo = extractValue(objStr, "gameLogo");
                double minBet = Double.parseDouble(extractValue(objStr, "minBet"));
                double maxBet = Double.parseDouble(extractValue(objStr, "maxBet"));
                String risk = extractValue(objStr, "riskLevel");
                String hash = extractValue(objStr, "hashKey");

                games.add(new Game(name, provider, stars, votes, logo, minBet, maxBet, risk, hash));
            }
        } 
        catch (Exception e) 
        {
            System.err.println("[ERROR] : Error reading the JSON file: " + e.getMessage());
        }
        return games;
    }

    private static String extractValue(String jsonObject, String key) {

        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(jsonObject);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "0";
    }
}