package com.campus360.identidad.domain;

public class Rol {
    private String id;
    private String nombre;
    private String[] permisos;
    
    public Rol() {}
    
    public Rol(String id, String nombre, String[] permisos) {
        this.id = id;
        this.nombre = nombre;
        this.permisos = permisos;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String[] getPermisos() { return permisos; }
    public void setPermisos(String[] permisos) { this.permisos = permisos; }
}
