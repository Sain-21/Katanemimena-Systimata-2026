package com.aueb.worker;

import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;
import com.aueb.shared.PlayRequest;
import com.aueb.shared.RemoveGameRequest;
import com.aueb.shared.SearchRequest;

import java.io.*;
import java.util.ArrayList;
import java.net.Socket;
import java.util.HashMap;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class WorkerHandler implements Runnable
{
    private Socket socket;
    private HashMap<String , Game> gameList;

    public WorkerHandler(Socket socket , HashMap<String , Game> gameList)
    {
        this.socket = socket;
        this.gameList = gameList;
    }

    @Override
    public void run()
    {
        try(ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream()))
        {
            Object received = ois.readObject();

            if(received instanceof Game)
            {
                Game game = (Game) received;
                synchronized(gameList)
                {
                    gameList.put(game.getGameName() , game);
                }
                System.out.println("[WORKER] Saved game: " + game.getGameName());
                oos.writeObject("Game Added Successfully");
                oos.flush();
            }   
            else if(received instanceof RemoveGameRequest)
            {
                RemoveGameRequest req = (RemoveGameRequest) received;
                String gameName = req.getGameName();   
                synchronized(gameList)
                {
                    if(gameList.remove(gameName) != null)
                    {
                       oos.writeObject("Game Removed!");
                    }
                    else
                    {
                        oos.writeObject("Game not found!");
                    }
                }
                oos.flush();
            }
            else if(received instanceof SearchRequest)
            {
                SearchRequest req = (SearchRequest) received;
                String requestedGame = req.getGameName();

                synchronized(gameList)
                {
                    if(gameList.containsKey(requestedGame))
                    {
                        oos.writeObject(gameList.get(requestedGame));
                    }
                    else
                    {
                        oos.writeObject(null); // gia reduce
                    }
                }
                oos.flush();
            }
            else if(received instanceof ListGamesRequest)
            {
                synchronized(gameList)
                {
                    oos.writeObject(new ArrayList<>(gameList.values()));
                }
                oos.flush();
            }
            else if (received instanceof PlayRequest)
            {
                PlayRequest playReq = (PlayRequest) received;
                double amount = playReq.getBetAmount();
                Game game;
                synchronized(gameList)
                {
                    game = gameList.get(playReq.getGameName());
                }

                if(game == null)
                {
                    oos.writeObject("Game not found!");
                }
                else if(amount < game.getMinBet() || amount > game.getMaxBet())
                {
                    oos.writeObject("Invalid!!! Allowed range: " + game.getMinBet() + " - " + game.getMaxBet());
                }
                else
                {
                    try
                    {
                        String[] srgData;
                        synchronized(WorkerServer.lock)
                        {
                            while (WorkerServer.numberBuffer.isEmpty())
                            {
                                System.out.println("[WORKER] Waiting for numbers....");
                                WorkerServer.lock.wait();
                            }
                            srgData = WorkerServer.numberBuffer.remove(0);
                            WorkerServer.lock.notifyAll();
                        }

                        int randomNumber = Integer.parseInt(srgData[0]);
                        String receivedHash = srgData[1];

                        String secret = "LaloFroutaSecret"; 
                        String checkStr = randomNumber + secret ; 

                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hashBytes = digest.digest(checkStr.getBytes(StandardCharsets.UTF_8));

                        StringBuilder sb = new StringBuilder();
                        for (byte b : hashBytes) {
                            sb.append(String.format("%02x", b));
                        }
                        String calculatedHash = sb.toString();          

                        //elegxos an to hash pou steilame einai idio me auto p esteile o srg
                        if (!calculatedHash.equals(receivedHash)) 
                        {
                            oos.writeObject("Error: Notheumenos Arithmos");
                        } 
                        else 
                        {
                            double payout = 0;
                            if(randomNumber % 100 == 0)
                            {
                                payout = amount * game.getJackpot();
                                oos.writeObject("JACKPOT!!! Kerdises: " + payout);
                            }
                            else
                            {
                                double multiplier = game.getMultiplier(randomNumber % 10);
                                payout = amount * multiplier;
                                if (payout > 0)
                                {
                                    oos.writeObject("Niki! Kerdises: " + payout);
                                }
                                else
                                {
                                    oos.writeObject("Xasate! O arithmos itan: " + randomNumber);
                                }
                            }
                            game.addPlay(amount, payout);
                            String player = playReq.getPlayerName();
                            double profit = amount - payout;

                            synchronized(WorkerServer.playerProfits)
                            {
                                double oldProf = EorkerServer.playerProfits.getOrDefault(player , 0.0);
                                double newProfit = oldProfit + profit;
                                WorkerServer.playerProfits.put(player , newProfit);
                            }
                            System.out.println("---------------------------------------");
                            System.out.println("[STAT - UPDATE] Game: " + game.getGameName());
                            System.out.println("PLAYER PROFITS: " + WorkerServer.playerProfits);
                            System.out.println("Bet: " + amount + " | Payout: " + payout);
                            System.out.println("Total Bets: " + game.getTotalBets());
                            System.out.println("Total Payouts: " + game.getTotalPayouts());
                            System.out.println("Current Profit: " + game.getProfit());
                            System.out.println("---------------------------------------");
                        }
                    }
                    catch(Exception e)
                    {
                        oos.writeObject("Sfalma kata to pontarisma");
                    }
                }
                oos.flush();
            }
            socket.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
