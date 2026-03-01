package com.campus360.identidad.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Clave secreta (mínimo 256 bits para HS256)
    private static final String SECRET = "campus360-g1-identidad-accesos-secret-key-2026-segura";
    private static final long EXPIRACION_MS = 30L * 60 * 1000; // 30 minutos
    private static final long REFRESH_EXPIRACION_MS = 7L * 24 * 60 * 60 * 1000; // 7 días

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // ============ GENERAR TOKEN JWT ============
    public String generarToken(String usuarioId, String correo, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("usuarioId", usuarioId);
        claims.put("correo", correo);
        claims.put("rol", rol);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(correo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRACION_MS))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ============ GENERAR REFRESH TOKEN ============
    public String generarRefreshToken(String usuarioId) {
        return Jwts.builder()
                .setSubject(usuarioId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRACION_MS))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ============ VALIDAR TOKEN ============
    public boolean esValido(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ============ EXTRAER CLAIMS ============
    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extraerCorreo(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerUsuarioId(String token) {
        return (String) extraerClaims(token).get("usuarioId");
    }

    public String extraerRol(String token) {
        return (String) extraerClaims(token).get("rol");
    }

    public Date extraerExpiracion(String token) {
        return extraerClaims(token).getExpiration();
    }
}
