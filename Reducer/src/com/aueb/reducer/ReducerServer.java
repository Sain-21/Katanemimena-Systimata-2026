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
    //HashMap pou krataei to requestId kai tis listes apo tous workers
    private static HashMap<String, ArrayList<Object>> pendingResults = new HashMap<>();
    private static final int TOTAL_WORKERS = 2;

    public static void main(String[] args) throws Exception
    {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[REDUCER] Listening on port " + PORT);

        while(true)
        {
            final Socket s = server.accept();
            Thread t = new Thread(new Runnable() {
                public void run() 
                {
                    handle(s);
                }
            });
            t.start();
        }
    }
    

    private static void handle(Socket s)
    {
        try
        {
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            Object[] data = (Object[]) in.readObject();

            final String requestId = (String) data[0];
            Object partialData = data[1];

            synchronized(pendingResults)
            {
                if (!pendingResults.containsKey(requestId))
                {
                    pendingResults.put(requestId, new ArrayList<Object>());
                }
                pendingResults.get(requestId).add(partialData);

                //An pirame apotelesmata apo olous tous workers
                if (pendingResults.get(requestId).size() == TOTAL_WORKERS)
                {
                    //elegxoume ti tupou einai ta dedomena (apo to 1o stoixeio)
                    Object firstItem = pendingResults.get(requestId).get(0);
                    Object finalResult = null;

                    if (firstItem instanceof List)
                    {
                        //anazitisi paixnidiou -> reduce listes
                        List<Game> finalMatches = new ArrayList<>();
                        for (Object workerList : pendingResults.get(requestId))
                        {
                            finalMatches.addAll((List<Game>) workerList);
                        }
                        finalResult = finalMatches;
                    }
                    else if (firstItem instanceof Map)
                    {
                        //einai statistika -> reduce ta maps
                        Map<String, Double> finalStats = new HashMap<>();
                        for (Object workerMap : pendingResults.get(requestId))
                        {
                            Map<String, Double> partialMap = (Map<String, Double>) workerMap;
                            for (String key : partialMap.keySet())
                            {
                                finalStats.merge(key, partialMap.get(key), Double::sum);
                            }
                        }
                        finalResult = finalStats;
                    }
                    if (finalResult != null)
                    {
                        sendToMaster(requestId, finalResult);
                    }
                    pendingResults.remove(requestId);
                }
            }
            s.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void sendToMaster(String requestId, Object finalResult)
    {
        try
        {
            Socket masterSocket = new Socket("localhost", 1312);
            ObjectOutputStream out = new ObjectOutputStream(masterSocket.getOutputStream());
            out.writeObject(new Object[]{requestId, finalResult});
            out.flush();
            masterSocket.close();
            System.out.println("[REDUCER] Sent final results to Master for Request: " + requestId);
        }
        catch(Exception e)
        {
            System.err.println("[REDUCER] Could not reach Master on port 1312: " + e.getMessage());
        }
    }
}