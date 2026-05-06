package com.example.casinolalo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Παρακαλώ εισάγετε username", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish();
            }
        });
    }
}
