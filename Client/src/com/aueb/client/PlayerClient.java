package com.aueb.client;

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
    private static double balance = 0.0; // tokens

    public static void main(String[] args) 
    {
        System.out.println("=== Welcome to CasinoLalo ===");
        System.out.print("Enter your Name: ");
        username = scanner.nextLine();

        while (true) 
        {
            System.out.println("\n--- PLAYER MENU ---");
            System.out.println("Balance: " + balance);
            System.out.println("1. View ALL Games");
            System.out.println("2. Search & Play by Name");
            System.out.println("3. Filter Games (Stars, Bet, Risk)");
            System.out.println("4. Add tokens");
            System.out.println("5. Exit");
            System.out.print("Choice: ");
            
            String choice = scanner.nextLine();
            if (choice.equals("5")) break;

            switch (choice) 
            {
                case "1":
                    displayGames(fetchAllGames());
                    break;
                case "2":
                    searchAndPlay();
                    break;
                case "3":
                    searchWithFilters();
                    break;
                
                case "4":
                    addBalance();
                    break;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void addBalance() 
    {
        System.out.print("Posa Tokens Thelete na Prosthesete; ");
        double amount = Double.parseDouble(scanner.nextLine());
        balance += amount;
        System.out.println("Epitixis Katathesi! Neo Balance: " + balance);
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

    private static void searchWithFilters() 
    {
        System.out.print("Min Stars (1-5): ");
        double stars = Double.parseDouble(scanner.nextLine());
        System.out.print("Risk (low, medium, high): ");
        String risk = scanner.nextLine();
        System.out.print("Bet Limit ($, $$, $$$): ");
        String limit = scanner.nextLine();

        SearchRequest req = new SearchRequest(username, stars, risk, limit);

        // Xrisi thread gia asugxroni ektelesi oste h efarmogi na einai diadrastiki 
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(req);
                out.flush();

                // Lipsi listas apotelesmaton (MapReduce Result) 
                Object resp = in.readObject();
                if (resp instanceof List) {
                    List<Game> results = (List<Game>) resp;
                    System.out.println("\n--- Search Results (" + results.size() + " found) ---");
                    for (Game g : results) {
                        System.out.println("-> " + g.getGameName() + " (Stars: " + g.getStars() + ")");
                    }
                }
            } catch (Exception e) {
                System.err.println("Async search error: " + e.getMessage());
            }
    }).start();
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
        if (amount < found.getMinBet() || amount > found.getMaxBet())
        {
            System.out.println("Bet must me around: " + found.getMinBet() + " - " + found.getMaxBet());
            return;
        }
        if (amount > balance) 
        {
            System.out.println("Error: Aneparkes Ypolipo (Balance: " + balance + ")");
            return;
        }

        balance -= amount;

        try (Socket s = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) 
             {
            
            
            out.writeObject(new PlayRequest(username, gameName, amount));
            out.flush();

            Object response = in.readObject();

            if (response instanceof Double) {
                double payout = (Double) response;
                
                // 4. Prosthiki tou payout sto balance
                balance += payout; 

                if (payout > amount) {
                    System.out.println("WIN! You won " + payout);
                } else if (payout == 0) {
                    System.out.println("LOST! Better luck next time.");
                } else {
                    System.out.println("Partial return: " + payout);
                }
            }   

            System.out.println("Current Balance: " + balance + " FUN");

            System.out.print("Rate this game (1-5 stars) or press Enter to skip: ");
            String rate = scanner.nextLine();
            if (!rate.isEmpty()) 
            {
                sendRating(gameName, Integer.parseInt(rate));
            }
        } 
        catch (Exception e) 
        { 
            balance += amount;
            e.printStackTrace(); 
        }
    }

    private static void sendRating(String gameName, int stars) 
    {
        try (Socket s = new Socket(HOST, PORT);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream()); 
            ObjectInputStream in = new ObjectInputStream(s.getInputStream()))
        {
            out.writeObject(new RateRequest(gameName, username, stars));
            out.flush();
            
            Object response = in.readObject();
            System.out.println(response);
        } 
        catch (Exception e) 
        {
            System.out.println("Rating failed.");
        }
    }
}