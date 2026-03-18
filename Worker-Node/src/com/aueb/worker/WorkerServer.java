package com.aueb.worker;

import com.aueb.shared.Game;
import com.aueb.shared.RemoveGameRequest;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class WorkerServer 
{
    // save games to worker memory
    private static HashMap<String, Game> gamesList = new HashMap<String , Game>();

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
            while(true)
            {
                Socket socket = serverSocket.accept();

                System.out.println("[WORKER-" + port + "] New connection");

                new Thread(new WorkerHandler(socket , gamesList)).start();
            }
        } 
        catch (Exception e) 
        {
            System.err.println("[ERROR] : Worker error: " + e.getMessage());
        }
    }
}