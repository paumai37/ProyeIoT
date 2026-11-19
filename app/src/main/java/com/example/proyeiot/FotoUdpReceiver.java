package com.example.proyeiot;

import android.os.Handler;
import android.os.Looper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import android.util.Log;
public class FotoUdpReceiver {

    public interface Listener {
        void alRecibirFoto(byte[] jpeg, int evento, int frameId);
        void alFallar(Exception e);
    }

    private static final long TIMEOUT_MS = 3000;   // descartar fotos incompletas tras 3 s
    private static final int MAX_PAQUETES = 1000;  // protección contra cabeceras corruptas

    private final int puerto;
    private final Listener listener;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    // Solo se usa desde el hilo de recepción, así que no necesita sincronización
    private final Map<Integer, Foto> fotos = new HashMap<>();

    private volatile boolean corriendo = false;
    private DatagramSocket socket;

    private static class Foto {
        final int total;
        final int evento;
        final byte[][] trozos;
        final long inicio = System.currentTimeMillis();
        int recibidos = 0;

        Foto(int total, int evento) {
            this.total = total;
            this.evento = evento;
            this.trozos = new byte[total][];
        }
    }

    public FotoUdpReceiver(int puerto, Listener listener) {
        this.puerto = puerto;
        this.listener = listener;
    }

    public void iniciar() {
        if (corriendo) return;
        corriendo = true;

        new Thread(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(puerto));

                byte[] buffer = new byte[2048];   // cabecera 8 bytes + trozo de 1024
                while (corriendo) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);
                    procesar(paquete.getData(), paquete.getLength());
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
        }).start();
    }

    public void detener() {
        corriendo = false;
        if (socket != null) socket.close();   // desbloquea receive()
    }

    private void procesar(byte[] data, int largo) {
        Log.d("FOTO", "paquete de " + largo + " bytes");
        if (largo < 8) return;

        // Cabecera en little-endian: frame(2) idx(2) total(2) evento(1) reservado(1)
        int frameId = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        int idx = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
        int total = (data[4] & 0xFF) | ((data[5] & 0xFF) << 8);
        int evento = data[6] & 0xFF;

        if (total <= 0 || total > MAX_PAQUETES || idx >= total) return;

        descartarCaducadas();

        Foto foto = fotos.get(frameId);
        if (foto == null || foto.total != total) {
            foto = new Foto(total, evento);
            fotos.put(frameId, foto);
        }

        if (foto.trozos[idx] == null) {   // ignora paquetes duplicados
            foto.trozos[idx] = Arrays.copyOfRange(data, 8, largo);
            foto.recibidos++;
        }

        if (foto.recibidos == foto.total) {
            fotos.remove(frameId);
            byte[] jpeg = unir(foto);
            final int ev = foto.evento;
            final int fid = frameId;
            handlerUI.post(() -> listener.alRecibirFoto(jpeg, ev, fid));
        }
    }

    private byte[] unir(Foto foto) {
        int tam = 0;
        for (byte[] trozo : foto.trozos) tam += trozo.length;

        byte[] jpeg = new byte[tam];
        int pos = 0;
        for (byte[] trozo : foto.trozos) {
            System.arraycopy(trozo, 0, jpeg, pos, trozo.length);
            pos += trozo.length;
        }
        return jpeg;
    }

    private void descartarCaducadas() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Foto>> it = fotos.entrySet().iterator();
        while (it.hasNext()) {
            if (ahora - it.next().getValue().inicio > TIMEOUT_MS) it.remove();
        }
    }
}
