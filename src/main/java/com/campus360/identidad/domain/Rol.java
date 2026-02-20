package com.campus360.identidad.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
public class Rol {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String nombre;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", 
                     joinColumns = @JoinColumn(name = "rol_id"))
    @Column(name = "permiso", length = 50)
    private List<String> permisos = new ArrayList<>();
    
    @OneToMany(mappedBy = "rol")
    private List<Usuario> usuarios = new ArrayList<>();
    
    public Rol() {}
    
    public Rol(String nombre, List<String> permisos) {
        this.nombre = nombre;
        this.permisos = permisos;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public List<String> getPermisos() { return permisos; }
    public void setPermisos(List<String> permisos) { this.permisos = permisos; }
    
    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }
}