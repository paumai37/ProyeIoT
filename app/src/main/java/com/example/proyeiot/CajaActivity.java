package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CajaActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String cajaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_caja);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Datos que vienen de la lista de cajas
        cajaId = getIntent().getStringExtra("cajaId");
        String nombre = getIntent().getStringExtra("cajaNombre");
        ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre != null ? nombre : "Caja");

        if (cajaId == null) {
            Toast.makeText(this, "Caja no encontrada", Toast.LENGTH_SHORT).show();
        } else {
            cargarCaja();
            cargarRegistro();
        }

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

    // Descripción, interruptor, objetos y usuarios
    private void cargarCaja() {
        db.collection("cajas").document(cajaId).get()
                .addOnSuccessListener(this, d -> {
                    if (!d.exists()) {
                        Toast.makeText(this, "La caja ya no existe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Nombre (por si cambió en la base de datos)
                    String nombre = d.getString("nombre");
                    if (nombre != null) ((TextView) findViewById(R.id.tvNombreCaja)).setText(nombre);

                    // Descripción
                    String desc = d.getString("descripcion");
                    ((TextView) findViewById(R.id.tvDescripcion)).setText(desc != null ? desc : "");

                    // Interruptor de bloqueo: primero el valor, después el listener
                    // (así cargar los datos no provoca una escritura)
                    SwitchCompat sw = findViewById(R.id.switchBloquear);
                    Boolean bloqueada = d.getBoolean("bloqueada");
                    boolean activo = bloqueada != null && bloqueada;
                    sw.setOnCheckedChangeListener(null);
                    sw.setChecked(activo);
                    sw.setText(activo ? "ON" : "OFF");
                    sw.setOnCheckedChangeListener((b, nuevo) -> {
                        sw.setText(nuevo ? "ON" : "OFF");
                        db.collection("cajas").document(cajaId)
                                .update("bloqueada", nuevo)
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "No se pudo guardar: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    // Deshacer el cambio visual
                                    sw.setOnCheckedChangeListener(null);
                                    sw.setChecked(!nuevo);
                                    sw.setText(!nuevo ? "ON" : "OFF");
                                    // (si falla, recarga para dejar el listener bien puesto)
                                    cargarCaja();
                                });
                    });

                    // Objetos detectados
                    List<String> objetos = (List<String>) d.get("objetos");
                    ((TextView) findViewById(R.id.tvObjetos)).setText(
                            objetos != null && !objetos.isEmpty()
                                    ? String.join("\n", objetos)
                                    : "Sin objetos");

                    // Usuarios
                    List<String> miembros = (List<String>) d.get("miembros");
                    Map<String, String> roles = (Map<String, String>) d.get("roles");
                    mostrarUsuarios(miembros, roles);
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Una fila por miembro. La fila se crea ya (para mantener el orden)
    // y el nombre se rellena cuando llega de Firestore.
    private void mostrarUsuarios(List<String> miembros, Map<String, String> roles) {
        LinearLayout contUsuarios = findViewById(R.id.rvUsuarios);
        contUsuarios.removeAllViews();
        if (miembros == null) return;

        for (String uid : miembros) {
            View fila = getLayoutInflater().inflate(R.layout.item_usuario, contUsuarios, false);
            TextView tvNombre = fila.findViewById(R.id.tvNombre);
            tvNombre.setText("...");

            // Círculo según el rol: ADMIN = azul con "A", USUARIO = verde con "U"
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

    // Últimos 20 accesos, el más reciente primero
    private void cargarRegistro() {
        db.collection("cajas").document(cajaId).collection("registros")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(this, snap -> {
                    LinearLayout contRegistro = findViewById(R.id.rvRegistro);
                    contRegistro.removeAllViews();

                    SimpleDateFormat fmt = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

                    for (DocumentSnapshot r : snap.getDocuments()) {
                        String usuario = r.getString("usuario");
                        Timestamp ts = r.getTimestamp("fecha");
                        String fecha = ts != null ? fmt.format(new Date(ts.toDate().getTime())) : "-";

                        View fila = getLayoutInflater().inflate(R.layout.item_registro, contRegistro, false);
                        ((TextView) fila.findViewById(R.id.tvUsuario)).setText(usuario != null ? usuario : "-");
                        ((TextView) fila.findViewById(R.id.tvFecha)).setText(fecha);
                        contRegistro.addView(fila);
                    }
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error en el registro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}