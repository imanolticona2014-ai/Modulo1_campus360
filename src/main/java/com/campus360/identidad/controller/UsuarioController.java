package com.campus360.identidad.controller;

import com.campus360.identidad.service.UsuarioService;
import org.springframework.http.ResponseEntity;
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

    // ============ CREAR USUARIO ============
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearUsuario(@RequestBody Map<String, String> datos) {
        try {
            return ResponseEntity.ok(usuarioService.crearUsuario(datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ LISTAR TODOS LOS USUARIOS ============
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // ============ OBTENER USUARIO POR ID ============
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerUsuario(@PathVariable String id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ============ ACTUALIZAR USUARIO ============
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarUsuario(
            @PathVariable String id,
            @RequestBody Map<String, String> datos) {
        try {
            return ResponseEntity.ok(usuarioService.actualizarUsuario(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ DESACTIVAR USUARIO (baja lógica) ============
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> desactivarUsuario(@PathVariable String id) {
        try {
            return ResponseEntity.ok(usuarioService.desactivarUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ REACTIVAR USUARIO ============
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivarUsuario(@PathVariable String id) {
        try {
            return ResponseEntity.ok(usuarioService.reactivarUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ DESBLOQUEAR USUARIO ============
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<Map<String, String>> desbloquearUsuario(@PathVariable String id) {
        try {
            return ResponseEntity.ok(usuarioService.desbloquearUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ LISTAR ROLES ============
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> listarRoles() {
        return ResponseEntity.ok(usuarioService.listarRoles());
    }

    // ============ ASIGNAR ROL ============
    @PostMapping("/{usuarioId}/roles/{rolId}")
    public ResponseEntity<Map<String, String>> asignarRol(
            @PathVariable String usuarioId,
            @PathVariable String rolId) {
        try {
            return ResponseEntity.ok(usuarioService.asignarRol(usuarioId, rolId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
