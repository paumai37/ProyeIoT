package com.example.proyeiot;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViewById(R.id.btnRegister).setOnClickListener(this::registrar);
        findViewById(R.id.btnGoLogin).setOnClickListener(v -> {
            finish();   // vuelve al login, que está debajo en la pila
            overridePendingTransition(0, 0);
        });
    }

    void registrar(View v) {

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etUser = findViewById(R.id.etUser);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String nombre = etUser.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser nuevo = FirebaseAuth.getInstance().getCurrentUser();

                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();

                    nuevo.updateProfile(perfil).addOnCompleteListener(t1 -> {

                        // 1) Guardar el usuario en Firestore: usuarios/{uid}
                        Map<String, Object> datos = new HashMap<>();
                        datos.put("nombre", nombre);
                        datos.put("email", email);

                        FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(nuevo.getUid())
                                .set(datos)
                                .addOnCompleteListener(t2 ->

                                        // 2) Enviar el correo de verificación
                                        nuevo.sendEmailVerification().addOnCompleteListener(t3 -> {
                                            if (t3.isSuccessful()) {
                                                Toast.makeText(this,
                                                        "Cuenta creada. Revisa tu correo para verificarla",
                                                        Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(this,
                                                        "Cuenta creada, pero no se pudo enviar el correo",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                            // 3) Cerrar sesión (hasta que verifique el correo)
                                            FirebaseAuth.getInstance().signOut();
                                            finish();
                                            overridePendingTransition(0, 0);
                                        }));
                    });
                });
    }
}