package com.campus360.identidad.domain;

public class Usuario {
    private String id;
    private String correo;
    private String nombre;
    private String rol;
    
    public Usuario() {}
    
    public Usuario(String id, String correo, String nombre, String rol) {
        this.id = id;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
