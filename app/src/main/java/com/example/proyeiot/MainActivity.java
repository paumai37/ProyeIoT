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
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnGoRegister).setOnClickListener(this::pgregister);
        findViewById(R.id.btnRegister2).setOnClickListener(this::sesiongoogle);
        findViewById(R.id.btnLogin).setOnClickListener(this::login);
        findViewById(R.id.btnRecu).setOnClickListener(this::unknownpassword);
    }


    void sesiongoogle(View v) {

        OAuthProvider.Builder provider = OAuthProvider.newBuilder("google.com");
        provider.setScopes(Arrays.asList("email", "profile"));   // pide el nombre y el email

        FirebaseAuth.getInstance()
                .startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();

                    // El nombre que manda Google en el login
                    String nombre = null;
                    if (authResult.getAdditionalUserInfo() != null
                            && authResult.getAdditionalUserInfo().getProfile() != null) {
                        Object n = authResult.getAdditionalUserInfo().getProfile().get("name");
                        if (n != null) nombre = n.toString();
                    }

                    if (user != null && nombre != null && !nombre.isEmpty()) {
                        user.updateProfile(new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nombre)
                                        .build())
                                .addOnCompleteListener(t -> irAlMapa());   // esperar a que se guarde
                    } else {
                        irAlMapa();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    void irAlMapa() {
        startActivity(new Intent(MainActivity.this, MapActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    void pgregister (View v) {
        startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        overridePendingTransition(0, 0);
    }

    void unknownpassword (View v) {
        EditText etEmail = findViewById(R.id.etEmail);
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Escribe tu email");
            return;
        }

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Te hemos enviado un correo para restablecer la contraseña",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
            if (task.isSuccessful()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null && !user.isEmailVerified()) {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Verifica tu correo antes de entrar", Toast.LENGTH_LONG).show();
                    return;
                }

                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }

        });

    }

}