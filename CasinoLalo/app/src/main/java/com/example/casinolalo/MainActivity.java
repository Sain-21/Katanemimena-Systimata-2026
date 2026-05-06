package com.example.casinolalo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.casinolalo.network.NetworkManager;
import com.aueb.shared.Game;

import java.util.List;

import android.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;

public class MainActivity extends AppCompatActivity {

    // Δηλώνουμε τις μεταβλητές μας
    private TextView tvBalance;
    private Button btnViewAll, btnAddBalance;
    private LinearLayout gamesContainer;
    private double balance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Προσαρμογή για τα περιθώρια της οθόνης (ώστε να μην κρύβεται πίσω από την κάμερα/notch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tvTitle), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Συνδέουμε τις μεταβλητές με το γραφικό περιβάλλον (XML)
        tvBalance = findViewById(R.id.tvBalance);
        btnViewAll = findViewById(R.id.btnViewAll);
        btnAddBalance = findViewById(R.id.btnAddBalance);
        gamesContainer = findViewById(R.id.gamesContainer);

        // --- Λειτουργία 1: Προσθήκη Υπολοίπου (Tokens) ---
        btnAddBalance.setOnClickListener(v -> {
            balance += 100.0; // Προσθέτουμε 100 δοκιμαστικά
            tvBalance.setText("Balance: " + balance + " FUN");
            Toast.makeText(MainActivity.this, "Μπήκαν 100 Tokens!", Toast.LENGTH_SHORT).show();
        });

        // --- Λειτουργία 2: Λήψη Παιχνιδιών από τον Master (Ασύγχρονα) ---
        // --- Λειτουργία 2: Λήψη Παιχνιδιών από τον Master ---
        btnViewAll.setOnClickListener(v -> {
            NetworkManager.fetchAllGames(new NetworkManager.GameListCallback() {
                @Override
                public void onSuccess(List<Game> games) {
                    runOnUiThread(() -> {
                        gamesContainer.removeAllViews(); // Καθαρίζουμε τη λίστα

                        for (Game game : games) {
                            Button gameBtn = new Button(MainActivity.this);
                            gameBtn.setText(game.getGameName() + " | Risk: " + game.getRiskLevel() + " | Min: " + game.getMinBet());

                            // Όταν πατάει το παιχνίδι, ανοίγει το Dialog!
                            gameBtn.setOnClickListener(view -> showPlayDialog(game));

                            gamesContainer.addView(gameBtn);
                        }
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    // Αντί για Toast, δείχνουμε ένα σοβαρό παραθυράκι λάθους
                    runOnUiThread(() -> showErrorDialog("Σφάλμα Δικτύου", errorMsg));
                }
            });
        });
    }

    // --- Η μέθοδος που φτιάχνει το αναδυόμενο παράθυρο πονταρίσματος ---
    private void showPlayDialog(Game game) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ποντάρισμα: " + game.getGameName());
        builder.setMessage("Όρια: " + game.getMinBet() + " - " + game.getMaxBet() + " FUN\nΔιαθέσιμο Υπόλοιπο: " + balance + " FUN");

        // Φτιάχνουμε ένα πεδίο εισαγωγής κειμένου (μόνο για αριθμούς)
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Ποσό πονταρίσματος");
        builder.setView(input);

        // Κουμπί PLAY
        builder.setPositiveButton("PLAY", (dialog, which) -> {
            String betStr = input.getText().toString();
            if (betStr.isEmpty()) return;

            double amount = Double.parseDouble(betStr);

            // Έλεγχοι (ακριβώς όπως στο Console app)
            if (amount < game.getMinBet() || amount > game.getMaxBet()) {
                showErrorDialog("Λάθος Ποσό", "Το ποντάρισμα πρέπει να είναι ανάμεσα σε " + game.getMinBet() + " και " + game.getMaxBet() + " FUN.");
                return;
            }
            if (amount > balance) {
                showErrorDialog("Ανεπαρκές Υπόλοιπο", "Δεν έχεις αρκετά Tokens για αυτό το ποντάρισμα.");
                return;
            }

            // Αφαιρούμε το ποσό προσωρινά
            balance -= amount;
            tvBalance.setText("Balance: " + balance + " FUN");

            // Στέλνουμε το Request στον Master (Βάλαμε "Player1" προσωρινά για username)
            NetworkManager.playGame("Player1", game.getGameName(), amount, new NetworkManager.PlayCallback() {
                @Override
                public void onResult(String resultMessage, double payout) {
                    runOnUiThread(() -> {
                        balance += payout; // Προσθέτουμε το κέρδος (αν υπάρχει)
                        tvBalance.setText("Balance: " + balance + " FUN");

                        // Δείχνουμε το αποτέλεσμα με ένα ωραίο Dialog αντί για Toast!
                        showErrorDialog("Αποτέλεσμα", resultMessage);
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    runOnUiThread(() -> {
                        balance += amount; // Επιστρέφουμε τα λεφτά αν υπήρξε σφάλμα
                        tvBalance.setText("Balance: " + balance + " FUN");
                        showErrorDialog("Σφάλμα", "Κάτι πήγε στραβά: " + errorMsg);
                    });
                }
            });
        });

        // Κουμπί Ακύρωσης
        builder.setNegativeButton("Ακύρωση", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // --- Βοηθητική μέθοδος για να δείχνουμε όμορφα τα σφάλματα/αποτελέσματα ---
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("ΟΚ", (dialog, which) -> dialog.dismiss())
                .show();
    }
}