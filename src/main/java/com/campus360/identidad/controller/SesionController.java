package com.campus360.identidad.controller;

import com.campus360.identidad.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {
    
    private final AuthService authService;
    
    public SesionController(AuthService authService) {
        this.authService = authService;
    }
    
    @GetMapping("/usuario/{usuarioId}")
    public List<Map<String, Object>> listarSesionesActivas(@PathVariable String usuarioId) {
        return authService.obtenerSesionesActivas(usuarioId);
    }
    
    @DeleteMapping("/{tokenId}")
    public Map<String, String> revocarSesion(@PathVariable Long tokenId) {
        authService.revocarSesion(tokenId);
        return Map.of(
            "mensaje", "Sesión revocada exitosamente",
            "tokenId", tokenId.toString()
        );
    }
    
    @DeleteMapping("/usuario/{usuarioId}/todas")
    public Map<String, String> revocarTodasLasSesiones(@PathVariable String usuarioId) {
        authService.revocarTodasLasSesiones(usuarioId);
        return Map.of(
            "mensaje", "Todas las sesiones fueron revocadas",
            "usuarioId", usuarioId
        );
    }
    
    @GetMapping("/estadisticas")
    public Map<String, Object> obtenerEstadisticas() {
        return authService.obtenerEstadisticasSesiones();
    }
}