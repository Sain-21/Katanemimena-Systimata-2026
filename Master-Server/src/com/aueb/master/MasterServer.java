package com.aueb.master;

import java.io.*;
import java.net.*;

import com.aueb.shared.Game;

public class MasterServer 
{
    //port
    private static final int PORT = 1312;

    public static void main(String[] args) 
    {
        System.out.println("Master Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) 
        {
            System.out.println("Master is listening on port " + PORT);

            while (true) 
                {
                //wait new connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                //new thread for each client
                Thread handler = new Thread(new ClientHandler(clientSocket));
                handler.start();
            }
        } 
        catch (IOException e) 
        {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

// Αυτή η κλάση αναλαμβάνει την επικοινωνία με τον κάθε πελάτη ξεχωριστά
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
            
            // Διαβάζουμε το αντικείμενο που έστειλε ο Client
            Object received = in.readObject();

            if (received instanceof Game) 
            {
                Game game = (Game) received;
                System.out.println("[MASTER] Received new game from Manager: " + game.getGameName());

                // 1. Έχουμε 1 Worker για την ώρα (στην πόρτα 5001)
                int numberOfNodes = 1; 

                // 2. Υπολογίζουμε σε ποιον Worker θα πάει (Η συνάρτηση Hash)
                int nodeId = Math.abs(game.getGameName().hashCode()) % numberOfNodes;

                // 3. Επειδή έχουμε μόνο 1 Worker τώρα, ξέρουμε ότι είναι στο port 5001
                int workerPort = 5001; 
                System.out.println("[MASTER] Forwarding game to Worker Node " + nodeId + " (Port: " + workerPort + ")");

                // 4. Στέλνουμε το παιχνίδι στον Worker μέσω νέου TCP Socket
                try (Socket workerSocket = new Socket("localhost", workerPort);
                     ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream())) {
                    
                    workerOut.writeObject(game);
                    workerOut.flush();
                    System.out.println("[MASTER] Game successfully forwarded to Worker!");
                    
                } catch (IOException e) {
                    System.err.println("[MASTER] Failed to send game to Worker: " + e.getMessage());
                }
            }

        } 
        catch (IOException | ClassNotFoundException e)
        {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }
}