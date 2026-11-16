package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cajasActivity extends AppCompatActivity {

    // Caja con su rol del usuario actual ("ADMIN" o "USUARIO")
    private static class Caja {
        final String nombre;
        final String rol;

        Caja(String nombre, String rol) {
            this.nombre = nombre;
            this.rol = rol;
        }
    }

    // Guardamos cada fila junto con su nombre para poder filtrar
    private final List<View> filas = new ArrayList<>();
    private final List<String> nombres = new ArrayList<>();

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

        // Lista de cajas (sin adapter). Cambia los roles aquí para probar.
        LinearLayout cont = findViewById(R.id.contCajas);

        List<Caja> cajas = new ArrayList<>();
        cajas.add(new Caja("Caja 1", "USUARIO"));
        cajas.add(new Caja("Caja 2", "ADMIN"));
        cajas.add(new Caja("Caja 3", "ADMIN"));

        for (Caja caja : cajas) {
            View fila = getLayoutInflater().inflate(R.layout.item_cajas, cont, false);
            ((TextView) fila.findViewById(R.id.tvCaja)).setText(caja.nombre);

            // Círculo según el rol
            TextView circulo = fila.findViewById(R.id.vRol);
            if ("ADMIN".equals(caja.rol)) {
                circulo.setText("A");
                circulo.setBackgroundResource(R.drawable.circulo_rol_admin);
            } else {
                circulo.setText("U");
                circulo.setBackgroundResource(R.drawable.circulo_rol_usuario);
            }

            // Admin -> pantalla completa. Usuario -> pantalla de solo lectura.
            View.OnClickListener abrir = v -> {
                Class<?> destino = "ADMIN".equals(caja.rol)
                        ? CajaActivity.class
                        : CajaUsuarioActivity.class;
                Intent i = new Intent(cajasActivity.this, destino);
                i.putExtra("cajaNombre", caja.nombre);
                i.putExtra("rol", caja.rol);
                startActivity(i);
            };
            fila.setOnClickListener(abrir);                               // pulsar la tarjeta
            fila.findViewById(R.id.btnAcceder).setOnClickListener(abrir); // pulsar el botón

            cont.addView(fila);
            filas.add(fila);
            nombres.add(caja.nombre);
        }

        // Buscador: filtra mientras escribes
        EditText etBuscar = findViewById(R.id.etBuscar);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Campana de arriba a la derecha
        findViewById(R.id.ivCampana).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show());

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

    private void filtrar(String texto) {
        String q = texto.trim().toLowerCase(Locale.getDefault());
        for (int i = 0; i < filas.size(); i++) {
            boolean coincide = nombres.get(i).toLowerCase(Locale.getDefault()).contains(q);
            filas.get(i).setVisibility(coincide ? View.VISIBLE : View.GONE);
        }
    }
}