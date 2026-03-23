package com.aueb.master;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

import com.aueb.shared.SearchRequest;
import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;
import com.aueb.shared.PlayRequest;
import com.aueb.shared.RemoveGameRequest;

public class MasterServer 
{
    private static final int PORT = 1312;
    
    // static map gia apothikeysi statistikwn paiktwn
    public static Map<String, Double> playerStats = new ConcurrentHashMap<>();

    public static void main(String[] args) 
    {
        System.out.println("[MASTER] : Master Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) 
        {
            System.out.println("[MASTER] : Master is listening on port " + PORT);

            while (true) 
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[MASTER] : New client connected: " + clientSocket.getInetAddress());

                Thread handler = new Thread(new ClientHandler(clientSocket));
                handler.start();
            }
        } 
        catch (IOException e) 
        {
            System.err.println("[ERROR] : Server error: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable 
{
    private Socket socket;
    private static final int[] workerPorts = {5001, 5002};

    public ClientHandler(Socket socket) 
    {
        this.socket = socket;
    }

    @Override
    public void run() 
    {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) 
        {
            Object received = in.readObject();

            // add game
            if (received instanceof Game) 
            {
                Game game = (Game) received;
                System.out.println("[MASTER] : Received game: " + game.getGameName());

                int nodeId = Math.abs(game.getGameName().hashCode()) % workerPorts.length;
                int targetPort = workerPorts[nodeId];

                // proothisi ston worker
                forwardToWorker(targetPort, game, null);
                System.out.println("[MASTER] : Game " + game.getGameName() + " sent to Worker on port " + targetPort);
                out.writeObject("Game " + game.getGameName() + " received and forwarded.");
                out.flush();
            }

            //game search
            else if (received instanceof SearchRequest) 
            {
                SearchRequest req = (SearchRequest) received;
                System.out.println("[MASTER] MapReduce Search for: " + req.getGameName());

                Game foundGame = null;
                for(int port : workerPorts)
                {
                    Object response = forwardToWorker(port, req, null);
                    if(response instanceof Game) 
                    {
                        foundGame = (Game) response;
                        break; 
                    }
                }
                out.writeObject(foundGame != null ? foundGame : "Game not found!");
                out.flush();
            }

            //remove game
            else if (received instanceof RemoveGameRequest)
            {
                RemoveGameRequest req = (RemoveGameRequest) received;
                int nodeId = Math.abs(req.getGameName().hashCode()) % workerPorts.length;
                Object response = forwardToWorker(workerPorts[nodeId], req, null);
                out.writeObject(response);
                out.flush();
            }

            //game list
            else if (received instanceof ListGamesRequest)
            {
                List<Game> allGames = new ArrayList<>();
                for (int port : workerPorts)
                {
                    Object resp = forwardToWorker(port, received, null);
                    if (resp instanceof List) {
                        allGames.addAll((List<Game>) resp);
                    }
                }
                out.writeObject(allGames);
                out.flush();
            }

            //bet
            else if (received instanceof PlayRequest)
            {
                PlayRequest req = (PlayRequest) received;
                int nodeId = Math.abs(req.getGameName().hashCode()) % workerPorts.length;
                
                //send request to worker
                Object response = forwardToWorker(workerPorts[nodeId], req, null);
        
                double bet = req.getBetAmount();
                double payout = 0.0; 
                
                if (response instanceof Double)
                {
                    payout = (Double) response;
                } 
                else if (response instanceof String) 
                {
                    try 
                    {
                        if (((String) response).contains("WIN")) 
                        {
                            payout = Double.parseDouble(((String) response).replaceAll("[^0-9.]", ""));
                        }
                    } 
                    catch (Exception e) 
                    { 
                        payout = 0.0; 
                    }
                }

                //casino profit
                double netProfit = bet - payout;
                MasterServer.playerStats.merge(req.getPlayerName(), netProfit, Double::sum);

                out.writeObject(response);
                out.flush();
            }

            //player statistics
            else if (received instanceof String && received.equals("GET_PLAYER_STATS")) 
            {
                System.out.println("[MASTER] : Aggregating player stats from all workers...");
                Map<String, Double> globalPlayerStats = new HashMap<>();
                        
                for (int port : workerPorts) 
                {
                    try (Socket workerSocket = new Socket("localhost", port);
                         ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                         ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) 
                         {
                        
                        // Στέλνουμε το αίτημα στον Worker
                        workerOut.writeObject("GET_PLAYER_STATS");
                        workerOut.flush();
                        
                        Object response = workerIn.readObject();
                        if (response instanceof Map) 
                        {
                            Map<String, Double> workerMap = (Map<String, Double>) response;
                            
                            for (Map.Entry<String, Double> entry : workerMap.entrySet()) 
                            {
                                globalPlayerStats.merge(entry.getKey(), entry.getValue(), Double::sum);
                            }
                        }
                    } 
                    catch (Exception e) 
                    {
                        System.err.println("[MASTER] Could not get stats from worker at port " + port);
                    }
                }
                //send map to manager
                out.writeObject(globalPlayerStats);
                out.flush();
            }
        }
        catch (IOException | ClassNotFoundException e)
        {
            System.err.println("[ERROR] : Error handling client: " + e.getMessage());
        }
    }

    //helper func for communication with workers
    private Object forwardToWorker(int port, Object request, Object defaultValue) 
    {
        try (Socket workerSocket = new Socket("localhost", port);
             ObjectOutputStream oos = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(workerSocket.getInputStream())) 
             {
            
            oos.writeObject(request);
            oos.flush();
            return ois.readObject();
        }
        catch (Exception e) 
        {
            System.err.println("[MASTER] Worker at " + port + " error: " + e.getMessage());
            return defaultValue;
        }
    }
}