package com.campus360.identidad.controller;

import com.campus360.identidad.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    
    @PostMapping
    public Map<String, Object> crearUsuario(@RequestBody Map<String, String> datos) {
        return usuarioService.crearUsuario(datos);
    }
    
    @GetMapping("/roles")
    public List<Map<String, Object>> listarRoles() {
        return usuarioService.listarRoles();
    }
    
    @PostMapping("/{usuarioId}/roles/{rolId}")
    public Map<String, String> asignarRol(
            @PathVariable String usuarioId,
            @PathVariable String rolId) {
        return usuarioService.asignarRol(usuarioId, rolId);
    }
}