package com.example.casinolalo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.casinolalo.network.NetworkManager;
import com.aueb.shared.Game;

import java.util.List;
import java.util.Locale;

@SuppressLint("DiscouragedApi")

public class MainActivity extends AppCompatActivity
{
    private TextView tvBalance, tvWelcomeUser;
    private Button btnBack;
    private GridLayout gamesGrid;
    private double balance = 0.0;
    private String username = "Guest";
    
    // handler gia to autorefresh
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    
    // execute list refresh
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // get username
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) username = "Guest";

        tvBalance = findViewById(R.id.tvBalance);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnBack = findViewById(R.id.btnBack);
        gamesGrid = findViewById(R.id.gamesGrid);

        // padding settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (tvWelcomeUser != null)
        {
            tvWelcomeUser.setText("Player: " + username);
        }

        loadUserBalance();

        if (btnBack != null)
        {
            btnBack.setOnClickListener(v -> finish());
        }

        //load games
        refreshGamesList();

        // refresh every 5 secs
        refreshRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                refreshGamesList();
                refreshHandler.postDelayed(this, 5000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    //load balance
    @Override
    protected void onResume()
    {
        super.onResume();
        loadUserBalance();
    }

    //stop refresh when screen is off
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (refreshHandler != null && refreshRunnable != null)
        {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    //communication with network manager for game list
    private void refreshGamesList()
    {
        NetworkManager.fetchAllGames(new NetworkManager.GameListCallback()
        {
            @Override
            public void onSuccess(List<Game> games)
            {
                runOnUiThread(() -> {
                    if (gamesGrid != null) {
                        gamesGrid.removeAllViews();//clean old list
                        for (Game game : games) {
                            addGameToUI(game);//add games to ui
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMsg)
            {
                // network error
            }
        });
    }

    //dimiourgia view card gia ena game kai add sto grid
    private void addGameToUI(Game game)
    {
        //fortosi layout gia kathe game_item
        View gameView = LayoutInflater.from(this).inflate(R.layout.game_item, gamesGrid, false);
        TextView title = gameView.findViewById(R.id.gameTitle);
        ImageView icon = gameView.findViewById(R.id.gameIcon);
        
        title.setText(game.getGameName());


        //dynamic load game image based on game name
        int resId = getResources().getIdentifier(
            game.getGameName().toLowerCase().replace(" ", "_"), 
            "drawable", getPackageName()
        );
        
        if (resId != 0)
        {
            icon.setImageResource(resId);
        }
        else
        {
            icon.setImageResource(R.drawable.ic_launcher_foreground); // placeholder an den yparxei eikona
        }
        
        // grid settings (2 stiles)
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        gameView.setLayoutParams(params);

        // when user selects a game, redirect to GameDetailActivity
        gameView.setOnClickListener(view ->
        {
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

    //balance update
    private void updateBalanceUI()
    {
        if (tvBalance != null)
        {
            tvBalance.setText(String.format(Locale.US, "Balance: %.2f", balance));
        }
    }

    //load balance from local file SharedPreferences
    private void loadUserBalance()
    {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .getFloat("balance_" + username, 0.0f);
        updateBalanceUI();
    }
}
