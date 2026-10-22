package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class cajasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cajas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lista de cajas
        RecyclerView rv = findViewById(R.id.rvCajas);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<String> cajas = new ArrayList<>();
        cajas.add("Caja 1");
        cajas.add("Caja 2");
        cajas.add("Caja 3");

        rv.setAdapter(new CajasAdapter(cajas));

        // Barra inferior (una sola vez)
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_cajas);   // antes del listener, para no dispararlo

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_mapa) {
                startActivity(new Intent(cajasActivity.this, MapActivity.class));
                finish();
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(cajasActivity.this, AjustesActivity.class));
                finish();
            }
            // nav_cajas: ya estás aquí, no hace nada
            return true;
        });
    }
}