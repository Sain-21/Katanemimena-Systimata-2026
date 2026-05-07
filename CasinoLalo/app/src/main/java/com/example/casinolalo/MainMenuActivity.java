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

public class MainMenuActivity extends AppCompatActivity
{
    private TextView tvWelcome, tvBalance;
    private Button btnPlay, btnAdd100, btnAddCustom, btnLogout;
    private String username;
    private double balance;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // load username from intent of LoginActivity
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) username = "Guest";

        tvWelcome = findViewById(R.id.tvWelcomeUserMenu);
        tvBalance = findViewById(R.id.tvBalanceMenu);
        btnPlay = findViewById(R.id.btnPlayGames);
        btnAdd100 = findViewById(R.id.btnAdd100Menu);
        btnAddCustom = findViewById(R.id.btnAddCustomMenu);
        btnLogout = findViewById(R.id.btnLogoutMenu);

        // print welcome
        tvWelcome.setText("Γεια σου, " + username + "!");

        loadBalance();

        //play button
        btnPlay.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
        });

        //add 100
        btnAdd100.setOnClickListener(v ->
        {
            balance += 100;
            saveAndUpdateUI();
            Toast.makeText(this, "Προστέθηκαν 100 στο Balance!", Toast.LENGTH_SHORT).show();
        });

        //add custom
        btnAddCustom.setOnClickListener(v -> showAddTokensDialog());

        //logout
        btnLogout.setOnClickListener(v ->
        {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadBalance(); 
    }

    //load balance
    private void loadBalance()
    {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .getFloat("balance_" + username, 0.0f);
        updateBalanceUI();
    }

    //save new balance
    private void saveAndUpdateUI()
    {
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .edit()
                .putFloat("balance_" + username, (float) balance)
                .apply();
        updateBalanceUI();
    }

    private void updateBalanceUI()
    {
        tvBalance.setText(String.format(Locale.US, "Balance: %.2f", balance));
    }

    //pop up dialog for custom balance amount to add
    private void showAddTokensDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Προσθήκη Balance");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Ποσό (π.χ. 100)");
        builder.setView(input);

        builder.setPositiveButton("Προσθήκη", (dialog, which) ->
        {
            try
            {
                double amount = Double.parseDouble(input.getText().toString());
                balance += amount;
                saveAndUpdateUI();
            }
            catch (Exception e)
            {
                Toast.makeText(this, "Άκυρο ποσό", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Άκυρο", null);
        builder.show();
    }
}
