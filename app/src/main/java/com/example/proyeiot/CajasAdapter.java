package com.example.proyeiot;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CajasAdapter extends RecyclerView.Adapter<CajasAdapter.CajaViewHolder> {

    private final List<String> cajas;

    public CajasAdapter(List<String> cajas) {
        this.cajas = cajas;
    }

    @NonNull
    @Override
    public CajaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cajas, parent, false);
        return new CajaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CajaViewHolder holder, int position) {
        String nombre = cajas.get(position);
        holder.tvCaja.setText(nombre);

        View.OnClickListener abrir = v -> {
            Intent i = new Intent(v.getContext(), CajaActivity.class);
            i.putExtra("cajaNombre", nombre);
            v.getContext().startActivity(i);
            if (v.getContext() instanceof Activity) {
                ((Activity) v.getContext()).overridePendingTransition(0, 0);
            }
        };
        holder.itemView.setOnClickListener(abrir);   // pulsar la tarjeta
        holder.btnAcceder.setOnClickListener(abrir); // pulsar el botón
    }

    @Override
    public int getItemCount() {
        return cajas.size();
    }

    static class CajaViewHolder extends RecyclerView.ViewHolder {
        TextView tvCaja;
        Button btnAcceder;

        CajaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCaja = itemView.findViewById(R.id.tvCaja);
            btnAcceder = itemView.findViewById(R.id.btnAcceder);
        }
    }
}