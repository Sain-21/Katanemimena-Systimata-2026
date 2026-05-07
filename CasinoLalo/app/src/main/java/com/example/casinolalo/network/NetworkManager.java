package com.example.casinolalo.network;

import android.util.Log;

import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class NetworkManager
{
    //local host of pc in emulator
    private static final String HOST = "10.0.2.2";
    private static final int PORT = 1312;

    public interface GameListCallback
    {
        void onSuccess(List<Game> games);
        void onError(String errorMsg);
    }

    public static void fetchAllGames(GameListCallback callback)
    {
        new Thread(() ->
        {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                //send request
                out.writeObject(new ListGamesRequest());
                out.flush();

                // read answer
                Object response = in.readObject();
                if (response instanceof List)
                {
                    List<Game> games = (List<Game>) response;
                    callback.onSuccess(games);
                }

            }
            catch (Exception e)
            {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public interface PlayCallback
    {
        void onResult(String resultMessage, double payout);
        void onError(String errorMsg);
    }

    public static void playGame(String username, String gameName, double betAmount, PlayCallback callback)
    {
        new Thread(() ->
        {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
            {

                //play request
                out.writeObject(new com.aueb.shared.PlayRequest(username, gameName, betAmount));
                out.flush();

                // read master answer
                Object response = in.readObject();

                if (response instanceof Double)
                {
                    double payout = (Double) response;
                    String msg;

                    if (payout > betAmount) msg = "WIN! Κέρδισες " + payout + "!";
                    else if (payout == 0) msg = "LOST! Καλή τύχη την επόμενη φορά.";
                    else msg = "Επιστροφή: " + payout + "!";

                    callback.onResult(msg, payout);
                }
                else if (response instanceof String)
                {
                    callback.onResult("[SERVER] " + response, betAmount);
                }

            }
            catch (Exception e)
            {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void rateGame(String username, String gameName, int stars)
    {
        new Thread(() ->
        {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
            {

                out.writeObject(new com.aueb.shared.RateRequest(gameName, username, stars));
                out.flush();
                Object response = in.readObject();
                Log.d("Rating", response.toString());

            } catch (Exception e) {
                Log.e("Rating", "Σφάλμα βαθμολογίας: " + e.getMessage());
            }
        }).start();
    }
}