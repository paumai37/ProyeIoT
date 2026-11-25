package com.example.proyeiot;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class cajasActivity extends AppCompatActivity {

    // Caja con su id de Firestore y el rol del usuario actual ("ADMIN" o "USUARIO")
    private static class Caja {
        final String id;
        final String nombre;
        final String rol;

        Caja(String id, String nombre, String rol) {
            this.id = id;
            this.nombre = nombre;
            this.rol = rol;
        }
    }

    // Guardamos cada fila junto con su nombre para poder filtrar
    private final List<View> filas = new ArrayList<>();
    private final List<String> nombres = new ArrayList<>();

    private LinearLayout cont;
    private EditText etBuscar;

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

        cont = findViewById(R.id.contCajas);

        cargarCajas();

        // Buscador: filtra mientras escribes
        etBuscar = findViewById(R.id.etBuscar);
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

        findViewById(R.id.fabAnadir).setOnClickListener(this::popupCrearCaja);

        // Botón atrás del móvil -> Mapa
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(cajasActivity.this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });
    }

    void popupCrearCaja(View v) {
        View vista = getLayoutInflater().inflate(R.layout.popup_crear_caja, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(vista)
                .create();

        EditText etCodigo = vista.findViewById(R.id.etCodigo);
        Button btnAceptar = vista.findViewById(R.id.btnAceptar);
        Button btnCancelar = vista.findViewById(R.id.btnCancelar);

        btnAceptar.setOnClickListener(b -> {
            String codigo = etCodigo.getText().toString().trim();

            if (codigo.isEmpty()) {
                etCodigo.setError("Escribe un código");
                return;
            }

            // Aquí creas la caja con ese código
            if (codigo.contains("/") || codigo.equals(".") || codigo.equals("..")
                    || codigo.startsWith("__")) {
                etCodigo.setError("Código no válido (sin \"/\" y sin empezar por \"__\")");
                return;
            }

            btnAceptar.setEnabled(false);   // evita dobles pulsaciones mientras se procesa
            entrarOCrearCaja(codigo, dialog, btnAceptar);
        });

        btnCancelar.setOnClickListener(b -> dialog.dismiss());

        dialog.show();
    }

    // Si existe cajas/{codigo}: te añade como ADMIN.
    // Si no existe: la crea con ese id y te deja como único miembro y ADMIN.
    private void entrarOCrearCaja(String codigo, AlertDialog dialog, Button btnAceptar) {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("cajas").document(codigo);

        db.runTransaction(tr -> {
                    DocumentSnapshot d = tr.get(ref);

                    if (!d.exists()) {
                        Map<String, Object> caja = new HashMap<>();
                        caja.put("nombre", "Mi caja");
                        caja.put("descripcion", "La descripción de mi caja");
                        caja.put("bloqueada", false);
                        caja.put("objetos", new ArrayList<String>());
                        caja.put("miembros", new ArrayList<>(Collections.singletonList(uid)));
                        caja.put("roles", Collections.singletonMap(uid, "ADMIN"));
                        tr.set(ref, caja);
                        return "CREADA";
                    }

                    List<String> miembros = (List<String>) d.get("miembros");
                    Map<String, String> roles = (Map<String, String>) d.get("roles");
                    boolean esMiembro = miembros != null && miembros.contains(uid);
                    boolean esAdmin = roles != null && "ADMIN".equals(roles.get(uid));
                    if (esMiembro && esAdmin) return "YA_ADMIN";

                    tr.update(ref,
                            "miembros", FieldValue.arrayUnion(uid),
                            "roles." + uid, "ADMIN");
                    return "UNIDO";
                })
                .addOnSuccessListener(this, resultado -> {
                    switch (resultado) {
                        case "CREADA":
                            Toast.makeText(this, "Caja creada. Eres ADMIN", Toast.LENGTH_SHORT).show();
                            break;
                        case "UNIDO":
                            Toast.makeText(this, "Te has unido a la caja como ADMIN", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(this, "Ya eres ADMIN de esta caja", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    cargarCajas();   // refresca la lista para que aparezca
                })
                .addOnFailureListener(this, e -> {
                    btnAceptar.setEnabled(true);
                    boolean sinPermisos = e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                    Toast.makeText(this,
                            sinPermisos ? "Sin permisos: revisa las reglas de Firestore"
                                    : "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }



    // Carga de Firestore las cajas donde el usuario actual es miembro
    private void cargarCajas() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "No hay sesión iniciada", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("cajas")
                .whereArrayContains("miembros", uid)
                .get()
                .addOnSuccessListener(this, snap -> {
                    cont.removeAllViews();
                    filas.clear();
                    nombres.clear();

                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No tienes cajas asignadas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String nombre = d.getString("nombre");
                        Map<String, String> roles = (Map<String, String>) d.get("roles");
                        String rol = (roles != null && roles.get(uid) != null) ? roles.get(uid) : "USUARIO";

                        añadirFila(new Caja(d.getId(), nombre != null ? nombre : "Sin nombre", rol));
                    }

                    // Si ya había algo escrito en el buscador, se vuelve a aplicar
                    if (etBuscar != null) filtrar(etBuscar.getText().toString());
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void añadirFila(Caja caja) {
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
            i.putExtra("cajaId", caja.id);
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

    private void filtrar(String texto) {
        String q = texto.trim().toLowerCase(Locale.getDefault());
        for (int i = 0; i < filas.size(); i++) {
            boolean coincide = nombres.get(i).toLowerCase(Locale.getDefault()).contains(q);
            filas.get(i).setVisibility(coincide ? View.VISIBLE : View.GONE);
        }
    }
}