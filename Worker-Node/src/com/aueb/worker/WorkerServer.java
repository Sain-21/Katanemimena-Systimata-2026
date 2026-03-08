

import com.aueb.shared.Game;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerServer 
{
    // Εδώ θα αποθηκεύονται τα παιχνίδια στη μνήμη του Worker
    private static ConcurrentHashMap<String, Game> gamesList = new ConcurrentHashMap<>();

    public static void main(String[] args) 
    {
        int port = 5001; // Η πόρτα του Worker
        System.out.println("Worker Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(port)) 
        {
            System.out.println("Worker is listening on port " + port);

            while (true)
                {
                Socket socket = serverSocket.accept();
                
                // Διαβάζουμε το παιχνίδι που μας έστειλε ο Master
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object received = in.readObject();

                if (received instanceof Game) 
                {
                    Game game = (Game) received;
                    // Το αποθηκεύουμε στη μνήμη
                    gamesList.put(game.getGameName(), game);
                    System.out.println("[WORKER] Received and saved game: " + game.getGameName());
                }
                
                in.close();
                socket.close();
            }
        } 
        catch (Exception e) 
        {
            System.err.println("Worker error: " + e.getMessage());
        }
    }
}