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

public class CajaUsuarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_caja_usuario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Nombre de la caja
        String nombre = getIntent().getStringExtra("cajaNombre");
        ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre != null ? nombre : "Caja Casa");

        // Descripción
        ((TextView) findViewById(R.id.tvDescripcion)).setText(
                "Caja de casa que contiene varios utensilios importantes para la empresa y archivos confidenciales.");

        // Usuarios (solo lectura)
        LinearLayout contUsuarios = findViewById(R.id.rvUsuarios);

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(new Usuario("Sergio Benitez Ivars", "ADMIN"));
        usuarios.add(new Usuario("Miguel Gonzalez Moià", "USUARIO"));

        for (Usuario u : usuarios) {
            View fila = getLayoutInflater().inflate(R.layout.item_usuario_info, contUsuarios, false);
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(u.nombre);

            TextView circulo = fila.findViewById(R.id.tvRolCirculo);
            if ("ADMIN".equals(u.rol)) {
                circulo.setText("A");
                circulo.setBackgroundResource(R.drawable.circulo_rol_admin);
            } else {
                circulo.setText("U");
                circulo.setBackgroundResource(R.drawable.circulo_rol_usuario);
            }
            contUsuarios.addView(fila);
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
                startActivity(new Intent(CajaUsuarioActivity.this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(CajaUsuarioActivity.this, AjustesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_cajas) {
                finish();   // vuelve a la lista de cajas
            }
            return true;
        });
    }
}