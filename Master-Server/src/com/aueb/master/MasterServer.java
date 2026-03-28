package com.aueb.master;

import java.util.*;
import java.io.*;
import java.net.*;

import com.aueb.shared.SearchRequest;
import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;
import com.aueb.shared.PlayRequest;
import com.aueb.shared.RateRequest;
import com.aueb.shared.RemoveGameRequest;

public class MasterServer 
{
    private static final int PORT = 1312;
    private static final int REDUCER_PORT = 7000;
    

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

    public static Object sendToReducer(Object data)
    {
        try(
            Socket rs = new Socket("localhost", REDUCER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(rs.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(rs.getInputStream());
        )
        {
            out.writeObject(data);
            out.flush();

            return in.readObject();
        }
        catch(Exception e)
        {
            System.out.println("[MASTER] Reducer communication error");
            return null;
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
            // Mesa ston Master ClientHandler
            else if (received instanceof SearchRequest) {
                SearchRequest req = (SearchRequest) received;
                List<List<Game>> finalResults = new ArrayList<>();
            
                for (int port : workerPorts) {
                    // Stelnoume to aitima se kathe worker [cite: 32]
                    Object response = forwardToWorker(port, req, null);
                    if (response instanceof List) {
                        // REDUCE: Prosthiki stin sinoliki lista [cite: 34]
                        finalResults.add((List<Game>) response);
                    }
                }

                Object reducedResults = MasterServer.sendToReducer(finalResults);
            
                // An i playAction zitise ena paixnidi, Steile to prwto Game, allios oli ti lista
                if (req.getGameName() != null) 
                {
                    List<Game> finalList = (List<Game>) reducedResults;
                    if(finalList != null && !finalList.isEmpty())
                    {
                        out.writeObject(finalList.get(0));
                    }
                    else
                    {
                        out.writeObject("Game not found!");
                    }
                } 
                else 
                {
                    out.writeObject(reducedResults);
                }
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
                List<List<Game>> allWorkerLists = new ArrayList<>();
                for (int port : workerPorts)
                {
                    Object resp = forwardToWorker(port, received, null);
                    if (resp instanceof List)
                    {
                        allWorkerLists.add((List<Game>) resp);
                    }
                }
                Object reduced = MasterServer.sendToReducer(allWorkerLists);
                out.writeObject(reduced);
                out.flush();
            }

            //bet
            else if (received instanceof PlayRequest)
            {
                PlayRequest req = (PlayRequest) received;
                int nodeId = Math.abs(req.getGameName().hashCode()) % workerPorts.length;
                
                Object response = forwardToWorker(workerPorts[nodeId], req, null);
                
                out.writeObject(response);
                out.flush();
            }
            else if(received instanceof RateRequest)
            {
                RateRequest rr = (RateRequest) received;
                System.out.println("[MASTER] : Received rating for " + rr.getGameName());

                int nodeId = Math.abs(rr.getGameName().hashCode()) % workerPorts.length;
                Object response = forwardToWorker(workerPorts[nodeId], rr, "Error communicating with worker");
                out.writeObject(response);
                out.flush();
            }

            //player statistics
            else if (received instanceof String && received.equals("GET_PLAYER_STATS")) 
            {
                System.out.println("[MASTER] : Aggregating player stats from all workers...");
                List<Map<String, Double>> partialMaps = new ArrayList<>();

                for (int port : workerPorts)
                {
                    Object response = forwardToWorker(port, "GET_PLAYER_STATS", null);
                    if (response instanceof Map)
                    {
                        partialMaps.add((Map<String, Double>) response);
                    }
                }
                System.out.println("[MASTER] Sending partial maps to Reducer...");

                Object reducedStats = MasterServer.sendToReducer(partialMaps);
                if(reducedStats == null)
                {
                    out.writeObject(new HashMap<String , Double>());
                }
                else
                {
                    out.writeObject(reducedStats);
                }
                out.flush();
            }
            else if(received instanceof String && received.equals("GET_PROVIDER_STATS"))
            {
                System.out.println("[MASTER] : Aggregating provider stats from all workers...");
                List<Map<String, Double>> partialMaps = new ArrayList<>();
                for (int port : workerPorts)
                {
                    Object response = forwardToWorker(port, "GET_PROVIDER_STATS", null);
                    if (response instanceof Map)
                    {
                        partialMaps.add((Map<String, Double>) response);
                    }
                }
                Object reducedStats = MasterServer.sendToReducer(partialMaps);
                out.writeObject(reducedStats);
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