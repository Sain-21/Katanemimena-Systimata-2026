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
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        )
        {
            Object[] data = (Object[]) in.readObject();

            String requestId = (String) data[0];
            List<Object> partialResults =(List<Object>) data[1];

            Object finalResult;

            if(requestId.startsWith("GET_PLAYER_STATS") || requestId.startsWith("GET_PROVIDER_STATS"))
            {
                Map<String, Double> mergedMap = new HashMap<>();
                for (Object obj : partialResults)
                {
                    Map<String , Double> map = (Map<String , Double>) obj;
                    for (Map.Entry<String, Double> e : map.entrySet())
                    {
                         mergedMap.merge(e.getKey(), e.getValue(), Double::sum);
                    }
                }
                 finalResult = mergedMap;
            }
            else
            {
                List<Game> merged = new ArrayList<>();
                for(Object obj : partialResults)
                {
                    List<Game> list = (List<Game>) obj;
                    merged.addAll(list);
                }
                finalResult = merged;
            }
            out.writeObject(finalResult);
            out.flush();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}