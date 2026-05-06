package com.example.casinolalo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.casinolalo.network.NetworkManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;

public class GameDetailActivity extends AppCompatActivity {
    private ImageView ivIcon;
    private TextView tvName, tvStats, tvLimits, tvBalance;
    private EditText etBet;
    private Button btnPlay, btnExit, btnRate;
    private CheckBox cbAutoPlay;
    private LinearLayout historyContainer;
    
    private String username;
    private String gameName;
    private double balance;
    private double minBet, maxBet;
    
    private boolean isPlaying = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoPlayRunnable = this::handlePlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        username = getIntent().getStringExtra("USERNAME");
        gameName = getIntent().getStringExtra("GAME_NAME");
        minBet = getIntent().getDoubleExtra("MIN_BET", 0.0);
        maxBet = getIntent().getDoubleExtra("MAX_BET", 0.0);
        double stars = getIntent().getFloatExtra("STARS", 0.0f);
        String risk = getIntent().getStringExtra("RISK");

        ivIcon = findViewById(R.id.ivGameDetailIcon);
        tvName = findViewById(R.id.tvGameDetailName);
        tvStats = findViewById(R.id.tvGameDetailStats);
        tvLimits = findViewById(R.id.tvGameDetailLimits);
        tvBalance = findViewById(R.id.tvDetailBalance);
        etBet = findViewById(R.id.etBetAmount);
        btnPlay = findViewById(R.id.btnPlayAction);
        btnExit = findViewById(R.id.btnBackToGames);
        btnRate = findViewById(R.id.btnRateGame);
        cbAutoPlay = findViewById(R.id.cbAutoPlay);
        historyContainer = findViewById(R.id.historyContainer);

        tvName.setText(gameName);
        String starsStr = "⭐".repeat(Math.max(0, (int)stars));
        tvStats.setText(String.format("%s | %s", risk, starsStr));
        tvLimits.setText(String.format(Locale.US, "Limits: %.2f - %.2f", minBet, maxBet));

        // Dynamically set image
        int resId = getResources().getIdentifier(
            gameName.toLowerCase().replace(" ", "_"), 
            "drawable", getPackageName()
        );
        if (resId != 0) {
            ivIcon.setImageResource(resId);
        }

        loadBalance();
        updateBalanceUI();
        loadHistory();

        btnPlay.setOnClickListener(v -> handlePlay());
        btnExit.setOnClickListener(v -> {
            stopAutoPlay();
            finish();
        });
        btnRate.setOnClickListener(v -> showRatingDialog());

        cbAutoPlay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnPlay.setEnabled(!isPlaying); 
            } else {
                stopAutoPlay();
                btnPlay.setEnabled(!isPlaying);
                btnPlay.setAlpha(isPlaying ? 0.5f : 1.0f);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoPlay();
    }

    private void loadBalance() {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .getFloat("balance_" + username, 0.0f);
    }

    private void updateBalanceUI() {
        tvBalance.setText(String.format(Locale.US, "Credits: %.2f", balance));
    }

    private void handlePlay() {
        if (isPlaying) return;
        
        String betStr = etBet.getText().toString();
        if (betStr.isEmpty()) {
            Toast.makeText(this, "Enter bet amount", Toast.LENGTH_SHORT).show();
            cbAutoPlay.setChecked(false);
            return;
        }

        try {
            double amount = Double.parseDouble(betStr);

            if (amount < minBet || amount > maxBet) {
                Toast.makeText(this, "Bet out of limits", Toast.LENGTH_SHORT).show();
                cbAutoPlay.setChecked(false);
                return;
            }
            if (amount > balance) {
                Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show();
                cbAutoPlay.setChecked(false);
                return;
            }

            isPlaying = true;
            btnPlay.setEnabled(false);
            btnPlay.setAlpha(0.5f);

            balance -= amount;
            updateBalanceUI();
            saveBalance();

            NetworkManager.playGame(username, gameName, amount, new NetworkManager.PlayCallback() {
                @Override
                public void onResult(String resultMessage, double payout) {
                    runOnUiThread(() -> {
                        balance += payout;
                        updateBalanceUI();
                        saveBalance();
                        
                        addToHistory(resultMessage, payout > 0);
                        
                        isPlaying = false;
                        if (!cbAutoPlay.isChecked()) {
                            mainHandler.postDelayed(() -> {
                                if (!cbAutoPlay.isChecked()) {
                                    btnPlay.setEnabled(true);
                                    btnPlay.setAlpha(1.0f);
                                }
                            }, 500);
                        } else {
                            mainHandler.postDelayed(autoPlayRunnable, 1000);
                        }
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    runOnUiThread(() -> {
                        balance += amount; 
                        updateBalanceUI();
                        saveBalance();
                        Toast.makeText(GameDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        
                        isPlaying = false;
                        cbAutoPlay.setChecked(false);
                        btnPlay.setEnabled(true);
                        btnPlay.setAlpha(1.0f);
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            cbAutoPlay.setChecked(false);
        }
    }

    private void stopAutoPlay() {
        mainHandler.removeCallbacks(autoPlayRunnable);
    }

    private void addToHistory(String message, boolean isWin) {
        TextView historyItem = createHistoryTextView(message, isWin);
        historyContainer.addView(historyItem, 0);
        
        if (historyContainer.getChildCount() > 50) {
            historyContainer.removeViewAt(historyContainer.getChildCount() - 1);
        }
        saveHistory();
    }

    private TextView createHistoryTextView(String message, boolean isWin) {
        TextView historyItem = new TextView(this);
        String fixedMsg = fixDecimalInMessage(message);
        historyItem.setText(fixedMsg);
        historyItem.setTextColor(isWin ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        historyItem.setTextSize(14);
        historyItem.setPadding(8, 4, 8, 4);
        historyItem.setGravity(Gravity.CENTER);
        return historyItem;
    }

    private String fixDecimalInMessage(String msg) {
        return msg.replaceAll("(\\d+\\.\\d{3,})", "$1").replaceAll("(\\d+\\.\\d\\d)\\d+", "$1");
    }

    private void saveBalance() {
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .edit()
                .putFloat("balance_" + username, (float) balance)
                .apply();
    }

    private void saveHistory() {
        JSONArray historyArray = new JSONArray();
        for (int i = 0; i < historyContainer.getChildCount(); i++) {
            TextView tv = (TextView) historyContainer.getChildAt(i);
            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("text", tv.getText().toString());
                obj.put("color", tv.getCurrentTextColor());
                historyArray.put(obj);
            } catch (JSONException e) { e.printStackTrace(); }
        }
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .edit()
                .putString("history_" + username + "_" + gameName, historyArray.toString())
                .apply();
    }

    private void loadHistory() {
        String historyStr = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .getString("history_" + username + "_" + gameName, "[]");
        try {
            JSONArray historyArray = new JSONArray(historyStr);
            for (int i = historyArray.length() - 1; i >= 0; i--) {
                org.json.JSONObject obj = historyArray.getJSONObject(i);
                TextView tv = new TextView(this);
                tv.setText(obj.getString("text"));
                tv.setTextColor(obj.getInt("color"));
                tv.setTextSize(14);
                tv.setPadding(8, 4, 8, 4);
                tv.setGravity(Gravity.CENTER);
                historyContainer.addView(tv, 0);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate " + gameName);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Rate 1 to 5 stars");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            try {
                int stars = Integer.parseInt(input.getText().toString());
                if (stars >= 1 && stars <= 5) {
                    NetworkManager.rateGame(username, gameName, stars);
                    Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter 1-5 stars", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
