package com.example.casinolalo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.casinolalo.network.NetworkManager;
import com.aueb.shared.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Η οθόνη που εμφανίζει τη λίστα των διαθέσιμων παιχνιδιών.
 * Περιλαμβάνει Φίλτρα (Αναζήτηση, Ρίσκο, Rating) και αυτόματη ανανέωση.
 */
@SuppressLint("DiscouragedApi")
public class MainActivity extends AppCompatActivity {

    private TextView tvBalance, tvWelcomeUser;
    private Button btnBack;
    private GridLayout gamesGrid;
    private EditText etSearch;
    private Spinner spinnerRisk, spinnerRating;
    
    private double balance = 0.0;
    private String username = "Guest";
    private List<Game> allGames = new ArrayList<>(); // Όλα τα παιχνίδια από τον server
    
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        username = getIntent().getStringExtra("USERNAME");
        if (username == null) username = "Guest";

        tvBalance = findViewById(R.id.tvBalance);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnBack = findViewById(R.id.btnBack);
        gamesGrid = findViewById(R.id.gamesGrid);
        etSearch = findViewById(R.id.etSearchGames);
        spinnerRisk = findViewById(R.id.spinnerRiskFilter);
        spinnerRating = findViewById(R.id.spinnerRatingFilter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (tvWelcomeUser != null) {
            tvWelcomeUser.setText("Player: " + username);
        }

        loadUserBalance();
        setupFilters();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        refreshGamesList();

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshGamesList();
                refreshHandler.postDelayed(this, 10000); // 10s
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 10000);
    }

    /**
     * Ρύθμιση των Spinners και των Listeners για τα φίλτρα.
     */
    private void setupFilters() {
        String[] riskOptions = {"All Risk", "Low", "Medium", "High"};
        ArrayAdapter<String> riskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, riskOptions);
        spinnerRisk.setAdapter(riskAdapter);

        String[] ratingOptions = {"All Ratings", "5 Stars", "4+ Stars", "3+ Stars"};
        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ratingOptions);
        spinnerRating.setAdapter(ratingAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerRisk.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerRating.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Εφαρμογή των επιλεγμένων φίλτρων στη λίστα των παιχνιδιών.
     */
    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase();
        String selectedRisk = spinnerRisk.getSelectedItem().toString();
        int minStars = 0;
        if (spinnerRating.getSelectedItemPosition() == 1) minStars = 5;
        else if (spinnerRating.getSelectedItemPosition() == 2) minStars = 4;
        else if (spinnerRating.getSelectedItemPosition() == 3) minStars = 3;

        List<Game> filteredList = new ArrayList<>();
        for (Game game : allGames) {
            boolean matchesName = game.getGameName().toLowerCase().contains(query);
            boolean matchesRisk = selectedRisk.equals("All Risk") || game.getRiskLevel().equalsIgnoreCase(selectedRisk);
            boolean matchesRating = game.getStars() >= minStars;

            if (matchesName && matchesRisk && matchesRating) {
                filteredList.add(game);
            }
        }
        updateUIWithGames(filteredList);
    }

    private void refreshGamesList() {
        NetworkManager.fetchAllGames(new NetworkManager.GameListCallback() {
            @Override
            public void onSuccess(List<Game> games) {
                allGames = games;
                runOnUiThread(() -> applyFilters());
            }
            @Override public void onError(String errorMsg) {}
        });
    }

    private void updateUIWithGames(List<Game> games) {
        gamesGrid.removeAllViews();
        for (Game game : games) {
            addGameToUI(game);
        }
    }

    private void addGameToUI(Game game) {
        View gameView = LayoutInflater.from(this).inflate(R.layout.game_item, gamesGrid, false);
        TextView title = gameView.findViewById(R.id.gameTitle);
        TextView info = gameView.findViewById(R.id.gameInfo);
        ImageView icon = gameView.findViewById(R.id.gameIcon);
        
        title.setText(game.getGameName());
        String starsStr = "⭐".repeat(Math.max(0, (int)game.getStars()));
        info.setText(String.format("%s | %s", game.getRiskLevel(), starsStr));
        
        int resId = getResources().getIdentifier(
            game.getGameName().toLowerCase().replace(" ", "_"), 
            "drawable", getPackageName()
        );
        if (resId != 0) icon.setImageResource(resId);
        else icon.setImageResource(R.drawable.ic_launcher_foreground);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        // Orismos stilis me varos (weight) 1 gia na moirazetai o xoros akrivos sti mesi
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        gameView.setLayoutParams(params);

        gameView.setOnClickListener(view -> {
            Intent intent = new Intent(this, GameDetailActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("GAME_NAME", game.getGameName());
            intent.putExtra("MIN_BET", game.getMinBet());
            intent.putExtra("MAX_BET", game.getMaxBet());
            intent.putExtra("RISK", game.getRiskLevel());
            intent.putExtra("STARS", (float)game.getStars());
            startActivity(intent);
        });
        gamesGrid.addView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserBalance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void updateBalanceUI() {
        if (tvBalance != null) {
            tvBalance.setText(String.format(Locale.US, "Credits: %.2f", balance));
        }
    }

    private void loadUserBalance() {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).getFloat("balance_" + username, 0.0f);
        updateBalanceUI();
    }
}
