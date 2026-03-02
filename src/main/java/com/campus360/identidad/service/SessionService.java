package com.campus360.identidad.service;

import com.campus360.identidad.domain.Token;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.exception.RecursoNoEncontradoException;
import com.campus360.identidad.repository.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final TokenRepository tokenRepository;
    private final AuditoriaClient auditoriaClient;

    public SessionService(TokenRepository tokenRepository,
                         AuditoriaClient auditoriaClient) {
        this.tokenRepository = tokenRepository;
        this.auditoriaClient = auditoriaClient;
    }

    // ============ GESTIÓN DE SESIONES (CU-07) ============
    public List<Map<String, Object>> obtenerSesionesActivas(String usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        return tokenRepository.findTokensActivosByUsuarioId(usuarioId, ahora)
                .stream().map(this::tokenToMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerTodasLasSesiones() {
        LocalDateTime ahora = LocalDateTime.now();
        return tokenRepository.findAll().stream()
                .filter(t -> !t.getRevocado() && t.getFechaExpiracion().isAfter(ahora))
                .map(this::tokenToMap)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buscarSesionesPorTermino(String termino) {
        LocalDateTime ahora = LocalDateTime.now();
        return tokenRepository.findAll().stream()
                .filter(t -> !t.getRevocado() && t.getFechaExpiracion().isAfter(ahora))
                .filter(t -> {
                    Usuario u = t.getUsuario();
                    String b = termino.toLowerCase();
                    return u.getId().toLowerCase().contains(b) ||
                            u.getCorreo().toLowerCase().contains(b) ||
                            (u.getNombres() + " " + u.getApellidos()).toLowerCase().contains(b);
                })
                .map(this::tokenToMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revocarSesion(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        token.setRevocado(true);
        tokenRepository.save(token);
        auditoriaClient.registrar("SESION_REVOCADA", token.getUsuario().getCorreo(),
                "N/A", "Sesión revocada por admin - Token ID: " + tokenId);
    }

    @Transactional
    public void revocarTodasLasSesiones(String usuarioId) {
        tokenRepository.revocarTokensByUsuarioId(usuarioId);
        auditoriaClient.registrar("TODAS_SESIONES_REVOCADAS", usuarioId,
                "N/A", "Todas las sesiones revocadas por admin");
    }

    public Map<String, Object> obtenerEstadisticasSesiones() {
        List<Token> todos = tokenRepository.findAll();
        LocalDateTime ahora = LocalDateTime.now();

        long activas = 0, expiradas = 0, revocadas = 0;
        for (Token t : todos) {
            if (t.getRevocado()) revocadas++;
            else if (t.getFechaExpiracion().isBefore(ahora)) expiradas++;
            else activas++;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todos.size());
        stats.put("activas", activas);
        stats.put("expiradas", expiradas);
        stats.put("revocadas", revocadas);
        return stats;
    }

    // ============ MÉTODO PRIVADO ============
    private Map<String, Object> tokenToMap(Token token) {
        LocalDateTime ahora = LocalDateTime.now();
        Map<String, Object> sesion = new HashMap<>();
        sesion.put("id", token.getId());
        sesion.put("tokenPreview", token.getToken().substring(0, Math.min(30, token.getToken().length())) + "...");
        sesion.put("usuarioId", token.getUsuario().getId());
        sesion.put("usuarioCorreo", token.getUsuario().getCorreo());
        sesion.put("usuarioNombre", token.getUsuario().getNombres() + " " + token.getUsuario().getApellidos());
        sesion.put("fechaCreacion", token.getFechaCreacion());
        sesion.put("fechaExpiracion", token.getFechaExpiracion());
        sesion.put("minutosRestantes", java.time.Duration.between(ahora, token.getFechaExpiracion()).toMinutes());
        sesion.put("dispositivo", token.getDispositivo() != null ? token.getDispositivo() : "Desconocido");
        sesion.put("ip", token.getIpAddress() != null ? token.getIpAddress() : "No registrada");
        return sesion;
    }
}
