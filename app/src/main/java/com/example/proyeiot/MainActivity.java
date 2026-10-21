package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnGoRegister).setOnClickListener(this::pgregister);
        findViewById(R.id.btnRegister2).setOnClickListener(this::sesiongoogle);
        findViewById(R.id.btnLogin).setOnClickListener(this::login);
    }


    void sesiongoogle(View v) {

        OAuthProvider.Builder provider = OAuthProvider.newBuilder("google.com");

        FirebaseAuth.getInstance()
                .startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    void pgregister (View v) {
        startActivity(new Intent(MainActivity.this, RegisterActivity.class));
    }
    void login (View v) {
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }

        });

    }
}