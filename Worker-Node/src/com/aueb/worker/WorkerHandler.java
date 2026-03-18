package com.aueb.worker;

import com.aueb.shared.Game;
import com.aueb.shared.RemoveGameRequest;
import com.aueb.shared.SearchRequest;

import java.io.*;

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
            socket.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
