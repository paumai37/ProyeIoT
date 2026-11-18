package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class CajaUsuarioActivity extends AppCompatActivity {

    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();

        // Datos que vienen de la lista de cajas
        String cajaId = getIntent().getStringExtra("cajaId");
        String nombre = getIntent().getStringExtra("cajaNombre");
        ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre != null ? nombre : "Caja");

        if (cajaId == null) {
            Toast.makeText(this, "Caja no encontrada", Toast.LENGTH_SHORT).show();
        } else {
            cargarCaja(cajaId);
        }

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

    // Solo lectura: descripción, objetos y usuarios
    private void cargarCaja(String cajaId) {
        db.collection("cajas").document(cajaId).get()
                .addOnSuccessListener(this, d -> {
                    if (!d.exists()) {
                        Toast.makeText(this, "La caja ya no existe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String nombre = d.getString("nombre");
                    if (nombre != null) ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre);

                    String desc = d.getString("descripcion");
                    ((TextView) findViewById(R.id.tvDescripcion)).setText(desc != null ? desc : "");

                    // Objetos detectados
                    List<String> objetos = (List<String>) d.get("objetos");
                    ((TextView) findViewById(R.id.tvObjetos)).setText(
                            objetos != null && !objetos.isEmpty()
                                    ? String.join("\n", objetos)
                                    : "Sin objetos");

                    // Usuarios (solo lectura)
                    List<String> miembros = (List<String>) d.get("miembros");
                    Map<String, String> roles = (Map<String, String>) d.get("roles");
                    mostrarUsuarios(miembros, roles);
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void mostrarUsuarios(List<String> miembros, Map<String, String> roles) {
        LinearLayout contUsuarios = findViewById(R.id.rvUsuarios);
        contUsuarios.removeAllViews();
        if (miembros == null) return;

        for (String uid : miembros) {
            View fila = getLayoutInflater().inflate(R.layout.item_usuario_info, contUsuarios, false);
            TextView tvNombre = fila.findViewById(R.id.tvNombre);
            tvNombre.setText("...");

            String rol = (roles != null && roles.get(uid) != null) ? roles.get(uid) : "USUARIO";
            TextView circulo = fila.findViewById(R.id.tvRolCirculo);
            if ("ADMIN".equals(rol)) {
                circulo.setText("A");
                circulo.setBackgroundResource(R.drawable.circulo_rol_admin);
            } else {
                circulo.setText("U");
                circulo.setBackgroundResource(R.drawable.circulo_rol_usuario);
            }
            contUsuarios.addView(fila);

            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(this, u -> {
                        String n = u.getString("nombre");
                        tvNombre.setText(n != null && !n.isEmpty() ? n : "Sin nombre");
                    })
                    .addOnFailureListener(this, e -> tvNombre.setText("Sin nombre"));
        }
    }
}