package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ajustes);


        findViewById(R.id.btnAjustesPerfil).setOnClickListener(this::gohome);
        findViewById(R.id.btnAcercade).setOnClickListener(this::goacercade);


        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_ajustes);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_mapa) {
                startActivity(new Intent(AjustesActivity.this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_cajas) {
                startActivity(new Intent(AjustesActivity.this, cajasActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });
    }
    void  gohome (View v) {
        startActivity(new Intent(AjustesActivity.this, HomeActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
    void  goacercade (View v) {
        startActivity(new Intent(AjustesActivity.this, AcercadeActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

}