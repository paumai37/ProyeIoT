package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.btnClose).setOnClickListener(this::CloseSesion);
        findViewById(R.id.btnVolver).setOnClickListener(this::volver);

        texto();
    }

    void CloseSesion(View v) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    void volver(View v) {
        startActivity(new Intent(HomeActivity.this, AjustesActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    void texto() {
        TextView emailTextView = findViewById(R.id.emailtextview);
        TextView sesiontextView = findViewById(R.id.sesion);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            emailTextView.setText(user.getEmail());
            String nombre = user.getDisplayName();
            sesiontextView.setText(nombre != null && !nombre.isEmpty() ? nombre : "Sin nombre");
        } else {
            emailTextView.setText("Sin sesión iniciada");
            sesiontextView.setText("-");
        }
    }
}