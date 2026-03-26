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
                Game game = (Game) received;
                synchronized (gameList) 
                {
                    gameList.put(game.getGameName(), game);
                }
                System.out.println("[WORKER] Saved/Updated game: " + game.getGameName());
                oos.writeObject("Game Processed Successfully");
            } 
            
            // remove game
            else if (received instanceof RemoveGameRequest) 
            {
                RemoveGameRequest req = (RemoveGameRequest) received;
                synchronized (gameList) {
                    if (gameList.remove(req.getGameName()) != null) 
                    {
                        oos.writeObject("Game Removed!");
                    } 
                    else 
                    {
                        oos.writeObject("Game not found!");
                    }
                }
            }

            //search
            // Mesa ston WorkerHandler
            else if (received instanceof SearchRequest) {
                SearchRequest req = (SearchRequest) received;
                List<Game> matches = new ArrayList<>();
            
                synchronized(gameList) {
                    for (Game g : gameList.values()) {
                        // An anazitame sygkekrimeno onoma (playAction)
                        if (req.getGameName() != null) {
                            if (g.getGameName().equalsIgnoreCase(req.getGameName())) {
                                matches.add(g);
                                break;
                            }
                        } 
                        // alliws filtrarisma me mapreduce kritiria [cite: 69, 71]
                        else if (g.getStars() >= req.getMinStars() &&
                                 g.getRiskLevel().equalsIgnoreCase(req.getRiskLevel()) &&
                                 g.getBetCategory().equals(req.getBetLimit())) {
                            matches.add(g);
                        }
                    }
                }
                oos.writeObject(matches);
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
                synchronized(gameList) 
                {
                    Game g = gameList.get(rr.getGameName());
                    if (g != null) g.addRating(rr.getStars());
                }
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

        //get number from buffer(srg)
        String[] srgData;
        synchronized (WorkerServer.lock) 
        {
            while (WorkerServer.numberBuffer.isEmpty()) 
            {
                System.out.println("[WORKER] : Waiting for SRG numbers...");
                WorkerServer.lock.wait();
            }
            srgData = WorkerServer.numberBuffer.remove(0);
            WorkerServer.lock.notifyAll();
        }

        int randomNumber = Integer.parseInt(srgData[0]);
        String receivedHash = srgData[1];

        //hash verify
        if (!verifyHash(randomNumber, receivedHash , game.getHashKey())) 
        {
            oos.writeObject("[ERROR] : Manipulated number detected!");
            return;
        }

        double payout = 0;
        String message;

        if (randomNumber % 100 == 0) 
        {
            payout = amount * game.getJackpot();
            message = "JACKPOT!!! You won: " + payout;
        } 
        else
        {
            double multiplier = game.getMultiplier(randomNumber % 10);
            payout = amount * multiplier;
            message = (payout > 0) ? "Win! You won: " + payout : "Lost! Number was: " + randomNumber;
        }

        synchronized (game) 
        {
            game.addPlay(amount, payout);
        }

        double playerNetResult = payout - amount;
        playerNetResult = Math.round(playerNetResult * 100.0) / 100.0;


            // update local worker map
        synchronized (WorkerServer.playerProfits) 
        {
            WorkerServer.playerProfits.merge(playReq.getPlayerName(), playerNetResult, Double::sum);
        }
        
        double providerNetResult = amount - payout;
        synchronized(WorkerServer.providerProfits)
        {
            WorkerServer.providerProfits.merge(game.getProviderName(), providerNetResult, Double::sum);
        }

        System.out.println("[PLAY] : Player: " + playReq.getPlayerName() + " | Game: " + game.getGameName() + " | Profit: " + playerNetResult);
        //oos.writeObject(message);
        oos.writeObject(Double.valueOf(payout));
        oos.flush();
    }

    private boolean verifyHash(int num, String receivedHash , String secret) throws Exception 
    {
        if(secret == null)
        {
            secret = "defaultSecret";
        }
        String checkStr = num + secret;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(checkStr.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString().equals(receivedHash);
    }
}