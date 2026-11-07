package com.example.proyeiot;

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
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        findViewById(R.id.btnRegister).setOnClickListener(this::registrar);
        findViewById(R.id.btnGoLogin).setOnClickListener(v -> {
            finish();   // vuelve al login, que está debajo en la pila
            overridePendingTransition(0, 0);
        });
    }

    void registrar(View v) {

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etUser = findViewById(R.id.etUser);

        String user = etUser.getText().toString().trim();

        if (!etEmail.getText().toString().trim().isEmpty()
                && !etPassword.getText().toString().trim().isEmpty()) {
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(etEmail.getText().toString(), etPassword.getText().toString())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser nuevo = FirebaseAuth.getInstance().getCurrentUser();

                            UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(user)
                                    .build();

                            nuevo.updateProfile(perfil).addOnCompleteListener(t1 ->
                                    nuevo.sendEmailVerification().addOnCompleteListener(t2 -> {
                                        if (t2.isSuccessful()) {
                                            Toast.makeText(this,
                                                    "Cuenta creada. Revisa tu correo para verificarla",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(this,
                                                    "Cuenta creada, pero no se pudo enviar el correo",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        FirebaseAuth.getInstance().signOut();
                                        finish();
                                        overridePendingTransition(0, 0);
                                    }));

                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}