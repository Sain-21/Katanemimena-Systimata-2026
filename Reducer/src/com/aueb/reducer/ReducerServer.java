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
                    Map<String, Double> reducedMap = new HashMap<>();
                    for (Object obj : incomingList)
                    {
                        Map<String, Double> part = (Map<String, Double>) obj;
                        for (String key : part.keySet())
                        {
                            reducedMap.merge(key, part.get(key), Double::sum);
                        }
                    }
                    System.out.println("[REDUCER] Stats merged.");
                    out.writeObject(reducedMap);
                }
                else if (incomingList.get(0) instanceof List)
                {
                    List<Game> combinedGames = new ArrayList<>();
                    for (Object obj : incomingList)
                    {
                        combinedGames.addAll((List<Game>) obj);
                    }
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

}