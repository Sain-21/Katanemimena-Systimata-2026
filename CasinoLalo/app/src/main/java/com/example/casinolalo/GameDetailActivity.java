package com.example.casinolalo;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.casinolalo.network.NetworkManager;
import com.example.casinolalo.ui.DiceSliderView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;
import java.util.Random;

public class GameDetailActivity extends AppCompatActivity
{
    //game image
    private ImageView ivIcon;

    private DiceSliderView diceSlider;
    private TextView tvName, tvStats, tvLimits, tvBalance;
    private EditText etBet;
    private Button btnPlay, btnExit, btnRate, btnDouble;
    private CheckBox cbAutoPlay;
    private LinearLayout historyContainer;
    
    private String username;
    private String gameName;
    private String riskLevel;
    private double balance;
    private double minBet, maxBet;
    
    private boolean isPlaying = false;
    
    //handler for auto-play timing
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    //random generator for optical result of play
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        //get info from previous screen
        username = getIntent().getStringExtra("USERNAME");
        gameName = getIntent().getStringExtra("GAME_NAME");
        minBet = getIntent().getDoubleExtra("MIN_BET", 0.0);
        maxBet = getIntent().getDoubleExtra("MAX_BET", 0.0);
        double stars = getIntent().getFloatExtra("STARS", 0.0f);
        riskLevel = getIntent().getStringExtra("RISK");

        diceSlider = findViewById(R.id.diceSlider);
        ivIcon = findViewById(R.id.ivGameDetailIcon);
        tvName = findViewById(R.id.tvGameDetailName);
        tvStats = findViewById(R.id.tvGameDetailStats);
        tvLimits = findViewById(R.id.tvGameDetailLimits);
        tvBalance = findViewById(R.id.tvDetailBalance);
        etBet = findViewById(R.id.etBetAmount);
        btnPlay = findViewById(R.id.btnPlayAction);
        btnExit = findViewById(R.id.btnBackToGames);
        btnRate = findViewById(R.id.btnRateGame);
        btnDouble = findViewById(R.id.btnDoubleBet);
        cbAutoPlay = findViewById(R.id.cbAutoPlay);
        historyContainer = findViewById(R.id.historyContainer);

        //x2 button
        btnDouble.setOnClickListener(v ->
        {
            try
            {
                String currentBetStr = etBet.getText().toString();
                if (currentBetStr.isEmpty()) return;
                double currentBet = Double.parseDouble(currentBetStr);
                double doubledBet = currentBet * 2;
                
                //update of bet
                etBet.setText(String.format(Locale.US, "%.2f", doubledBet));
            }
            catch (Exception e) {}
        });

        //dynamic check for x2 btn
        etBet.addTextChangedListener(new android.text.TextWatcher()
        {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s)
            {
                checkDoubleButtonState();
            }
        });

        // load image of game
        int resId = getResources().getIdentifier(
                gameName.toLowerCase().replace(" ", "_"),
                "drawable", getPackageName()
        );
        if (resId != 0)
        {
            ivIcon.setImageResource(resId);
        }
        else
        {
            ivIcon.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Ρύθμιση κειμένων
        tvName.setText(gameName);
        String starsStr = "⭐".repeat(Math.max(0, (int)stars));
        tvStats.setText(String.format("%s | %s", riskLevel, starsStr));
        tvLimits.setText(String.format(Locale.US, "Limits: %.2f - %.2f", minBet, maxBet));

        //calc win propability based on risk lvl
        float winChance = 0.5f;
        if ("low".equalsIgnoreCase(riskLevel)) winChance = 0.70f;
        else if ("medium".equalsIgnoreCase(riskLevel)) winChance = 0.40f;
        else if ("high".equalsIgnoreCase(riskLevel)) winChance = 0.10f;
        diceSlider.setWinChance(winChance);

        // load data
        loadBalance();
        updateBalanceUI();
        loadHistory();
        checkDoubleButtonState();

        //play btn logic
        btnPlay.setOnClickListener(v ->
        {
            if (cbAutoPlay.isChecked() && isPlaying)
            {
                stopAutoPlay();
            }
            else
            {
                handlePlay();
            }
        });

        // exit
        btnExit.setOnClickListener(v ->
        {
            stopAutoPlay();
            finish();
        });

        // rating dialog
        btnRate.setOnClickListener(v -> showRatingDialog());

        //autoplay open/close listen
        cbAutoPlay.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if (!isChecked)
            {
                stopAutoPlay();
            }
            if (!isPlaying)
            {
                btnPlay.setText("PLAY NOW");
                btnPlay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopAutoPlay();
    }


    //check if x2 btn should be open
    //isOpen otan to bet * 2 < betLimit || bet * < balance
    private void checkDoubleButtonState()
    {
        if (isPlaying)
        {
            btnDouble.setEnabled(false);
            btnDouble.setAlpha(0.5f);
            return;
        }

        try
        {
            String betStr = etBet.getText().toString();
            if (betStr.isEmpty())
            {
                btnDouble.setEnabled(false);
                btnDouble.setAlpha(0.5f);
                return;
            }

            double currentBet = Double.parseDouble(betStr);
            double doubledBet = currentBet * 2;

            boolean canDouble = (doubledBet >= minBet && doubledBet <= maxBet && doubledBet <= balance);

            btnDouble.setEnabled(canDouble);
            btnDouble.setAlpha(canDouble ? 1.0f : 0.5f);
        }
        catch (Exception e)
        {
            btnDouble.setEnabled(false);
            btnDouble.setAlpha(0.5f);
        }
    }

    //check bet, remove bet from balance and call network
    private void handlePlay()
    {
        if (isPlaying && !cbAutoPlay.isChecked()) return;
        
        String betStr = etBet.getText().toString();
        if (betStr.isEmpty())
        {
            Toast.makeText(this, "Enter bet amount", Toast.LENGTH_SHORT).show();
            cbAutoPlay.setChecked(false);
            return;
        }

        try
        {
            double amount = Double.parseDouble(betStr);
            if (amount < minBet || amount > maxBet || amount > balance)
            {
                Toast.makeText(this, "Invalid bet or funds", Toast.LENGTH_SHORT).show();
                stopAutoPlay();
                return;
            }

            isPlaying = true;
            checkDoubleButtonState(); //x2 disable

            if (cbAutoPlay.isChecked())
            {
                btnPlay.setText("STOP AUTO");
                btnPlay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            }
            else
            {
                btnPlay.setEnabled(false);
                btnPlay.setAlpha(0.5f);
            }

            // remove bet
            balance -= amount;
            updateBalanceUI();
            saveBalance();

            // server call
            NetworkManager.playGame(username, gameName, amount, new NetworkManager.PlayCallback()
            {
                @Override
                public void onResult(String resultMessage, double payout)
                {
                    runOnUiThread(() ->
                    {
                        //calc dice slider location
                        float finalValue;
                        float winChance = 0.5f;
                        if ("low".equalsIgnoreCase(riskLevel)) winChance = 0.70f;
                        else if ("medium".equalsIgnoreCase(riskLevel)) winChance = 0.40f;
                        else if ("high".equalsIgnoreCase(riskLevel)) winChance = 0.10f;

                        if (payout > 0)
                        {
                            //win dice go to green space
                            finalValue = random.nextFloat() * winChance;
                        }
                        else
                        {
                            //loss dice go to red space
                            finalValue = winChance + (random.nextFloat() * (1f - winChance));
                        }

                        animateDice(finalValue, resultMessage, payout);
                    });
                }

                @Override
                public void onError(String errorMsg)
                {
                    runOnUiThread(() ->
                    {
                        balance += amount;// return bet onError
                        updateBalanceUI();
                        saveBalance();
                        Toast.makeText(GameDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        stopAutoPlay();
                    });
                }
            });

        }
        catch (Exception e)
        {
            stopAutoPlay();
        }
    }

    //move slider to final pos
    private void animateDice(float targetValue, String resultMessage, double payout)
    {
        float startValue = diceSlider.getCurrentValue();
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, targetValue);
        animator.setDuration(400); // speed
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> diceSlider.setCurrentValue((float) animation.getAnimatedValue()));
        
        animator.addListener(new android.animation.AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(android.animation.Animator animation)
            {
                // update balance after animation
                balance += payout;
                updateBalanceUI();
                saveBalance();

                addToHistory(resultMessage, payout > 0);

                if (cbAutoPlay.isChecked())
                {
                    mainHandler.postDelayed(() ->
                    {
                        if (cbAutoPlay.isChecked())
                        {
                            handlePlay();
                        }
                        else
                        {
                            resetPlayState();
                        }
                    }, 600);
                }
                else
                {
                    resetPlayState();
                }
                
                checkDoubleButtonState();
            }
        });
        animator.start();
    }

    private void resetPlayState()
    {
        isPlaying = false;
        btnPlay.setEnabled(true);
        btnPlay.setAlpha(1.0f);
        btnPlay.setText("PLAY NOW");
        btnPlay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
    }

    private void stopAutoPlay()
    {
        cbAutoPlay.setChecked(false);
        mainHandler.removeCallbacksAndMessages(null);
        resetPlayState();
    }

    private void addToHistory(String message, boolean isWin)
    {
        TextView historyItem = new TextView(this);
        historyItem.setText(fixDecimalInMessage(message));

        if (message.toLowerCase().contains("επιστροφή"))
        {
            historyItem.setTextColor(Color.parseColor("#FFD700"));
        }
        else
        {
            historyItem.setTextColor(isWin ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
        historyItem.setTextSize(14);
        historyItem.setPadding(8, 4, 8, 4);
        historyItem.setGravity(Gravity.CENTER);
        
        historyContainer.addView(historyItem, 0);
        if (historyContainer.getChildCount() > 50) historyContainer.removeViewAt(50);
        saveHistory();
    }


    private String fixDecimalInMessage(String msg)
    {
        return msg.replaceAll("(\\d+\\.\\d{3,})", "$1").replaceAll("(\\d+\\.\\d\\d)\\d+", "$1");
    }

    private void updateBalanceUI()
    {
        tvBalance.setText(String.format(Locale.US, "Credits: %.2f", balance));
    }

    private void loadBalance()
    {
        balance = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).getFloat("balance_" + username, 0.0f);
    }

    private void saveBalance()
    {
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit().putFloat("balance_" + username, (float) balance).apply();
    }

    private void saveHistory()
    {
        JSONArray historyArray = new JSONArray();
        for (int i = 0; i < historyContainer.getChildCount(); i++)
        {
            TextView tv = (TextView) historyContainer.getChildAt(i);
            try
            {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("text", tv.getText().toString());
                obj.put("color", tv.getCurrentTextColor());
                historyArray.put(obj);
            }
            catch (JSONException e) {}
        }
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit().putString("history_" + username + "_" + gameName, historyArray.toString()).apply();
    }

    private void loadHistory()
    {
        String historyStr = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).getString("history_" + username + "_" + gameName, "[]");
        try
        {
            JSONArray historyArray = new JSONArray(historyStr);
            for (int i = historyArray.length() - 1; i >= 0; i--)
            {
                org.json.JSONObject obj = historyArray.getJSONObject(i);
                TextView tv = new TextView(this);
                tv.setText(obj.getString("text"));
                tv.setTextColor(obj.getInt("color"));
                tv.setTextSize(14);
                tv.setPadding(8, 4, 8, 4);
                tv.setGravity(Gravity.CENTER);
                historyContainer.addView(tv, 0);
            }
        }
        catch (JSONException e) {}
    }

    private void showRatingDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate " + gameName);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Rate 1 to 5 stars");
        builder.setView(input);
        builder.setPositiveButton("Submit", (dialog, which) ->
        {
            try
            {
                int stars = Integer.parseInt(input.getText().toString());
                if (stars >= 1 && stars <= 5)
                {
                    NetworkManager.rateGame(username, gameName, stars);
                    Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e) {}
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
