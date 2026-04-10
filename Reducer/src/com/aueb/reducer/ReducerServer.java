package com.aueb.reducer;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import com.aueb.shared.Game;

public class ReducerServer
{
    private static final int PORT = 7000;
    public static void main(String[] args) throws Exception
    {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[REDUCER] Listening on port " + PORT);

        while(true)
        {
            Socket socket = server.accept();
            //handle each client in a new thread
            new Thread(() -> handle(socket)).start();
        }
    }
    

    private static void handle(Socket socket)
    {
        try(
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        )
        {
            Object data = in.readObject();
            System.out.println("[REDUCER] Received: " + data.getClass().getSimpleName());

            if (data instanceof List)
            {
                List<?> incomingList = (List<?>) data;

                if (incomingList.isEmpty())
                {
                    out.writeObject(incomingList);
                }
                else if (incomingList.get(0) instanceof Map)
                {
                    Map<String, Double> reducedMap = reduceStats((List<Map<String, Double>>) incomingList);
                    System.out.println("[REDUCER] Stats merged.");
                    out.writeObject(reducedMap);
                }
                else if (incomingList.get(0) instanceof List)
                {
                    List<Game> combinedGames = reduceGames((List<List<Game>>) incomingList);
                    System.out.println("[REDUCER] Game lists combined.");
                    out.writeObject(combinedGames);
                }
            }
            out.flush();
        }
        catch(Exception e)
        {
            System.err.println("[REDUCER ERROR] : " + e.getMessage());
        }
    }

    private static List<Game> reduceGames(List<List<Game>> intermediateLists) 
    {
        List<Game> finalResult = new ArrayList<>();
        for (List<Game> subList : intermediateLists) 
        {
            finalResult.addAll(subList);
        }
        return finalResult;
    }

    private static Map<String, Double> reduceStats(List<Map<String, Double>> intermediateMaps) 
    {
        Map<String, Double> finalStats = new HashMap<>();
        for (Map<String, Double> partialMap : intermediateMaps)
        {
            for (String key : partialMap.keySet())
            {
                finalStats.merge(key, partialMap.get(key), Double::sum);
            }
        }
        return finalStats;
    }
}