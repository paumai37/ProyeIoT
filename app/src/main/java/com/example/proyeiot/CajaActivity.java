package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class CajaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_caja);


        // Nombre de la caja que se pulsó
        String nombre = getIntent().getStringExtra("cajaNombre");
        ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre != null ? nombre : "Caja Casa");

        // Descripción
        ((TextView) findViewById(R.id.tvDescripcion)).setText(
                "Caja de casa que contiene varios utensilios importantes para la empresa y archivos confidenciales.");

        // Interruptor de bloqueo
        SwitchCompat sw = findViewById(R.id.switchBloquear);
        sw.setChecked(true);
        sw.setText("ON");
        sw.setOnCheckedChangeListener((b, activo) -> sw.setText(activo ? "ON" : "OFF"));

        // Lista de usuarios (sin adapter)
        LinearLayout contUsuarios = findViewById(R.id.rvUsuarios);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(new Usuario("Sergio Benitez Ivars"));
        usuarios.add(new Usuario("Miguel Gonzalez Moià"));

        for (Usuario u : usuarios) {
            View fila = getLayoutInflater().inflate(R.layout.item_usuario, contUsuarios, false);
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(u.nombre);
            contUsuarios.addView(fila);
        }

        // Lista del registro (sin adapter)
        LinearLayout contRegistro = findViewById(R.id.rvRegistro);

        List<Registro> registros = new ArrayList<>();
        registros.add(new Registro("Sergio Benitez Ivars", "12:52 12/12/2025"));
        registros.add(new Registro("Miguel Gonzalez Moià", "12:32 12/12/2025"));
        registros.add(new Registro("Miguel Gonzalez Moià", "12:20 12/12/2025"));

        for (Registro r : registros) {
            View fila = getLayoutInflater().inflate(R.layout.item_registro, contRegistro, false);
            ((TextView) fila.findViewById(R.id.tvUsuario)).setText(r.usuario);
            ((TextView) fila.findViewById(R.id.tvFecha)).setText(r.fecha);
            contRegistro.addView(fila);
        }

        // Objetos detectados
        List<String> objetos = new ArrayList<>();
        objetos.add("1x Archivos");
        objetos.add("2x Soldadores");
        objetos.add("2x Guantes");

        ((TextView) findViewById(R.id.tvObjetos)).setText(String.join("\n", objetos));

        // Barra inferior
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_cajas);   // antes del listener, para no dispararlo

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_mapa) {
                startActivity(new Intent(CajaActivity.this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(CajaActivity.this, AjustesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_cajas) {
                finish();   // vuelve a la lista de cajas
            }
            return true;
        });
    }
}