package com.example.proyeiot;

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
        holder.tvCaja.setText(cajas.get(position));
        holder.btnAcceder.setOnClickListener(v -> {
            // abrir la caja
        });
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