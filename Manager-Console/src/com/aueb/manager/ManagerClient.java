package com.aueb.manager;

import com.aueb.shared.Game;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagerClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1312; //port

        // read game.json
        List<Game> gamesToSend = readGamesFromJson("C:\\Users\\paink\\Desktop\\Katanemimena-Systimata-2026\\Resources\\game.json");
        
        System.out.println("[CLIENT] : Found " + gamesToSend.size() + " games in JSON. Sending...\n");

        // send game to master
        for (Game g : gamesToSend) 
            {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) 
                {
                
                System.out.println("[CLIENT] : Sending game: " + g.getGameName());
                out.writeObject(g);
                out.flush();
                
                Thread.sleep(500); 

            } 
            catch (Exception e) 
            {
                System.err.println("[ERROR] : game connection" + g.getGameName() + ": " + e.getMessage());
            }
        }
    }

    // --- ΒΟΗΘΗΤΙΚΕΣ ΜΕΘΟΔΟΙ ΓΙΑ ΔΙΑΒΑΣΜΑ JSON (Χωρίς εξωτερικές βιβλιοθήκες) ---
    //Gemini

    private static List<Game> readGamesFromJson(String filePath) {
        List<Game> games = new ArrayList<>();
        try {
            // Διαβάζουμε όλο το κείμενο από το αρχείο
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Ψάχνουμε οτιδήποτε βρίσκεται ανάμεσα σε { }
            Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(jsonContent);

            while (matcher.find()) {
                String objStr = matcher.group(1);
                
                // Εξάγουμε τα δεδομένα
                String name = extractValue(objStr, "gameName");
                String provider = extractValue(objStr, "provider");
                int reels = Integer.parseInt(extractValue(objStr, "reels"));
                int maxWin = Integer.parseInt(extractValue(objStr, "maxWin"));
                String imageUrl = extractValue(objStr, "imageUrl");
                double minBet = Double.parseDouble(extractValue(objStr, "minBet"));
                double maxBet = Double.parseDouble(extractValue(objStr, "maxBet"));
                String volatility = extractValue(objStr, "volatility");
                String gameHash = extractValue(objStr, "gameHash");

                // Δημιουργούμε το αντικείμενο και το βάζουμε στη λίστα
                games.add(new Game(name, provider, reels, maxWin, imageUrl, minBet, maxBet, volatility, gameHash));
            }
        } 
        catch (Exception e) 
        {
            System.err.println("[ERROR] : Error reading the JSON file: " + e.getMessage());
        }
        return games;
    }

    private static String extractValue(String jsonObject, String key) {
        // Ψάχνει το "key": "value" ή "key": 123 στο κείμενο
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(jsonObject);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "0";
    }
}