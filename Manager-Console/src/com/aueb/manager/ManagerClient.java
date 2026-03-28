package com.aueb.manager;

import com.aueb.shared.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ManagerClient 
{
    private static final String FILE_PATH = "Resources\\game.json";
    private static final String HOST = "localhost";
    private static final int PORT = 1312;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) 
    {
        //main screen
        while (true) 
        {
            System.out.println("\n================ Manager Console ================");
            System.out.println("1. Add games from JSON file");
            System.out.println("2. Add NEW game manually");
            System.out.println("3. Remove a game");
            System.out.println("4. Display Profit/Loss per GAME");
            System.out.println("5. Display Profit/Loss per PLAYER");
            System.out.println("6. Display Profit/Loss per PROVIDER");
            System.out.println("7. Edit game");
            System.out.println("8. Exit");
            System.out.print("Choice: ");

            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;
            int choice = Integer.parseInt(input);

            if (choice == 1) addFromJson();
            else if (choice == 2) addManually();
            else if (choice == 3) removeGame();
            else if (choice == 4) showGameStats();
            else if (choice == 5) showPlayerStats();
            else if (choice == 6) showProviderStats();
            else if (choice == 7) editLocalJson();
            else if (choice == 8) break;
        }
    }

    // add games from json
    private static void addFromJson() 
    {
        List<Game> games = readGamesFromJson(FILE_PATH);
        System.out.println("[INFO] : Sending " + games.size() + " games to Master...");
        for (Game g : games) 
        {
            sendGameToMaster(g);
        }
    }

    // manual game add
    private static void addManually() 
    {
        System.out.println("\n--- Create New Game ---");
        String name = promptString("Game Name", "");
        if(name.trim().isEmpty())
        {
            System.out.println("Error: Game name cannot be empty!");
            return;
        }
        String provider = promptString("Provider", "");
        int stars = promptInt("Stars (0-5)", 0);
        if(stars < 0 || stars > 5)
        {
            System.out.println("Error: Stars must be between 0 and 5!");
            return;
        }
        double minBet = promptDouble("Min Bet", 0.1);
        double maxBet = promptDouble("Max Bet", 100.0);
        if(minBet < 0.1 || maxBet <= minBet)
        {
            System.out.println("Error: Invalid bet range! (Min must be >= 0.1 and Max > Min)");
            return;
        }
        String risk = promptString("Risk Level (low/medium/high)", "low").toLowerCase();
        if(!Arrays.asList("low" , "medium" , "high").contains(risk))
        {
            System.out.println("Error: Risk level must be low, medium, or high!");
            return;
        }
        Game newGame = new Game(name, provider, stars, 0, null, minBet, maxBet, risk, null);
        sendGameToMaster(newGame);
        try
        {
            Thread.sleep(500); // Pause for 1 second
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    // delete game
    private static void removeGame() 
    {
        System.out.print("Name of game to remove: ");
        String name = sc.nextLine();
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            out.writeObject(new RemoveGameRequest(name));
            System.out.println("[SERVER]: " + in.readObject());
        } 
        catch (Exception e) 
        { 
            e.printStackTrace();
        }
    }

    // statistics per game
    private static void showGameStats() 
    {
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            out.writeObject(new ListGamesRequest());
            List<Game> games = (List<Game>) in.readObject();

            System.out.println("\n--- PROFIT/LOSS PER GAME ---");
            System.out.printf("%-20s | %-12s | %-12s | %-12s\n", "Game Name", "Total Bets", "Total Payout", "Profit");
            for (Game g : games) 
            {
                System.out.printf("%-20s | %-12.2f | %-12.2f | %-12.2f\n", 
                g.getGameName(), g.getTotalBets(), g.getTotalPayouts(), g.getProfit());
            }
        } 
        catch (Exception e) 
        { 
            e.printStackTrace(); 
        }
    }

    // statistics per player
    private static void showPlayerStats() 
    {
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            
            out.writeObject("GET_PLAYER_STATS");
            out.flush();
            
            Object response = in.readObject();
            if (response instanceof Map) 
            {
                Map<String, Double> stats = (Map<String, Double>) response;
                System.out.println("\n========= PLAYER STATS (GLOBAL) =========");
                System.out.printf("%-20s | %-15s | %-10s\n", "Player Name", "Net Profit/Loss", "Status");
                System.out.println("---------------------------------------------------------");
                
                for (Map.Entry<String, Double> entry : stats.entrySet()) 
                {
                    double val = entry.getValue();
                    String status = (val >= 0) ? "WINNING" : "LOSING";
                    System.out.printf("%-20s | %-15.2f | %-10s\n", entry.getKey(), val, status);
                }
            }
        } 
        catch (Exception e) 
        {
            System.out.println("[ERROR] Failed to fetch player stats: " + e.getMessage());
        }
    }

    // edit game
    private static void editLocalJson() 
    {
        List<Game> games = readGamesFromJson(FILE_PATH);
        String name = promptString("Game name to edit", "");
        for (Game g : games) 
        {
            if (g.getGameName().equalsIgnoreCase(name)) 
            {
                g.setGameName(promptString("New Name", g.getGameName()));
                g.setMinBet(promptDouble("New Min Bet", g.getMinBet()));
                g.setMaxBet(promptDouble("New Max Bet", g.getMaxBet()));
                g.setRiskLevel(promptString("New Risk", g.getRiskLevel()));
                writeGamesToJson(FILE_PATH, games);
                sendGameToMaster(g);
                System.out.println("[OK] Local JSON updated.");
                return;
            }
        }
        System.out.println("Game not found.");
    }

    private static void showProviderStats()
    {
        try (Socket s = new Socket(HOST, PORT);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(s.getInputStream()))
        {
            out.writeObject("GET_PROVIDER_STATS");
            Object response = in.readObject();

            if (response instanceof Map)
            {
                Map<String, Double> stats = (Map<String, Double>) response;
                System.out.println("\n--- PROFITS PER PROVIDER ---");
                stats.forEach((k, v) -> System.out.printf("Provider: %-15s | Profit: %.2f\n", k, v));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // helpers
    private static void sendGameToMaster(Game g) 
    {
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            out.writeObject(g);
            out.flush();
            
            //wait for acceptance
            Object response = in.readObject(); 
            System.out.println("[SUCCESS] Master says: " + response);
            
        } 
        catch (Exception e) 
        { 
            System.err.println("Failed to send " + g.getGameName() + ": " + e.getMessage()); 
        }
    }

    private static String promptString(String label, String current) 
    {
        System.out.print(label + (current.isEmpty() ? "" : " [" + current + "]") + ": ");
        String input = sc.nextLine().trim();
        return input.isEmpty() ? current : input;
    }

    private static int promptInt(String label, int current) 
    {
        String res = promptString(label, String.valueOf(current));
        return Integer.parseInt(res);
    }

    private static double promptDouble(String label, double current) 
    {
        String res = promptString(label, String.valueOf(current));
        return Double.parseDouble(res);
    }

    private static List<Game> readGamesFromJson(String path) 
    {
        try 
        {
            return new ObjectMapper().readValue(new File(path), new TypeReference<List<Game>>() {});
        } 
        catch (Exception e) 
        { 
            return new ArrayList<>(); 
        }
    }

    private static void writeGamesToJson(String path, List<Game> games) 
    {
        try 
        {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(path), games);
        } 
        catch (Exception e) 
        { 
            e.printStackTrace(); 
        }
    }
}