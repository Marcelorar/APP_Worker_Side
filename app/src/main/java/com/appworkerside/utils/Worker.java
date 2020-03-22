package com.appworkerside.utils;

import java.util.Objects;

public class Worker {
    private String username;
    private String Nombre;
    private String Apellido;
    private String foto;

    public Worker(String username, String nombre, String apellido, String foto) {
        this.username = username;
        Nombre = nombre;
        Apellido = apellido;
        this.foto = foto;
    }

    public Worker() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getApellido() {
        return Apellido;
    }

    public void setApellido(String apellido) {
        Apellido = apellido;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Worker)) return false;
        Worker worker = (Worker) o;
        return username.equals(worker.username) &&
                Nombre.equals(worker.Nombre) &&
                Apellido.equals(worker.Apellido) &&
                foto.equals(worker.foto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, Nombre, Apellido, foto);
    }
}

