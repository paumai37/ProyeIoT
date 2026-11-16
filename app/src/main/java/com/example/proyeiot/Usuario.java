package com.example.proyeiot;

public class Usuario {
    public final String nombre;
    public final String rol;   // "ADMIN" o "USUARIO"

    public Usuario(String nombre, String rol) {
        this.nombre = nombre;
        this.rol = rol;
    }
}