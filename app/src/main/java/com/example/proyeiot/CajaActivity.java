package com.example.proyeiot;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class CajaActivity extends AppCompatActivity {

    private String ultimoUid = "sin_uid";
    private FirebaseFirestore db;
    private String cajaId;

    // Detector (UDP)
    private boolean sensorDetectado = false;
    private static final String TAG = "UDP";
    private static final int PUERTO = 5005;
    private static final int PUERTO_FOTO = 5009;
    private FotoUdpReceiver receptorFoto;
    private UdpReceiver receptor;
    private SwitchCompat switchBloquear;
    private volatile boolean recibiendoDatos = false;

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

        // --- Edición (solo ADMIN llega a esta pantalla) ---
        findViewById(R.id.btnEditarNombre).setOnClickListener(v -> editarNombre());
        findViewById(R.id.tvNombreCaja).setOnClickListener(v -> editarNombre());
        findViewById(R.id.btnEditarDescripcion).setOnClickListener(v -> editarDescripcion());
        findViewById(R.id.tvDescripcion).setOnClickListener(v -> editarDescripcion());
        findViewById(R.id.btnAnadirUsuario).setOnClickListener(v -> anadirUsuario());

        // --- Escucha UDP del sensor (se controla con el switch de bloquear) ---
        switchBloquear = findViewById(R.id.switchBloquear);

        receptor = new UdpReceiver(PUERTO, new UdpReceiver.Listener() {
            @Override
            public void alRecibir(String mensaje, String ipOrigen) {
                recibiendoDatos = true;
                Log.d(TAG, ipOrigen + " → " + mensaje);

                // dist: o uid: significa que el sensor está conectado
                if (!sensorDetectado && (mensaje.startsWith("dist:") || mensaje.startsWith("uid:"))) {
                    sensorDetectado = true;
                    Toast.makeText(CajaActivity.this, "Sensor detectado", Toast.LENGTH_SHORT).show();
                }

                if (mensaje.startsWith("uid:")) {
                    ultimoUid = mensaje.substring(4);
                    Toast.makeText(CajaActivity.this,
                            "Tarjeta: " + mensaje.substring(4), Toast.LENGTH_SHORT).show();
                }

                if (mensaje.startsWith("dist:")) {
                    try {
                        float distancia = Float.parseFloat(mensaje.substring(5));
                        if (distancia < 6) {
                            Toast.makeText(CajaActivity.this, "Caja cerrada", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(CajaActivity.this, "error en caja", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void alFallar(Exception e) {
                // No tocamos el switch: cambiarlo escribiría en Firestore
                Toast.makeText(CajaActivity.this,
                        "Error UDP: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        receptorFoto = new FotoUdpReceiver(PUERTO_FOTO, new FotoUdpReceiver.Listener() {
            @Override
            public void alRecibirFoto(byte[] jpeg, int evento, int frameId) {
                Log.d("FOTO", "foto completa: " + jpeg.length + " bytes");
                if (isFinishing()) return;

                // Primero comprobamos que la imagen es válida: no guardamos basura
                Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                if (bmp == null) {
                    Toast.makeText(CajaActivity.this, "Foto incompleta", Toast.LENGTH_SHORT).show();
                    return;
                }

                guardarFoto(ajustarTamano(jpeg, bmp));

                ImageView iv = new ImageView(CajaActivity.this);
                iv.setImageBitmap(bmp);
                new AlertDialog.Builder(CajaActivity.this)
                        .setTitle("Foto")
                        .setView(iv)
                        .setPositiveButton("Cerrar", null)
                        .show();
            }

            @Override
            public void alFallar(Exception e) {
                Toast.makeText(CajaActivity.this, "Error foto UDP: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        receptorFoto.iniciar();




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


    // Guarda la foto y su registro a la vez: o se guardan los dos o ninguno
    private void guardarFoto(byte[] jpeg) {
        DocumentReference cajaRef = db.collection("cajas").document(cajaId);
        DocumentReference fotoRef = cajaRef.collection("fotos").document();
        DocumentReference registroRef = cajaRef.collection("registros").document();

        Map<String, Object> datosFoto = new HashMap<>();
        datosFoto.put("foto", Base64.encodeToString(jpeg, Base64.NO_WRAP));

        Map<String, Object> registro = new HashMap<>();
        registro.put("usuario", ultimoUid);
        registro.put("fecha", FieldValue.serverTimestamp());
        registro.put("fotoId", fotoRef.getId());

        WriteBatch batch = db.batch();
        batch.set(fotoRef, datosFoto);
        batch.set(registroRef, registro);

        batch.commit()
                .addOnSuccessListener(x -> cargarRegistro())   // refresca la lista de accesos
                .addOnFailureListener(e -> Toast.makeText(CajaActivity.this,
                        "No se pudo guardar la foto: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Firestore limita cada documento a 1 MiB y Base64 pesa un 33 % más que el JPEG.
    // Si la foto es grande, se reduce hasta que quepa con margen.
    private byte[] ajustarTamano(byte[] jpeg, Bitmap bmp) {
        final int MAX_BYTES = 700_000;
        byte[] resultado = jpeg;
        Bitmap b = bmp;
        while (resultado.length > MAX_BYTES && b.getWidth() > 320) {
            b = Bitmap.createScaledBitmap(b, b.getWidth() * 3 / 4, b.getHeight() * 3 / 4, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 70, out);
            resultado = out.toByteArray();
        }
        return resultado;
    }

    private void verFoto(String fotoId) {
        db.collection("cajas").document(cajaId).collection("fotos").document(fotoId).get()
                .addOnSuccessListener(this, d -> {
                    String texto = d.getString("foto");
                    if (texto == null) {
                        Toast.makeText(this, "La foto no tiene datos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    byte[] bytes = Base64.decode(texto, Base64.NO_WRAP);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp == null) {
                        Toast.makeText(this, "La foto está corrupta", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ImageView iv = new ImageView(this);
                    iv.setImageBitmap(bmp);
                    new AlertDialog.Builder(this)
                            .setTitle("Foto")
                            .setView(iv)
                            .setPositiveButton("Cerrar", null)
                            .show();
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void buscarSensor() {
        recibiendoDatos = false;
        new Thread(() -> {
            try (DatagramSocket s = new DatagramSocket()) {
                s.setBroadcast(true);
                byte[] hola = "HOLA".getBytes(StandardCharsets.UTF_8);
                DatagramPacket p = new DatagramPacket(
                        hola, hola.length,
                        InetAddress.getByName("255.255.255.255"), 4210);

                for (int i = 0; i < 15 && !recibiendoDatos; i++) {
                    s.send(p);
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error buscando sensor", e);
            }
        }).start();
    }

    private void iniciarEscucha() {
        if (receptor.estaActivo()) return;   // evita arrancarla dos veces
        sensorDetectado = false;
        receptor.iniciar();
        buscarSensor();
    }

    private void detenerEscucha() {
        receptor.detener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Al volver a la pantalla, retoma la escucha si el switch está en ON
        if (switchBloquear != null && switchBloquear.isChecked()) iniciarEscucha();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detenerEscucha();   // solo para el socket; el switch y Firestore no se tocan
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receptor != null) receptor.detener();
        if (receptorFoto != null) receptorFoto.detener();
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

                    // Interruptor de bloqueo + escucha UDP (un único listener)
                    SwitchCompat sw = findViewById(R.id.switchBloquear);
                    Boolean bloqueada = d.getBoolean("bloqueada");
                    boolean activo = bloqueada != null && bloqueada;

                    // Primero el valor, después el listener
                    // (así cargar los datos no provoca una escritura)
                    sw.setOnCheckedChangeListener(null);
                    sw.setChecked(activo);
                    sw.setText(activo ? "ON" : "OFF");

                    // Estado inicial de la escucha según el valor guardado
                    if (activo) iniciarEscucha(); else detenerEscucha();

                    sw.setOnCheckedChangeListener((b, nuevo) -> {
                        sw.setText(nuevo ? "ON" : "OFF");

                        // 1) Escucha UDP
                        if (nuevo) iniciarEscucha(); else detenerEscucha();

                        // 2) Guardar en Firestore
                        db.collection("cajas").document(cajaId)
                                .update("bloqueada", nuevo)
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "No se pudo guardar: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    // Deshacer el cambio visual
                                    sw.setOnCheckedChangeListener(null);
                                    sw.setChecked(!nuevo);
                                    sw.setText(!nuevo ? "ON" : "OFF");
                                    detenerEscucha();
                                    // Recarga para dejar el listener bien puesto
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

            // Flecha arriba -> ADMIN, flecha abajo -> USUARIO
            fila.findViewById(R.id.btnSubirRol).setOnClickListener(v -> cambiarRol(uid, "ADMIN", roles));
            fila.findViewById(R.id.btnBajarRol).setOnClickListener(v -> cambiarRol(uid, "USUARIO", roles));

            // Papelera -> quitar al usuario de la caja (pide confirmación)
            fila.findViewById(R.id.btnEliminarUsuario).setOnClickListener(v ->
                    confirmarEliminar(uid, tvNombre.getText().toString(), roles));

            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(this, u -> {
                        String n = u.getString("nombre");
                        tvNombre.setText(n != null && !n.isEmpty() ? n : "Sin nombre");
                    })
                    .addOnFailureListener(this, e -> tvNombre.setText("Sin nombre"));
        }
    }

    // ------------------------------------------------------------------
    // Edición
    // ------------------------------------------------------------------

    // Diálogo genérico con un campo de texto
    private void dialogoTexto(String titulo, String valorActual, boolean multilinea,
                              Consumer<String> alAceptar) {
        EditText et = new EditText(this);
        et.setText(valorActual);
        et.setSelection(et.getText().length());
        if (multilinea) {
            et.setMinLines(3);
            et.setGravity(android.view.Gravity.TOP);
        } else {
            et.setSingleLine(true);
        }

        FrameLayout marco = new FrameLayout(this);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        marco.setPadding(p, p / 2, p, 0);
        marco.addView(et);

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setView(marco)
                .setPositiveButton("Guardar", (d, w) -> alAceptar.accept(et.getText().toString().trim()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void editarNombre() {
        if (cajaId == null) return;
        TextView tv = findViewById(R.id.tvNombreCaja);
        dialogoTexto("Nombre de la caja", tv.getText().toString(), false, nuevo -> {
            if (nuevo.isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("cajas").document(cajaId).update("nombre", nuevo)
                    .addOnSuccessListener(x -> tv.setText(nuevo))
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "No se pudo guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void editarDescripcion() {
        if (cajaId == null) return;
        TextView tv = findViewById(R.id.tvDescripcion);
        dialogoTexto("Descripción", tv.getText().toString(), true, nuevo ->
                db.collection("cajas").document(cajaId).update("descripcion", nuevo)
                        .addOnSuccessListener(x -> tv.setText(nuevo))
                        .addOnFailureListener(e -> Toast.makeText(this,
                                "No se pudo guardar: " + e.getMessage(), Toast.LENGTH_LONG).show()));
    }

    // Busca al usuario por email en usuarios/ y lo añade a miembros con rol USUARIO
    private void anadirUsuario() {
        if (cajaId == null) return;
        dialogoTexto("Añadir usuario (email)", "", false, email -> {
            if (email.isEmpty()) return;

            db.collection("usuarios").whereEqualTo("email", email).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            agregarMiembro(snap.getDocuments().get(0).getId());
                            return;
                        }
                        // Segundo intento en minúsculas
                        db.collection("usuarios")
                                .whereEqualTo("email", email.toLowerCase(Locale.ROOT))
                                .limit(1).get()
                                .addOnSuccessListener(snap2 -> {
                                    if (snap2.isEmpty()) {
                                        Toast.makeText(this, "No existe ningún usuario con ese email",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        agregarMiembro(snap2.getDocuments().get(0).getId());
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void agregarMiembro(String uidNuevo) {
        db.collection("cajas").document(cajaId).get().addOnSuccessListener(d -> {
            List<String> miembros = (List<String>) d.get("miembros");
            if (miembros != null && miembros.contains(uidNuevo)) {
                Toast.makeText(this, "Ese usuario ya está en la caja", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("cajas").document(cajaId).update(
                            "miembros", FieldValue.arrayUnion(uidNuevo),
                            "roles." + uidNuevo, "USUARIO")
                    .addOnSuccessListener(x -> {
                        Toast.makeText(this, "Usuario añadido", Toast.LENGTH_SHORT).show();
                        cargarCaja();   // refresca la lista de usuarios
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "No se pudo añadir: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }).addOnFailureListener(e -> Toast.makeText(this,
                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Cambia el rol de un miembro y lo guarda en cajas/{id}.roles.{uid}
    private void cambiarRol(String uid, String nuevoRol, Map<String, String> roles) {
        String actual = (roles != null && roles.get(uid) != null) ? roles.get(uid) : "USUARIO";
        if (actual.equals(nuevoRol)) {
            Toast.makeText(this, "Ya es " + nuevoRol, Toast.LENGTH_SHORT).show();
            return;
        }

        // Nunca dejar la caja sin ningún admin
        if ("USUARIO".equals(nuevoRol) && roles != null) {
            int admins = 0;
            for (String r : roles.values()) if ("ADMIN".equals(r)) admins++;
            if (admins <= 1) {
                Toast.makeText(this, "La caja tiene que tener al menos un admin",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        db.collection("cajas").document(cajaId).update("roles." + uid, nuevoRol)
                .addOnSuccessListener(x -> cargarCaja())   // refresca los círculos A/U
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se pudo cambiar el rol: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Pregunta antes de quitar a un usuario de la caja
    private void confirmarEliminar(String uid, String nombre, Map<String, String> roles) {
        String rol = (roles != null && roles.get(uid) != null) ? roles.get(uid) : "USUARIO";

        // Nunca dejar la caja sin ningún admin
        if ("ADMIN".equals(rol) && roles != null) {
            int admins = 0;
            for (String r : roles.values()) if ("ADMIN".equals(r)) admins++;
            if (admins <= 1) {
                Toast.makeText(this, "No puedes eliminar al único admin de la caja",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Quitar a " + nombre + " de esta caja?")
                .setPositiveButton("Eliminar", (d, w) -> eliminarMiembro(uid))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Quita el UID de miembros y borra su rol en cajas/{id}
    private void eliminarMiembro(String uid) {
        db.collection("cajas").document(cajaId).update(
                        "miembros", FieldValue.arrayRemove(uid),
                        "roles." + uid, FieldValue.delete())
                .addOnSuccessListener(x -> {
                    Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    // Si el admin se quitó a sí mismo, ya no tiene acceso a esta pantalla
                    if (uid.equals(FirebaseAuth.getInstance().getUid())) {
                        finish();
                    } else {
                        cargarCaja();   // refresca la lista
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
                        String fotoId = r.getString("fotoId");
                        View btnFoto = fila.findViewById(R.id.btnVerFoto);
                        if (fotoId == null) {
                            btnFoto.setVisibility(View.GONE);
                        } else {
                            btnFoto.setOnClickListener(v -> verFoto(fotoId));
                        }
                    }
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, "Error en el registro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}