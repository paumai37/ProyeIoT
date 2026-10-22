package com.example.proyeiot;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración de osmdroid: antes de setContentView
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("ProyeIoT-App-Android/1.0");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mapa
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.OpenTopo);
        map.setMaxZoomLevel(17.0);
        map.setMultiTouchControls(true);   // zoom con dos dedos

        // Atribución del mapa
        map.getOverlays().add(new CopyrightOverlay(this));

        // Cambia estas coordenadas por las tuyas
        GeoPoint caja1 = new GeoPoint(38.928765786041595, -0.1877644473806444);
        GeoPoint caja2 = new GeoPoint(38.99632678834556, -0.16609262781082546);

        map.getController().setZoom(15.0);
        map.getController().setCenter(caja1);

        anadirMarcador(caja1, "Caja 1");
        anadirMarcador(caja2, "Caja 2");

        // Barra inferior
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_mapa);   // antes del listener, para no dispararlo
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cajas) {
                startActivity(new Intent(MapActivity.this, cajasActivity.class));
                finish();
            } else if (id == R.id.nav_ajustes) {
                startActivity(new Intent(MapActivity.this, AjustesActivity.class));
                finish();
            }
            // nav_mapa: ya estás aquí, no hace nada
            return true;
        });
    }

    private void anadirMarcador(GeoPoint punto, String titulo) {
        Marker marcador = new Marker(map);
        marcador.setPosition(punto);
        marcador.setTitle(titulo);
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marcador);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}