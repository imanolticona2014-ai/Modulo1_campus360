package com.campus360.identidad.controller;

import com.campus360.identidad.service.SessionService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {
    
    private final SessionService sessionService;
    
    public SesionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    @GetMapping("/usuario/{usuarioId}")
    public List<Map<String, Object>> listarSesionesActivas(@PathVariable String usuarioId) {
        return sessionService.obtenerSesionesActivas(usuarioId);
    }
    @GetMapping("/todas")
    public List<Map<String, Object>> listarTodasLasSesiones() {
        return sessionService.obtenerTodasLasSesiones();
    }
    @GetMapping("/buscar")
    public List<Map<String, Object>> buscarSesionesPorUsuario(@RequestParam String termino) {
        return sessionService.buscarSesionesPorTermino(termino);
    }
    
    @DeleteMapping("/{tokenId}")
    public Map<String, String> revocarSesion(@PathVariable Long tokenId) {
        sessionService.revocarSesion(tokenId);
        return Map.of(
            "mensaje", "Sesión revocada exitosamente",
            "tokenId", tokenId.toString()
        );
    }
    
    @DeleteMapping("/usuario/{usuarioId}/todas")
    public Map<String, String> revocarTodasLasSesiones(@PathVariable String usuarioId) {
        sessionService.revocarTodasLasSesiones(usuarioId);
        return Map.of(
            "mensaje", "Todas las sesiones fueron revocadas",
            "usuarioId", usuarioId
        );
    }
    
    @GetMapping("/estadisticas")
    public Map<String, Object> obtenerEstadisticas() {
        return sessionService.obtenerEstadisticasSesiones();
    }
}