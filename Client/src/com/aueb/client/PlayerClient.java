package com.aueb.client;

import com.aueb.RateRequest;
import com.aueb.shared.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class PlayerClient 
{
    private static final String HOST = "localhost";
    private static final int PORT = 1312;
    private static Scanner scanner = new Scanner(System.in);
    private static String username;

    public static void main(String[] args) 
    {
        System.out.println("=== Welcome to CasinoLalo ===");
        System.out.print("Enter your Name: ");
        username = scanner.nextLine();

        while (true) 
        {
            System.out.println("\n--- PLAYER MENU ---");
            System.out.println("1. View ALL Games");
            System.out.println("2. Search & Play by Name");
            System.out.println("3. Filter Games (Stars, Bet, Risk)");
            System.out.println("4. Exit");
            System.out.print("Choice: ");
            
            String choice = scanner.nextLine();
            if (choice.equals("4")) break;

            switch (choice) 
            {
                case "1":
                    displayGames(fetchAllGames());
                    break;
                case "2":
                    searchAndPlay();
                    break;
                case "3":
                    filterMenu();
                    break;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static List<Game> fetchAllGames() 
    {
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            
            out.writeObject(new ListGamesRequest());
            out.flush();
            return (List<Game>) in.readObject();
        } 
        catch (Exception e) 
        {
            System.err.println("Error fetching games: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void displayGames(List<Game> games) 
    {
        if (games.isEmpty()) 
        {
            System.out.println("No games available.");
            return;
        }
        System.out.println("\n%-20s | %-10s | %-5s | %-10s | %-5s".formatted("Name", "Provider", "Stars", "Bet Limit", "Risk"));
        System.out.println("----------------------------------------------------------------------");
        for (Game g : games) 
        {
            System.out.println("%-20s | %-10s | %-5.1f | %-10s | %-5s".formatted(g.getGameName(), g.getProviderName(), g.getStars(), g.getBetCategory(), g.getRiskLevel()));
        }
        
        System.out.print("\nEnter game name to PLAY or 'back': ");
        String name = scanner.nextLine();
        if (!name.equalsIgnoreCase("back")) playAction(name);
    }

    private static void filterMenu() 
    {
        System.out.println("\nFilter by:");
        System.out.println("1. Stars (e.g. 4+)");
        System.out.println("2. Bet Category ($, $$, $$$)");
        System.out.println("3. Risk Level (Low, Medium, High)");
        System.out.print("Choice: ");
        String fChoice = scanner.nextLine();
        
        List<Game> all = fetchAllGames();
        List<Game> filtered = new ArrayList<>();

        if (fChoice.equals("1")) 
        {
            System.out.print("Enter min stars (1-5): ");
            double minStars = Double.parseDouble(scanner.nextLine());
            for(Game g : all) if(g.getStars() >= minStars) filtered.add(g);
        } 

        else if (fChoice.equals("2")) 
        {
            System.out.print("Enter category ($, $$, $$$): ");
            String cat = scanner.nextLine();
            for(Game g : all) if(g.getBetCategory().equals(cat)) filtered.add(g);
        } 

        else if (fChoice.equals("3")) 
        {
            System.out.print("Enter risk (Low, Medium, High): ");
            String risk = scanner.nextLine();
            for(Game g : all) if(g.getRiskLevel().equalsIgnoreCase(risk)) filtered.add(g);
        }
        
        displayGames(filtered);
    }

    private static void searchAndPlay() {
        System.out.print("Enter game name: ");
        String name = scanner.nextLine();
        playAction(name);
    }

    private static void playAction(String gameName) 
    {
        Game found = null;
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            
            out.writeObject(new SearchRequest(gameName));
            out.flush();
            Object resp = in.readObject();
            if (resp instanceof Game) found = (Game) resp;
        } 
        catch (Exception e) 
        { 
            return;
        }

        if (found == null) 
        {
            System.out.println("Game not found!");
            return;
        }

        System.out.print("Bet amount (" + found.getMinBet() + "-" + found.getMaxBet() + "): ");
        double amount = Double.parseDouble(scanner.nextLine());

        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            
            out.writeObject(new PlayRequest(username, gameName, amount));
            out.flush();
            System.out.println("RESULT: " + in.readObject());

            System.out.print("Rate this game (1-5 stars) or press Enter to skip: ");
            String rate = scanner.nextLine();
            if (!rate.isEmpty()) 
            {
                sendRating(gameName, Integer.parseInt(rate));
            }
        } 
        catch (Exception e) 
        { 
            e.printStackTrace(); 
        }
    }

    private static void sendRating(String gameName, int stars) 
    {
        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) 
             {
            out.writeObject(new RateRequest(gameName, stars));
            out.flush();
            System.out.println("Thank you for rating!");
        } 
        catch (Exception e) 
        {
            System.out.println("Rating failed.");
        }
    }
}