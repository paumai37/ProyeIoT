package com.example.proyeiot;

import android.os.Handler;
import android.os.Looper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class UdpReceiver {

    public interface Listener {
        void alRecibir(String mensaje, String ipOrigen);
        void alFallar(Exception e);
    }

    private final int puerto;
    private final Listener listener;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private volatile boolean corriendo = false;
    private DatagramSocket socket;
    private Thread hilo;

    public UdpReceiver(int puerto, Listener listener) {
        this.puerto = puerto;
        this.listener = listener;
    }

    public boolean estaActivo() {
        return corriendo;
    }

    public void iniciar() {
        if (corriendo) return;
        corriendo = true;

        hilo = new Thread(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new java.net.InetSocketAddress(puerto));

                byte[] buffer = new byte[2048];

                while (corriendo) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);   // se bloquea hasta que llega algo

                    final String mensaje = new String(
                            paquete.getData(), 0, paquete.getLength(),
                            StandardCharsets.UTF_8).trim();
                    final String ip = paquete.getAddress() != null
                            ? paquete.getAddress().getHostAddress() : "?";

                    handlerUI.post(() -> listener.alRecibir(mensaje, ip));
                }
            } catch (Exception e) {
                // Al llamar a detener() el socket se cierra y receive() lanza excepción: es normal
                if (corriendo) {
                    corriendo = false;
                    handlerUI.post(() -> listener.alFallar(e));
                }
            } finally {
                if (socket != null) socket.close();
            }
        });
        hilo.start();
    }

    public void detener() {
        corriendo = false;
        if (socket != null) {
            socket.close();   // esto desbloquea receive()
        }
    }
}