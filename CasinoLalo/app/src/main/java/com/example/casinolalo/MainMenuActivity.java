package com.example.casinolalo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.text.InputType;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainMenuActivity extends AppCompatActivity {
    private TextView tvWelcome, tvBalance;
    private Button btnPlay, btnAdd100, btnAddCustom, btnLogout;
    private String username;
    private double balance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        username = getIntent().getStringExtra("USERNAME");
        if (username == null) username = "Guest";

        tvWelcome = findViewById(R.id.tvWelcomeUserMenu);
        tvBalance = findViewById(R.id.tvBalanceMenu);
        btnPlay = findViewById(R.id.btnPlayGames);
        btnAdd100 = findViewById(R.id.btnAdd100Menu);
        btnAddCustom = findViewById(R.id.btnAddCustomMenu);
        btnLogout = findViewById(R.id.btnLogoutMenu);

        tvWelcome.setText("Γεια σου, " + username + "!");
        loadBalance();

        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        btnAdd100.setOnClickListener(v -> {
            balance += 100;
            saveAndUpdateUI();
            Toast.makeText(this, "Προστέθηκαν 100 Tokens!", Toast.LENGTH_SHORT).show();
        });

        btnAddCustom.setOnClickListener(v -> showAddTokensDialog());

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance(); // Reload in case balance changed in MainActivity
    }

    private void loadBalance() {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .getFloat("balance_" + username, 0.0f);
        updateBalanceUI();
    }

    private void saveAndUpdateUI() {
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .edit()
                .putFloat("balance_" + username, (float) balance)
                .apply();
        updateBalanceUI();
    }

    private void updateBalanceUI() {
        tvBalance.setText(String.format(Locale.US, "Balance: %.2f", balance));
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
                saveAndUpdateUI();
            } catch (Exception e) {
                Toast.makeText(this, "Άκυρο ποσό", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Άκυρο", null);
        builder.show();
    }
}
