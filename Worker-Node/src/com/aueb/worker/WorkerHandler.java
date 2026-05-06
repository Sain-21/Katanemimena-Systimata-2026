package com.aueb.worker;

import com.aueb.shared.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class WorkerHandler implements Runnable 
{
    private Socket socket;
    private HashMap<String, Game> gameList;

    public WorkerHandler(Socket socket, HashMap<String, Game> gameList) 
    {
        this.socket = socket;
        this.gameList = gameList;
    }

    @Override
    public void run() 
    {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) 
             {
            
            Object received = ois.readObject();

            // add game
            if (received instanceof Game) 
            {
                Game newGame = (Game) received;
                synchronized (gameList) 
                {
                    if(gameList.containsKey(newGame.getGameName()))
                    {
                        Game existing = gameList.get(newGame.getGameName());
                        existing.setMinBet(newGame.getMinBet());
                        existing.setMaxBet(newGame.getMaxBet());
                        existing.setRiskLevel(newGame.getRiskLevel());
                        existing.setProviderName(newGame.getProviderName());
                        System.out.println("[WORKER] Updated existing game properties:" + newGame.getGameName());
                    }
                    else
                    {
                        gameList.put(newGame.getGameName(), newGame);
                        System.out.println("[WORKER] Added new game to map: " + newGame.getGameName());

                        synchronized(WorkerServer.providerProfits)
                        {
                            if (!WorkerServer.providerProfits.containsKey(newGame.getProviderName()))
                            {
                                WorkerServer.providerProfits.put(newGame.getProviderName(), 0.0);
                            }
                        }
                    }
                }
                oos.writeObject("Game Processed Successfully");
            } 
            
            // remove game
            else if (received instanceof RemoveGameRequest) 
            {
                RemoveGameRequest req = (RemoveGameRequest) received;
                synchronized (gameList) 
                {
                    Game removedGame = gameList.remove(req.getGameName());

                    if(removedGame != null)
                    {
                        String provider = removedGame.getProviderName();

                        boolean providerExists = false;
                        for(Game g : gameList.values())
                        {
                            if(g.getProviderName() != null && g.getProviderName().equalsIgnoreCase(provider))
                            {
                                providerExists = true;
                                break;
                            }
                        }

                        oos.writeObject("Game Removed!");
                    }
                    else
                    {
                        oos.writeObject("Game not found!");
                    }
                }
            }

            //search
            else if (received instanceof SearchRequest) 
            {
                SearchRequest req = (SearchRequest) received;
                List<Game> matches;
                
                synchronized(gameList)
                {
                    matches = map(req, gameList.values());
                }
                oos.writeObject(matches);
                oos.flush();
            }

            // game list
            else if (received instanceof ListGamesRequest) 
            {
                synchronized (gameList) 
                {
                    oos.writeObject(new ArrayList<>(gameList.values()));
                }
            }

            //play game
            else if (received instanceof PlayRequest) 
            {
                handlePlayRequest((PlayRequest) received, oos);
            }

            // player statistics
            else if (received instanceof String && received.equals("GET_PLAYER_STATS")) 
            {
                synchronized (WorkerServer.playerProfits) 
                {
                    oos.writeObject(new HashMap<>(WorkerServer.playerProfits));
                }
            }

            else if (received instanceof RateRequest) 
            {
                RateRequest rr = (RateRequest) received;
                String responseMessage = "Error: Game not found.";
                synchronized(gameList) 
                {
                    Game g = gameList.get(rr.getGameName());
                    if (g != null) 
                    {
                        boolean success = g.addRating(rr.getPlayerName(), rr.getStars());
                        if (success)
                        {
                            responseMessage = "Rating submitted successfully! New average: " + String.format("%.1f", g.getStars());
                        }
                        else
                        {
                            responseMessage = "You have already rated this game!";
                        }
                    }
                }
                oos.writeObject(responseMessage);
                oos.flush();
            }
            else if(received instanceof String && received.equals("GET_PROVIDER_STATS"))
            {
                synchronized(WorkerServer.providerProfits)
                {
                    oos.writeObject(new HashMap<>(WorkerServer.providerProfits));
                }
            }
            oos.flush();
        } 
        catch (Exception e) 
        {
            System.err.println("[WORKER ERROR] " + e.getMessage());
        }
    }

    private void handlePlayRequest(PlayRequest playReq, ObjectOutputStream oos) throws Exception 
    {
        double amount = playReq.getBetAmount();
        Game game;
        
        synchronized (gameList) 
        {
            game = gameList.get(playReq.getGameName());
        }

        if (game == null) 
        {
            oos.writeObject("Game not found!");
            return;
        }

        if (amount < game.getMinBet() || amount > game.getMaxBet()) 
        {
            oos.writeObject("Invalid bet! Range: " + game.getMinBet() + " - " + game.getMaxBet());
            return;
        }

        String[] srgData = null;
        try (Socket srgSocket = new Socket("localhost", 6000);
            ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
            ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream()))
        {
            //stelneis mono to onoma tou game
            srgOut.writeObject(game.getGameName());
            srgOut.flush();

            //perneis piso [arithmos , hash]
            srgData = (String[])srgIn.readObject();
        }
        catch(Exception e)
        {
            System.err.println("[WORKER] SRG Error: " + e.getMessage());
            oos.writeObject("[ERROR] : Random Generator is offline.");
            return;
        }

        if(srgData == null) return;

        int randomNumber = Integer.parseInt(srgData[0]);
        String receivedHash = srgData[1];

        if(!verifyHash(randomNumber , receivedHash , game.getHashKey()))
        {
            oos.writeObject("[ERROR] : Manipulated number detected!");
            return;
        }

        double payout = 0;

        if (randomNumber % 100 == 0) 
        {
            payout = amount * game.getJackpot();
        } 
        else
        {
            double multiplier = game.getMultiplier(randomNumber % 10);
            payout = amount * multiplier;
        }

        synchronized (game) 
        {
            game.addPlay(amount, payout);
        }

        double playerNetResult = payout - amount;

        // update local worker map
        synchronized (WorkerServer.playerProfits) 
        {
            String player = playReq.getPlayerName();
            double old = WorkerServer.playerProfits.getOrDefault(player , 0.0);
            WorkerServer.playerProfits.put(player , old + playerNetResult);
        }
        
        double providerNetResult = amount - payout;
        synchronized(WorkerServer.providerProfits)
        {
            WorkerServer.providerProfits.merge(game.getProviderName(), providerNetResult, Double::sum);
        }

        System.out.println("[PLAY] : Player: " + playReq.getPlayerName() + " | Game: " + game.getGameName() + " | Profit: " + playerNetResult);
        oos.writeObject(Double.valueOf(payout));
        oos.flush();
    }

    private boolean verifyHash(int num, String receivedHash , String secret) throws Exception 
    {
        //if(secret == null)
        //{
        //    secret = "LaloFroutaSecret";
        //}

        String checkStr = num + secret;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(checkStr.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString().equals(receivedHash);
    }

    private List<Game> map(SearchRequest req, Collection<Game> localGames) 
    {
        List<Game> intermediateResults = new ArrayList<>();
        for (Game g : localGames) 
        {
            if (req.getGameName() != null) 
            {
                if (g.getGameName().equalsIgnoreCase(req.getGameName()))
                {
                    intermediateResults.add(g);
                    break;
                }
            } 
            else
            {
                boolean starsMatch;
                if (req.getMinStars() == -1.0)
                {
                    starsMatch = true;
                }
                else if (req.getMinStars() == 0.0)
                {
                    starsMatch = (g.getStars() == 0.0);
                }
                else
                {
                    starsMatch = (g.getStars() >= req.getMinStars());
                }
                boolean riskMatch = req.getRiskLevel().trim().equalsIgnoreCase("ALL") || g.getRiskLevel().trim().equalsIgnoreCase(req.getRiskLevel().trim());  
                boolean betMatch = req.getBetLimit().trim().equalsIgnoreCase("ALL") || g.getBetCategory().trim().equalsIgnoreCase(req.getBetLimit().trim());

                if (starsMatch && riskMatch && betMatch) 
                {
                    intermediateResults.add(g);
                }
            }
        }
        return intermediateResults;
    }
}