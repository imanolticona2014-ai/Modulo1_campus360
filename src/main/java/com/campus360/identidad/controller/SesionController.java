package com.campus360.identidad.controller;

import com.campus360.identidad.service.SessionService;
import com.campus360.identidad.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de sesiones activas (CU-07).
 *
 * CORRECCIÓN (Smell 3): Ahora depende de SessionService (que ya existía
 * en el repo) y TokenService, en lugar de AuthService. Cada controlador
 * habla con su propio servicio especializado.
 *
 * NOTA: El nombre del archivo se mantiene como SesionController.java
 * (con acento en la carpeta) para no romper la URL /api/v1/sesiones
 * que ya está definida en el proyecto.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {

    private final SessionService sessionService;
    private final TokenService tokenService;

    public SesionController(SessionService sessionService, TokenService tokenService) {
        this.sessionService = sessionService;
        this.tokenService = tokenService;
    }

    // ============ VALIDAR TOKEN (RF-09) ============
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        return ResponseEntity.ok(tokenService.validateToken(jwt));
    }

    // ============ SESIONES DE UN USUARIO ============
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Map<String, Object>>> listarSesionesActivas(
            @PathVariable String usuarioId) {
        return ResponseEntity.ok(sessionService.obtenerSesionesActivas(usuarioId));
    }

    // ============ TODAS LAS SESIONES ============
    @GetMapping("/todas")
    public ResponseEntity<List<Map<String, Object>>> listarTodasLasSesiones() {
        return ResponseEntity.ok(sessionService.obtenerTodasLasSesiones());
    }

    // ============ BUSCAR SESIONES ============
    @GetMapping("/buscar")
    public ResponseEntity<List<Map<String, Object>>> buscarSesionesPorUsuario(
            @RequestParam String termino) {
        return ResponseEntity.ok(sessionService.buscarSesionesPorTermino(termino));
    }

    // ============ REVOCAR SESIÓN ============
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Map<String, String>> revocarSesion(@PathVariable Long tokenId) {
        sessionService.revocarSesion(tokenId);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Sesión revocada exitosamente",
            "tokenId", tokenId.toString()
        ));
    }

    // ============ REVOCAR TODAS LAS SESIONES DE UN USUARIO ============
    @DeleteMapping("/usuario/{usuarioId}/todas")
    public ResponseEntity<Map<String, String>> revocarTodasLasSesiones(
            @PathVariable String usuarioId) {
        sessionService.revocarTodasLasSesiones(usuarioId);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Todas las sesiones fueron revocadas",
            "usuarioId", usuarioId
        ));
    }

    // ============ ESTADÍSTICAS ============
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        return ResponseEntity.ok(sessionService.obtenerEstadisticasSesiones());
    }
}
