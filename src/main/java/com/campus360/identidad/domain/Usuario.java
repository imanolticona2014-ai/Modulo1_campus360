package com.campus360.identidad.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    private String id;  // Mantenemos String por compatibilidad
    
    @Column(unique = true, nullable = false, length = 100)
    private String correo;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(nullable = false, length = 50)
    private String nombres;
    
    @Column(length = 50)
    private String apellidos;
    
    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;
    
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;
    
    @Column(name = "bloqueo_hasta")
    private LocalDateTime bloqueoHasta;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relación con tokens (uno a muchos)
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();
    
    public Usuario() {}
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    
    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    
    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    
    public LocalDateTime getBloqueoHasta() { return bloqueoHasta; }
    public void setBloqueoHasta(LocalDateTime bloqueoHasta) { this.bloqueoHasta = bloqueoHasta; }
    
    public List<Token> getTokens() { return tokens; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }
    
    public enum EstadoUsuario {
        ACTIVO, INACTIVO, BLOQUEADO
    }

    // ==========================================
    // INICIO DEL PATRÓN BUILDER
    // ==========================================

    // 1. Constructor privado que usa el Builder
    private Usuario(UsuarioBuilder builder) {
        this.id = builder.id;
        this.correo = builder.correo;
        this.passwordHash = builder.passwordHash;
        this.nombres = builder.nombres;
        this.apellidos = builder.apellidos;
        this.rol = builder.rol;
        this.estado = builder.estado != null ? builder.estado : EstadoUsuario.ACTIVO;
        this.intentosFallidos = builder.intentosFallidos != null ? builder.intentosFallidos : 0;
        this.bloqueoHasta = builder.bloqueoHasta;
    }

    // 2. Método estático de acceso
    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }

    // 3. Clase estática Builder
    public static class UsuarioBuilder {
        private String id;
        private String correo;
        private String passwordHash;
        private String nombres;
        private String apellidos;
        private Rol rol;
        private EstadoUsuario estado = EstadoUsuario.ACTIVO;
        private Integer intentosFallidos = 0;
        private LocalDateTime bloqueoHasta;

        public UsuarioBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UsuarioBuilder correo(String correo) {
            this.correo = correo;
            return this;
        }

        public UsuarioBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UsuarioBuilder nombres(String nombres) {
            this.nombres = nombres;
            return this;
        }

        public UsuarioBuilder apellidos(String apellidos) {
            this.apellidos = apellidos;
            return this;
        }

        public UsuarioBuilder rol(Rol rol) {
            this.rol = rol;
            return this;
        }

        public UsuarioBuilder estado(EstadoUsuario estado) {
            this.estado = estado;
            return this;
        }

        public UsuarioBuilder intentosFallidos(Integer intentosFallidos) {
            this.intentosFallidos = intentosFallidos;
            return this;
        }

        public UsuarioBuilder bloqueoHasta(LocalDateTime bloqueoHasta) {
            this.bloqueoHasta = bloqueoHasta;
            return this;
        }

        public Usuario build() {
            return new Usuario(this);
        }
    }
    // ==========================================
    // FIN DEL PATRÓN BUILDER
    // ==========================================
}