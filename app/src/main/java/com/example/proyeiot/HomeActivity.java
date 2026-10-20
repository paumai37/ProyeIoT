package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnClose).setOnClickListener(this::CloseSesion);

        texto();

    }

    void CloseSesion (View v) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
        finish();
    }


    void texto() {
        TextView emailTextView = findViewById(R.id.emailtextview);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            emailTextView.setText(user.getEmail());
        } else {
            emailTextView.setText("Sin sesión iniciada");
        }
    }

}