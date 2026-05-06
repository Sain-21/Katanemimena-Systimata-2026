package com.example.casinolalo.network;

import android.util.Log;

import com.aueb.shared.Game;
import com.aueb.shared.ListGamesRequest;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class NetworkManager {

    // 10.0.2.2 είναι το localhost του υπολογιστή σου μέσα από τον Emulator!
    private static final String HOST = "10.0.2.2";
    private static final int PORT = 1312;

    // Interface για να μιλάει το Thread με το MainActivity
    public interface GameListCallback {
        void onSuccess(List<Game> games);
        void onError(String errorMsg);
    }

    public static void fetchAllGames(GameListCallback callback) {
        // Ανοίγουμε νέο Thread για να μην κολλήσει το UI[cite: 1]
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                Log.d("Network", "Συνδέθηκε στον Master!");

                // Στέλνουμε το request
                out.writeObject(new ListGamesRequest());
                out.flush();

                // Διαβάζουμε την απάντηση
                Object response = in.readObject();
                if (response instanceof List) {
                    List<Game> games = (List<Game>) response;
                    callback.onSuccess(games); // Στέλνουμε τα παιχνίδια πίσω στην οθόνη
                }

            } catch (Exception e) {
                Log.e("Network", "Σφάλμα: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }).start(); // Ξεκινάει το Thread!
    }

    // --- Νέο Interface για το Αποτέλεσμα του Παιχνιδιού ---
    public interface PlayCallback {
        void onResult(String resultMessage, double payout);
        void onError(String errorMsg);
    }

    // --- Νέα μέθοδος για την αποστολή του PlayRequest ---
    public static void playGame(String username, String gameName, double betAmount, PlayCallback callback) {
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Στέλνουμε το PlayRequest (ακριβώς όπως το έκανες στο console app)
                out.writeObject(new com.aueb.shared.PlayRequest(username, gameName, betAmount));
                out.flush();

                // Διαβάζουμε την απάντηση του Master
                Object response = in.readObject();

                if (response instanceof Double) {
                    double payout = (Double) response;
                    String msg;

                    if (payout > betAmount) msg = "WIN! Κέρδισες " + payout + " FUN!";
                    else if (payout == 0) msg = "LOST! Καλή τύχη την επόμενη φορά.";
                    else msg = "Επιστροφή: " + payout + " FUN";

                    callback.onResult(msg, payout);
                }
                else if (response instanceof String) {
                    // Περίπτωση που ο Server στείλει κάποιο μήνυμα λάθους
                    callback.onResult("[SERVER] " + response, betAmount); // Επιστρέφουμε το ποσό πίσω
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void rateGame(String username, String gameName, int stars) {
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(new com.aueb.shared.RateRequest(gameName, username, stars));
                out.flush();
                // Λαμβάνουμε την απάντηση (το μήνυμα επιτυχίας/αποτυχίας του Master)
                Object response = in.readObject();
                Log.d("Rating", response.toString());

            } catch (Exception e) {
                Log.e("Rating", "Σφάλμα βαθμολογίας: " + e.getMessage());
            }
        }).start();
    }
}