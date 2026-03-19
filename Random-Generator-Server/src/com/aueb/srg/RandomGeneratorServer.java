package com.aueb.srg;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Random;

public class RandomGeneratorServer {
    private static final int PORT = 6000;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[SRG] Server started on port " + PORT);

            while (true) {
                // apodoxi sindesis
                Socket socket = serverSocket.accept();
                
                // antikeimeno pou tha diaxeiristei sindesi
                SRGHandler handler = new SRGHandler(socket);
                
                // dimiourgia kai start tou thread
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// klasi pou analambanei douleia gia kathe worker pou sindeetai
class SRGHandler implements Runnable {
    private Socket socket;
    private static final String SECRET_KEY = "LaloFroutaSecret";

    public SRGHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // 1. Paragogi tixaiou arithmou
            Random rand = new Random();
            int randomNumber = rand.nextInt(1000) + 1;

            // 2. Dimiourgia tou hash
            String rawString = randomNumber + SECRET_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawString.getBytes("UTF-8"));

            // 3. metatropi ton bytes se keimeno
            String hexHash = "";
            for (int i = 0; i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexHash += "0";
                hexHash += hex;
            }

            // 4. apostoli apotelesmatos (pinakas me 2 string)
            String[] response = new String[2];
            response[0] = String.valueOf(randomNumber);
            response[1] = hexHash;

            out.writeObject(response);
            out.flush();
            
            socket.close(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}