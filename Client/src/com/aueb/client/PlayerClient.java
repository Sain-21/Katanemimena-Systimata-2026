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
    private static double balance = 0.0;

    public static void main(String[] args) 
    {
        System.out.println("=== Welcome to CasinoLalo ===");
        System.out.print("Enter your Name: ");
        username = scanner.nextLine();

        //player menu
        while (true) 
        {
            System.out.println("\n--- PLAYER MENU ---");
            System.out.println("Balance: " + balance);
            System.out.println("1. View ALL Games");
            System.out.println("2. Search & Play by Name");
            System.out.println("3. Filter Games (Stars, Bet, Risk)");
            System.out.println("4. Add balance");
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

    //get all games
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

    //print all games
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
        double stars = 0.0;
        boolean validStars = false;

        while(!validStars)
        {
            System.out.print("Min Stars (1-5): ");
            String starsInput = scanner.nextLine().trim();

            if(starsInput.isEmpty())
            {
                stars = -1.0;
                validStars = true;
            }
            else
            {
                try
                {
                    stars = Double.parseDouble(starsInput);
                    if(stars >= 0.0 && stars <=5)
                    {
                        validStars = true;
                    }
                    else
                    {
                        System.out.println("Invalid Choice! Stars must be from 0-5");
                    }
                }
                catch(NumberFormatException e)
                {
                    System.out.println("Give a number from 0-5 or press Enter");
                }
            }
        }
        
        System.out.print("Risk (low, medium, high): ");
        String risk = scanner.nextLine().trim();
        if(risk.isEmpty())
        {
            risk = "ALL";
        }

        System.out.print("Bet Limit ($, $$, $$$): ");
        String limit = scanner.nextLine().trim();
        if(limit.isEmpty())
        {
            limit = "ALL";
        }

        SearchRequest req = new SearchRequest(username, stars, risk, limit);

        try (Socket socket = new Socket(HOST, PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(req);
            out.flush();

            //lipsi listas apotelesmatwn
            Object resp = in.readObject();
            if (resp instanceof List) 
            {
                List<Game> results = (List<Game>) resp;
                System.out.println("\n--- Search Results (" + results.size() + " found) ---");

                for (Game g : results) 
                {
                        System.out.println("-> " + g.getGameName() + " (Stars: " + g.getStars() + ")");
                }
            }
        } 
        catch (Exception e) 
        {
            System.err.println("Async search error: " + e.getMessage());
        }
    }

    private static void searchAndPlay() 
    {
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

        double amount = 0.0;
        while (true) 
        {
            System.out.print("Bet amount (" + found.getMinBet() + "-" + found.getMaxBet() + "): ");
            String betInput = scanner.nextLine().trim();

            if (betInput.isEmpty()) 
            {
                System.out.println("Invalid bet! You must enter a number.");
                continue; 
            }

            try 
            {
                amount = Double.parseDouble(betInput);
                break;
            } 
            catch (NumberFormatException e) 
            {
                System.out.println("Invalid input! Please enter a valid number.");
            }
        }

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
        
            out.writeObject(new PlayRequest(username, found.getGameName(), amount));
            out.flush();

            Object response = in.readObject();

            if (response instanceof Double) 
            {
                double payout = (Double) response;
            
                balance += payout; 

                if (payout > amount) 
                {
                    System.out.println("WIN! You won " + payout);
                } 
                else if (payout == 0) 
                {
                    System.out.println("LOST! Better luck next time.");
                } 
                else 
                {
                    System.out.println("Partial return: " + payout);
                }
            } 
            else if (response instanceof String) 
            {
                System.out.println("[SERVER] " + response);
                balance += amount;
            }  

            System.out.println("Current Balance: " + balance + " FUN");

            System.out.print("Rate this game (1-5 stars) or press Enter to skip: ");
            String rate = scanner.nextLine().trim();
            if (!rate.isEmpty()) 
            {
                try 
                {
                    sendRating(found.getGameName(), Integer.parseInt(rate));
                } 
                catch (NumberFormatException e) 
                {
                    System.out.println("Invalid rating number. Skipped.");
                }
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