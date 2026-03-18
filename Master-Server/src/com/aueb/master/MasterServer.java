package com.aueb.master;

import java.io.*;
import java.net.*;

import com.aueb.shared.Game;
import com.aueb.shared.RemoveGameRequest;

public class MasterServer 
{
    //port
    private static final int PORT = 1312;

    public static void main(String[] args) 
    {
        System.out.println("[MASTER] : Master Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) 
        {
            System.out.println("[MASTER] : Master is listening on port " + PORT);

            while (true) 
            {
                //wait new connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("[MASTER] : New client connected: " + clientSocket.getInetAddress());

                //new thread for each client
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

// client handler for each client
class ClientHandler implements Runnable 
{
    private Socket socket;

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
            
            //read object that client sent
            Object received = in.readObject();

            if (received instanceof Game) 
            {
                Game game = (Game) received;
                System.out.println("[MASTER] : Received game: " + game.getGameName());

                // diathesimoi workers (2)
                int[] workerPorts = {5001, 5002};
                int numberOfNodes = workerPorts.length;

                // sinatisi hash
                int nodeId = Math.abs(game.getGameName().hashCode()) % numberOfNodes;
                int targetPort = workerPorts[nodeId]; //5001 h 5002

                System.out.println("[MASTER] : Forwarding game to Worker " + nodeId+1 + " on port " + targetPort);

                // send game to worker
                try (Socket workerSocket = new Socket("localhost", targetPort);
                     ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream())) 
                    {
                    
                    workerOut.flush();
                    
                // dimiourgw to Input xwris na diavazoume tipota, gia na min skasei o worker
                ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream());
                                
                workerOut.writeObject(game);
                workerOut.flush();
                } 
                catch (IOException e) 
                {
                    System.err.println("[ERROR] : Failed to send game to Worker on port " + targetPort);
                }
            }
            else if (received instanceof String) 
            {
                String gameName = (String) received;
                System.out.println("[MASTER] : Search Query for: " + gameName);

                int targetPort = (Math.abs(gameName.hashCode()) % 2 == 0) ? 5001 : 5002;
                System.out.println("[MASTER] : Asking Worker at port " + targetPort);

                try (Socket workerSocket = new Socket("localhost", targetPort);
                    ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                    ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream());) 
                {   
                    workerOut.writeObject(gameName);
                    workerOut.flush();
                    
                    Object response = workerIn.readObject();
                    out.writeObject(response);//send answer to player
                    out.flush();
                    System.out.println("[MASTER] : Got answer from Worker " + targetPort + " | Found game: "+ gameName);
                    System.out.println("[MASTER] : Forwarding to player");
                } 
                catch (Exception e) 
                {
                    System.err.println("[ERROR] : Error querying worker: " + e.getMessage());
                }
            }
            else if (received instanceof RemoveGameRequest)
            {
                RemoveGameRequest req = (RemoveGameRequest) received;
                String gameName = req.getGameName();

                System.out.println("[MASTER] Remove request for: " + gameName);

                int[] workerPorts = {5001 , 5002};
                int nodeId = Math.abs(gameName.hashCode()) % workerPorts.length;
                int targetPort = workerPorts[nodeId];

                try(Socket workerSocket = new Socket("localhost" , targetPort);
                    ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                    ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream()))
                {
                        workerOut.writeObject(req);
                        workerOut.flush();

                        Object response = workerIn.readObject();

                        out.writeObject(response);
                        out.flush();
                        
                }
            }
        } 
        catch (IOException | ClassNotFoundException e)
        {
            System.err.println("[ERROR] : Error handling client: " + e.getMessage());
        }
    }
}