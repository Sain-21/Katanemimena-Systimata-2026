package com.aueb.worker;

import com.aueb.shared.Game;
import com.aueb.shared.RemoveGameRequest;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerServer 
{
    // save games to worker memory
    private static ConcurrentHashMap<String, Game> gamesList = new ConcurrentHashMap<>();

    public static void main(String[] args) 
    {
        // check gia port (arg)
        if (args.length < 1) 
        {
            System.out.println("[ERROR] : Have to provide port as an argument!");
            return;
        }

        // read arg
        int port = Integer.parseInt(args[0]); 
        System.out.println("[WORKER-" + port + "] : Worker Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[WORKER-" + port + "] : Worker is listening on port " + port);

            while (true) 
            {
                Socket socket = serverSocket.accept();
                
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush(); // Στέλνει το header αμέσως
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object received = in.readObject();

                if (received instanceof Game) 
                {
                    Game game = (Game) received;
                    gamesList.put(game.getGameName(), game);
                    System.out.println("[WORKER-" + port + "] : Received and saved game: " + game.getGameName());
                }
                else if (received instanceof RemoveGameRequest)
                {
                    RemoveGameRequest req = (RemoveGameRequest) received;
                    String gameName = req.getGameName();

                    if(gamesList.remove(gameName) != null)
                    {
                        System.out.println("[WORKER-" + port + "] Removed game: " + gameName);
                        out.writeObject("Game Removed!");
                    }
                    else
                    {
                        out.writeObject("Game not found!");
                    }
                    out.flush();
                }
                //game search
                else if (received instanceof String) 
                {
                    String requestedGame = (String) received;
                    System.out.println("[WORKER-" + port + "] Looking at memory for: " + requestedGame);

                    //psaxnw sto hashmap
                    if (gamesList.containsKey(requestedGame)) 
                    {
                        System.out.println("[WORKER-" + port + "] Found! Sending Callback.");
                        out.writeObject(gamesList.get(requestedGame));//sending game obj
                    } 
                    else 
                    {
                        System.out.println("[WORKER-" + port + "] Game non existant here.");
                        out.writeObject("[ERROR] : Game not found.");
                    }
                    out.flush();
                }
                in.close();
                out.close();
                socket.close();
            }
        } 
        catch (Exception e) 
        {
            System.err.println("[ERROR] : Worker error: " + e.getMessage());
        }
    }
}