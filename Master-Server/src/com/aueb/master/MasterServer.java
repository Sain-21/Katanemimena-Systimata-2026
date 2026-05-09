package com.aueb.master;

import java.util.*;
import java.io.*;
import java.net.*;

import com.aueb.shared.SearchRequest;
import com.aueb.shared.SyncRequest;
import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;
import com.aueb.shared.PlayRequest;
import com.aueb.shared.RateRequest;
import com.aueb.shared.RemoveGameRequest;

public class MasterServer 
{
    private static final int PORT = 1312;
    private static final int REDUCER_PORT = 7000;
    private static final String REDUCER_HOST = "Ip tou upologisti pou trexei reducer";
    private static final String SRG_HOST = "IP tou upologisti pou trexei ton srg";
    private static final int SRG_PORT = 6000;
    

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

    public static Object sendToReducer(Object data , String requestId)
    {
        try(
            Socket rs = new Socket(REDUCER_HOST, REDUCER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(rs.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(rs.getInputStream());
        )
        {
            out.writeObject(new Object[]{requestId, data});
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
    //private static final int[] workerPorts = {5001, 5002};
    private static final String[] workerHosts = {"PC1_IP", "PC2_IP", "PC3_IP"}; // Βάλε σωστές IP
    private static final int[] workerPorts = {5001, 5002, 5003};
    private static final String SRG_HOST = "IP tou upologisti pou trexei ton srg";
    private static final int SRG_PORT = 6000;

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
                
                try(Socket srgSocket = new Socket(SRG_HOST , SRG_PORT);
                    ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
                    ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream()))
                {
                    srgOut.writeObject(new String[]{"ADD_GAME", game.getGameName(), game.getHashKey()});
                    srgOut.flush();

                    Object ack = srgIn.readObject();
                    System.out.println("[MASTER] : SRG response for " + game.getGameName() + " => " + ack);
                }
                catch (Exception e)
                {
                    System.err.println("[MASTER] : SRG communication error: " + e.getMessage());
                }

                // ΛΟΓΙΚΗ REPLICATION: Στέλνουμε το παιχνίδι και στους δύο!
                //int primaryId = Math.abs(game.getGameName().hashCode()) % workerPorts.length;
                //int backupId = (primaryId + 1) % workerPorts.length;
//
                //forwardToWorker(workerPorts[primaryId], game, null);
                //forwardToWorker(workerPorts[backupId], game, null);

                int primaryId = Math.abs(game.getGameName().hashCode()) % workerPorts.length;
                int backup1Id = (primaryId + 1) % workerPorts.length;
                int backup2Id = (primaryId + 2) % workerPorts.length;

                forwardToWorker(primaryId, game, null);
                forwardToWorker(backup1Id, game, null);
                forwardToWorker(backup2Id, game, null);
                
                System.out.println("[MASTER] : Game " + game.getGameName() + " replicated to nodes " + primaryId + ", " + backup1Id + ", " + backup2Id);                out.writeObject("Game " + game.getGameName() + " received and replicated.");
                out.flush();
            }

            //game search
            else if (received instanceof SearchRequest) 
            {
                SearchRequest req = (SearchRequest) received;
                List<Object> partialResults = new ArrayList<>();
            
                for (int port : workerPorts) 
                {
                    Object response = forwardToWorker(port, req, null);
                    partialResults.add(response);
                }

                Object reducedResults = MasterServer.sendToReducer(partialResults , "SEARCH_REQUEST_" + System.currentTimeMillis());
            
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
                boolean removed = false;

                for(int port : workerPorts)
                {
                    Object response = forwardToWorker(port, req, null);
                    if("Game Removed!".equals(response))
                    {
                        removed = true;
                    }
                }
                out.writeObject(removed ? "Game Removed!" : "Game not found!");
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
                Object reduced = MasterServer.sendToReducer(allWorkerLists , "LIST_GAMES_REQUEST_" + System.currentTimeMillis());
                out.writeObject(reduced);
                out.flush();
            }

            //bet
            //bet
            //else if (received instanceof PlayRequest)
            //{
            //    PlayRequest req = (PlayRequest) received;
            //    int primaryId = Math.abs(req.getGameName().hashCode()) % workerPorts.length;
            //    int backupId = (primaryId + 1) % workerPorts.length;
            //    
            //    int primaryPort = workerPorts[primaryId];
            //    int backupPort = workerPorts[backupId];
//
            //    // Στέλνουμε αρχικά στον Primary. Αν επιστρέψει το default "FAIL", πάει να πει πως έπεσε.
            //    Object response = forwardToWorker(primaryPort, req, "FAIL");
            //    
            //    if ("FAIL".equals(response)) 
            //    {
            //        System.out.println("[MASTER FAILOVER] : Primary " + primaryPort + " is down! Routing PlayRequest to replica " + backupPort);
            //        response = forwardToWorker(backupPort, req, "FAIL");
            //    } 
            //    else 
            //    {
            //        // Αν ο Primary πέτυχε το Play, πρέπει να συγχρονίσουμε αθόρυβα τον Backup!
            //        if (response instanceof Double) 
            //        {
            //            double payout = (Double) response;
            //            SyncRequest syncReq = new SyncRequest("PLAY", req.getGameName(), req.getPlayerName(), req.getBetAmount(), payout);
            //            forwardToWorker(backupPort, syncReq, null);
            //        }
            //    }
            //    
            //    out.writeObject(response);
            //    out.flush();
            //}

            else if (received instanceof PlayRequest)
            {
                PlayRequest req = (PlayRequest) received;
                int primaryId = Math.abs(req.getGameName().hashCode()) % workerPorts.length;
                int backup1Id = (primaryId + 1) % workerPorts.length;
                int backup2Id = (primaryId + 2) % workerPorts.length;

                // Δοκιμάζουμε τον Primary
                Object response = forwardToWorker(primaryId, req, "FAIL");
                int successfulNode = primaryId;
                
                if ("FAIL".equals(response)) 
                {
                    System.out.println("[MASTER FAILOVER] : Primary " + primaryId + " is down! Routing PlayRequest to Backup 1 (" + backup1Id + ")");
                    response = forwardToWorker(backup1Id, req, "FAIL");
                    successfulNode = backup1Id;
                    
                    if ("FAIL".equals(response)) 
                    {
                        System.out.println("[MASTER FAILOVER] : Backup 1 is down! Routing PlayRequest to Backup 2 (" + backup2Id + ")");
                        response = forwardToWorker(backup2Id, req, "FAIL");
                        successfulNode = backup2Id;
                    }
                } 

                // Αν κάποιος από τους 3 τα κατάφερε, συγχρονίζουμε τους υπόλοιπους
                if (!"FAIL".equals(response) && response instanceof Double) 
                {
                    double payout = (Double) response;
                    SyncRequest syncReq = new SyncRequest("PLAY", req.getGameName(), req.getPlayerName(), req.getBetAmount(), payout);
                    
                    if (successfulNode != primaryId) forwardToWorker(primaryId, syncReq, null);
                    if (successfulNode != backup1Id) forwardToWorker(backup1Id, syncReq, null);
                    if (successfulNode != backup2Id) forwardToWorker(backup2Id, syncReq, null);
                }
                
                out.writeObject(response);
                out.flush();
            }
            
            else if(received instanceof RateRequest)
            {
                RateRequest rr = (RateRequest) received;
                System.out.println("[MASTER] : Received rating for " + rr.getGameName());

                int primaryId = Math.abs(rr.getGameName().hashCode()) % workerPorts.length;
                int backupId = (primaryId + 1) % workerPorts.length;

                Object response = forwardToWorker(workerPorts[primaryId], rr, "FAIL");

                if ("FAIL".equals(response)) 
                {
                    System.out.println("[MASTER FAILOVER] : Primary is down! Routing RateRequest to replica.");
                    response = forwardToWorker(workerPorts[backupId], rr, "Error communicating with worker");
                }
                else
                {
                    // Συγχρονίζουμε τα αστέρια στον Backup
                    SyncRequest syncReq = new SyncRequest("RATE", rr.getGameName(), rr.getPlayerName(), rr.getStars());
                    forwardToWorker(workerPorts[backupId], syncReq, null);
                }
                
                out.writeObject(response);
                out.flush();
            }

            //player statistics
            else if (received instanceof String && received.equals("GET_PLAYER_STATS")) 
            {
                System.out.println("[MASTER] : Getting player stats from all workers...");
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

                Object reducedStats = MasterServer.sendToReducer(partialMaps , "GET_PLAYER_STATS_REQUEST_" + System.currentTimeMillis());
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
                System.out.println("[MASTER] : Getting provider stats from all workers...");
                List<Map<String, Double>> partialMaps = new ArrayList<>();
                for (int port : workerPorts)
                {
                    Object response = forwardToWorker(port, "GET_PROVIDER_STATS", null);
                    if (response instanceof Map)
                    {
                        partialMaps.add((Map<String, Double>) response);
                    }
                }
                Object reducedStats = MasterServer.sendToReducer(partialMaps , "GET_PROVIDER_STATS_REQUEST_" + System.currentTimeMillis());
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
        try (Socket workerSocket = new Socket(workerHosts[port] , workerPorts[port]);
             ObjectOutputStream oos = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(workerSocket.getInputStream())) 
             {
            
            oos.writeObject(request);
            oos.flush();
            return ois.readObject();
        }
        catch (Exception e) 
        {
            System.err.println("[MASTER] Worker at " + workerHosts[port] + ":" + workerPorts[port] + " error: " + e.getMessage());
            return defaultValue;
        }
    }
}