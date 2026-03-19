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
                String gName = playReq.getGameName();
                double amount = playReq.getBetAmount();

                Game game;
                synchronized(gameList)
                {
                    game = gameList.get(gName);
                }

                if(game == null)
                {
                    oos.writeObject("To paixnidi den brethike");
                }
                else
                {
                    try
                    {
                        Socket srg = new Socket("localhost" , 6000);
                        ObjectInputStream srgIn = new ObjectInputStream(srg.getInputStream());
                        String[] srgData = (String[]) srgIn.readObject();
                        srg.close();

                        int randomNumber = Integer.parseInt(srgData[0]);
                        String receivedHash = srgData[1];

                        String secret = "LaloFroutaSecret";
                        String checkStr = randomNumber + secret;
                        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                        byte[] hashBytes = digest.digest(checkStr.getBytes("UTF-8"));

                        //Metatropi se Hex gia sygkrisi
                        String calculatedHash = "";
                        for (byte b : hashBytes) 
                        {
                            String hex = Integer.toHexString(0xff & b);
                            if (hex.length() == 1) calculatedHash += "0";
                            calculatedHash += hex;
                        }
                        if(!calculatedHash.equals(receivedHash))
                        {
                            oos.writeObject("Sfalma: Notheumenos Arithmos!");
                        }
                        else
                        {
                            //Ypologismos kerdous
                            double payout = 0;
                            
                            if(randomNumber % 100 == 0)
                            {
                                payout = amount * game.getJackpot();
                                oos.writeObject("Jackpot!!! Kerdises: " + payout);
                            }
                            else if (randomNumber % 10 == 0)
                            {
                                //thelei risk level (bazw x2 gia aplotita)
                                payout = amount * 2;
                                oos.writeObject("Niki!!! Kerdises: " + payout);
                            }
                            else
                            {
                                payout = 0;
                                oos.writeObject("Xasate! O tyxaios arithmos itan: " + randomNumber);
                            }
                            //enimerosi statistikon
                            game.addPlay(amount, payout);
                        }
                    }
                    catch(Exception e)
                    {
                        oos.writeObject("Sfalma kata to pontarisma");
                        e.printStackTrace();
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
