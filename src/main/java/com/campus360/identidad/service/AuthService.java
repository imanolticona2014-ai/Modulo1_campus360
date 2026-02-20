package com.campus360.identidad.service;

import com.campus360.identidad.domain.Token;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.TokenRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    
    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;
    
    public AuthService(UsuarioRepository usuarioRepository, TokenRepository tokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
    }
    
    // ============ LOGIN (RF-01) ============
    @Transactional
    public Map<String, Object> login(String correo, String password, String dispositivo, String ip) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        
        // Si no existe, crear uno demo (solo para pruebas)
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setNombres("Usuario");
            usuario.setApellidos("Demo");
            usuario.setPasswordHash("dummy_hash");
            usuarioRepository.save(usuario);
        }
        
        // Generar tokens
        String tokenStr = generarTokenJWT();
        String refreshTokenStr = generarRefreshToken();
        
        // Crear y guardar el token en BD
        Token token = new Token(tokenStr, refreshTokenStr, usuario, dispositivo, ip);
        tokenRepository.save(token);
        
        // Preparar respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenStr);
        response.put("refreshToken", refreshTokenStr);
        response.put("tokenId", token.getId());
        
        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("id", usuario.getId());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("nombre", usuario.getNombres() + " " + 
                      (usuario.getApellidos() != null ? usuario.getApellidos() : ""));
        usuarioMap.put("rol", usuario.getRol() != null ? usuario.getRol().getNombre() : "ESTUDIANTE");
        
        response.put("usuario", usuarioMap);
        response.put("mensaje", "Login exitoso");
        
        return response;
    }
    
    // ============ VALIDAR TOKEN (RF-09) ============
    public Map<String, String> validateToken(String tokenStr) {
        Token token = tokenRepository.findByToken(tokenStr).orElse(null);
        
        Map<String, String> response = new HashMap<>();
        
        if (token == null) {
            response.put("valido", "false");
            response.put("mensaje", "Token no encontrado");
            return response;
        }
        
        if (token.esValido()) {
            response.put("valido", "true");
            response.put("usuarioId", token.getUsuario().getId());
            response.put("rol", token.getUsuario().getRol() != null ? 
                         token.getUsuario().getRol().getNombre() : "ESTUDIANTE");
            response.put("mensaje", "Token válido");
        } else {
            response.put("valido", "false");
            if (token.getRevocado()) {
                response.put("mensaje", "Token revocado");
            } else if (token.estaExpirado()) {
                response.put("mensaje", "Token expirado");
            }
        }
        
        return response;
    }
    
    // ============ REFRESH TOKEN (RF-10) ============
    @Transactional
    public Map<String, String> refreshToken(String refreshToken) {
        Token token = tokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Refresh token inválido"));
        
        if (token.getRevocado()) {
            throw new RuntimeException("Refresh token revocado");
        }
        
        // Verificar expiración (7 días)
        LocalDateTime fechaLimite = token.getFechaCreacion().plusDays(7);
        if (LocalDateTime.now().isAfter(fechaLimite)) {
            throw new RuntimeException("Refresh token expirado");
        }
        
        // Generar nuevo token
        String nuevoToken = generarTokenJWT();
        
        // Crear nuevo registro
        Token nuevoTokenEntity = new Token(
            nuevoToken, 
            refreshToken, 
            token.getUsuario(),
            token.getDispositivo(),
            token.getIpAddress()
        );
        tokenRepository.save(nuevoTokenEntity);
        
        Map<String, String> response = new HashMap<>();
        response.put("token", nuevoToken);
        response.put("tokenId", nuevoTokenEntity.getId().toString());
        response.put("mensaje", "Token renovado exitosamente");
        
        return response;
    }
    
    // ============ LOGOUT (RF-02) ============
    @Transactional
    public Map<String, String> logout(String tokenStr) {
        Token token = tokenRepository.findByToken(tokenStr).orElse(null);
        
        if (token != null) {
            token.setRevocado(true);
            tokenRepository.save(token);
        }
        
        return Map.of("mensaje", "Sesión cerrada exitosamente");
    }
    
    // ============ GESTIÓN DE SESIONES (CU-07) ============
    
    public List<Map<String, Object>> obtenerSesionesActivas(String usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Token> tokens = tokenRepository.findTokensActivosByUsuarioId(usuarioId, ahora);
        
        return tokens.stream()
            .map(token -> {
                Map<String, Object> sesion = new HashMap<>();
                sesion.put("id", token.getId());
                sesion.put("tokenPreview", token.getToken().substring(0, Math.min(20, token.getToken().length())) + "...");
                sesion.put("fechaCreacion", token.getFechaCreacion());
                sesion.put("fechaExpiracion", token.getFechaExpiracion());
                sesion.put("minutosRestantes", 
                    java.time.Duration.between(ahora, token.getFechaExpiracion()).toMinutes());
                sesion.put("dispositivo", token.getDispositivo() != null ? token.getDispositivo() : "Desconocido");
                sesion.put("ip", token.getIpAddress() != null ? token.getIpAddress() : "No registrada");
                return sesion;
            })
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void revocarSesion(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        token.setRevocado(true);
        tokenRepository.save(token);
        
        System.out.println("📢 Evento auditoría: Sesión revocada - Token ID: " + tokenId);
    }
    
    @Transactional
    public void revocarTodasLasSesiones(String usuarioId) {
        tokenRepository.revocarTokensByUsuarioId(usuarioId);
        System.out.println("📢 Evento auditoría: Todas las sesiones revocadas - Usuario ID: " + usuarioId);
    }
    
    public Map<String, Object> obtenerEstadisticasSesiones() {
        List<Token> todos = tokenRepository.findAll();
        LocalDateTime ahora = LocalDateTime.now();
        
        long activas = todos.stream()
            .filter(t -> !t.getRevocado() && t.getFechaExpiracion().isAfter(ahora))
            .count();
        
        long expiradas = todos.stream()
            .filter(t -> t.getFechaExpiracion().isBefore(ahora))
            .count();
        
        long revocadas = todos.stream()
            .filter(Token::getRevocado)
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todos.size());
        stats.put("activas", activas);
        stats.put("expiradas", expiradas);
        stats.put("revocadas", revocadas);
        
        return stats;
    }
    
    // ============ MÉTODOS PRIVADOS ============
    
    private String generarTokenJWT() {
        return "jwt_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
    
    private String generarRefreshToken() {
        return "refresh_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
}