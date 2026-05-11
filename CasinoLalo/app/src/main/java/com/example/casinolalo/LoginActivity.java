package com.example.casinolalo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity
{
    private EditText etUsername;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);

        //log in button
        btnLogin.setOnClickListener(v ->
        {
            String username = etUsername.getText().toString().trim();

            //check if text is empty
            if (username.isEmpty())
            {
                Toast.makeText(this, "Παρακαλώ εισάγετε username", Toast.LENGTH_SHORT).show();
            }
            else
            {
                //go to main menu
                Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
                intent.putExtra("USERNAME", username);//send username to next screen
                startActivity(intent);
                finish();
            }
        });
    }
}
