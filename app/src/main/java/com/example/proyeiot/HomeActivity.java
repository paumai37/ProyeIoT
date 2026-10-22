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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_ajustes);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_cajas) {
                startActivity(new Intent(HomeActivity.this, cajasActivity.class));
            } else if (id == R.id.nav_mapa) {
                startActivity(new Intent(HomeActivity.this, MapActivity.class));
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(HomeActivity.this, AjustesActivity.class));
            }
            return true;
        });




        findViewById(R.id.btnClose).setOnClickListener(this::CloseSesion);
        findViewById(R.id.btnmapa).setOnClickListener(this::gomap);

        texto();

    }

    void CloseSesion (View v) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
        finish();
    }

    void gomap (View v) {
        startActivity(new Intent(HomeActivity.this, MapActivity.class));
    }


    void texto() {
        TextView emailTextView = findViewById(R.id.emailtextview);
        TextView sesiontextView = findViewById(R.id.sesion);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            emailTextView.setText(user.getEmail());
            sesiontextView.setText(user.getDisplayName());
        } else {
            emailTextView.setText("Sin sesión iniciada");
        }
    }

}