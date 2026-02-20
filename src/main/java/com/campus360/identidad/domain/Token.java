package com.campus360.identidad.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
public class Token {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 500)
    private String token;
    
    @Column(name = "refresh_token", nullable = false, unique = true, length = 500)
    private String refreshToken;
    
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
    
    @Column(nullable = false)
    private Boolean revocado = false;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    // Campos adicionales para tracking
    private String dispositivo;
    private String ipAddress;
    
    public Token() {}
    
    public Token(String token, String refreshToken, Usuario usuario) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.usuario = usuario;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(30); // 30 min
        this.revocado = false;
    }
    
    public Token(String token, String refreshToken, Usuario usuario, String dispositivo, String ipAddress) {
        this(token, refreshToken, usuario);
        this.dispositivo = dispositivo;
        this.ipAddress = ipAddress;
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    
    public Boolean getRevocado() { return revocado; }
    public void setRevocado(Boolean revocado) { this.revocado = revocado; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    // Métodos útiles
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }
    
    public boolean esValido() {
        return !revocado && !estaExpirado();
    }
}