package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Lista de cajas (sin adapter)
        LinearLayout cont = findViewById(R.id.contCajas);

        List<String> cajas = new ArrayList<>();
        cajas.add("Caja 1");
        cajas.add("Caja 2");
        cajas.add("Caja 3");

        for (String nombre : cajas) {
            View fila = getLayoutInflater().inflate(R.layout.item_cajas, cont, false);
            ((TextView) fila.findViewById(R.id.tvCaja)).setText(nombre);

            View.OnClickListener abrir = v -> {
                Intent i = new Intent(cajasActivity.this, CajaActivity.class);
                i.putExtra("cajaNombre", nombre);
                startActivity(i);
            };
            fila.setOnClickListener(abrir);                              // pulsar la tarjeta
            fila.findViewById(R.id.btnAcceder).setOnClickListener(abrir); // pulsar el botón

            cont.addView(fila);
        }

        // Barra inferior
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_cajas);   // antes del listener, para no dispararlo

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_mapa) {
                startActivity(new Intent(cajasActivity.this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(cajasActivity.this, AjustesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            // nav_cajas: ya estás aquí, no hace nada
            return true;
        });
    }
}