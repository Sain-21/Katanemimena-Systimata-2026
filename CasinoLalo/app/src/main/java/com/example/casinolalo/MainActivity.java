package com.example.casinolalo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvBalance, tvWelcomeUser;
    private Button btnViewAll, btnAdd100, btnAddCustom, btnLogout;
    private LinearLayout gamesContainer;
    private double balance = 0.0;
    private String username = "Guest";
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Σύνδεση UI
        tvBalance = findViewById(R.id.tvBalance);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser); // Πρόσθεσε ένα TextView στο XML για το όνομα
        btnViewAll = findViewById(R.id.btnViewAll);
        btnAdd100 = findViewById(R.id.btnAdd100Tokens);
        btnAddCustom = findViewById(R.id.btnAddCustomTokens);
        btnLogout = findViewById(R.id.btnLogout);
        gamesContainer = findViewById(R.id.gamesContainer);

        // Padding για notches/system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Αρχική εμφάνιση
        updateBalanceUI();
        showLoginDialog();

        // --- 1. Προσθήκη Tokens ---
        if (btnAdd100 != null) {
            btnAdd100.setOnClickListener(v -> {
                balance += 100;
                updateBalanceUI();
                Toast.makeText(this, "Προστέθηκαν 100 Tokens!", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnAddCustom != null) {
            btnAddCustom.setOnClickListener(v -> showAddTokensDialog());
        }

        // --- 2. Logout ---
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                saveUserBalance(); // Σιγουρευόμαστε ότι αποθηκεύτηκε πριν το logout
                username = "Guest";
                balance = 0.0;
                updateBalanceUI();
                gamesContainer.removeAllViews();
                showLoginDialog();
            });
        }

        // --- 3. Λήψη Παιχνιδιών ---
        btnViewAll.setOnClickListener(v -> refreshGamesList());

        // Αρχική φόρτωση
        refreshGamesList();

        // --- 4. Auto Refresh κάθε 5 δευτερόλεπτα για να πιάνουμε αλλαγές στο JSON ---
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshGamesList();
                refreshHandler.postDelayed(this, 5000); // 5 δευτερόλεπτα για γρήγορη απόκριση
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void refreshGamesList() {
        NetworkManager.fetchAllGames(new NetworkManager.GameListCallback() {
            @Override
            public void onSuccess(List<Game> games) {
                runOnUiThread(() -> {
                    gamesContainer.removeAllViews();
                    for (Game game : games) {
                        addGameToUI(game);
                    }
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> showErrorDialog("Σφάλμα Δικτύου", errorMsg));
            }
        });
    }

    // Μέθοδος για προσθήκη παιχνιδιού στο Container
    private void addGameToUI(Game game) {
        Button gameBtn = new Button(this);
        
        // Χρήση emoji αστεριού που έχει ενσωματωμένο περίγραμμα και προσθήκη shadow effect στο κουμπί
        String starsStr = "⭐".repeat(Math.max(0, (int)game.getStars()));
        String btnText = String.format("%s\nRisk: %s | Stars: %s", 
                game.getGameName(), game.getRiskLevel(), starsStr);
        
        gameBtn.setText(btnText);
        gameBtn.setAllCaps(false);
        gameBtn.setPadding(16, 16, 16, 16);
        
        // Στυλ για να ξεχωρίζουν στο άσπρο
        gameBtn.setTextColor(android.graphics.Color.BLACK);
        // Προσθήκη σκιών για να φαίνονται καλύτερα τα γράμματα και τα αστέρια
        gameBtn.setShadowLayer(1.5f, 1, 1, android.graphics.Color.LTGRAY);
        
        // Margin ανάμεσα στα παιχνίδια
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9), // 90% του πλάτους
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        gameBtn.setLayoutParams(params);
        
        gameBtn.setOnClickListener(view -> showPlayDialog(game));
        gamesContainer.addView(gameBtn);
    }

    // Ενημέρωση του UI για το υπόλοιπο (ΠΑΝΤΑ 2 δεκαδικά)
    private void updateBalanceUI() {
        tvBalance.setText(String.format(Locale.US, "Balance: %.2f", balance));
        // Μόνο αν δεν είναι Guest αποθηκεύουμε
        if (!"Guest".equals(username)) {
            saveUserBalance();
        }
    }

    private void saveUserBalance() {
        if (username != null && !username.equals("Guest")) {
            getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                    .edit()
                    .putFloat("balance_" + username, (float) balance)
                    .apply();
        }
    }

    private void loadUserBalance() {
        if (username != null && !username.equals("Guest")) {
            balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                    .getFloat("balance_" + username, 0.0f);
        } else {
            balance = 0.0;
        }
        updateBalanceUI();
    }

    private void showAddTokensDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Προσθήκη Tokens");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Ποσό (π.χ. 100)");
        builder.setView(input);

        builder.setPositiveButton("Προσθήκη", (dialog, which) -> {
            try {
                double amount = Double.parseDouble(input.getText().toString());
                balance += amount;
                updateBalanceUI();
            } catch (Exception e) {
                Toast.makeText(this, "Άκυρο ποσό", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Άκυρο", null);
        builder.show();
    }

    private void showPlayDialog(Game game) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Παιχνίδι: " + game.getGameName());
        builder.setMessage(String.format(Locale.US, "Όρια: %.2f - %.2f\nΥπόλοιπο: %.2f", 
                game.getMinBet(), game.getMaxBet(), balance));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Ποσό πονταρίσματος");
        builder.setView(input);

        builder.setPositiveButton("PLAY", (dialog, which) -> {
            String betStr = input.getText().toString();
            if (betStr.isEmpty()) return;

            double amount = Double.parseDouble(betStr);

            if (amount < game.getMinBet() || amount > game.getMaxBet()) {
                showErrorDialog("Λάθος Ποσό", "Εκτός ορίων πονταρίσματος.");
                return;
            }
            if (amount > balance) {
                showErrorDialog("Ανεπαρκές Υπόλοιπο", "Δεν έχετε αρκετά tokens.");
                return;
            }

            balance -= amount;
            updateBalanceUI();

            // Χρήση του δυναμικού username
            NetworkManager.playGame(username, game.getGameName(), amount, new NetworkManager.PlayCallback() {
                @Override
                public void onResult(String resultMessage, double payout) {
                    runOnUiThread(() -> {
                        balance += payout;
                        updateBalanceUI();
                        
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Αποτέλεσμα")
                                .setMessage(resultMessage)
                                .setPositiveButton("ΟΚ", (d, w) -> {
                                    // Έλεγχος αν ο παίκτης έχει ήδη βαθμολογήσει το παιχνίδι
                                    if (!game.hasVoted(username)) {
                                        showRatingDialog(game);
                                    }
                                })
                                .show();
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    runOnUiThread(() -> {
                        balance += amount; // Επιστροφή χρημάτων σε σφάλμα δικτύου
                        updateBalanceUI();
                        showErrorDialog("Σφάλμα", errorMsg);
                    });
                }
            });
        });
        builder.setNegativeButton("Άκυρο", null);
        builder.show();
    }

    private void showRatingDialog(Game game) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Βαθμολόγησε το " + game.getGameName());
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Από 1 έως 5 αστέρια");
        builder.setView(input);

        builder.setPositiveButton("Υποβολή", (dialog, which) -> {
            try {
                int stars = Integer.parseInt(input.getText().toString());
                if (stars >= 1 && stars <= 5) {
                    // Αποστολή στο backend
                    NetworkManager.rateGame(username, game.getGameName(), stars);
                    
                    // Ανανέωση ολόκληρης της λίστας αφού υπήρξε αλλαγή στη βαθμολογία
                    refreshGamesList();
                }
            } catch (Exception e) { /* ignore */ }
        });
        builder.setNegativeButton("Παράλειψη", null);
        builder.show();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Είσοδος στο Casino");
        builder.setCancelable(false);
        final EditText input = new EditText(this);
        input.setHint("Username");
        builder.setView(input);

        builder.setPositiveButton("Είσοδος", (dialog, which) -> {
            username = input.getText().toString().trim();
            if (username.isEmpty()) username = "Guest";
            if (tvWelcomeUser != null) tvWelcomeUser.setText("Γεια σου, " + username + "!");
            loadUserBalance(); // Φόρτωση του υπολοίπου για τον συγκεκριμένο χρήστη
            Toast.makeText(this, "Καλώς ήρθες " + username, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }
}