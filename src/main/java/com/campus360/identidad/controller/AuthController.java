package com.campus360.identidad.controller;

import com.campus360.identidad.service.AuthService;
import com.campus360.identidad.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación (RF-01, RF-02, RF-03, RF-04, RF-10).
 *
 * CORRECCIÓN (Smell 1): Se eliminaron los tres endpoints de debug que
 * exponían información crítica sin autenticación:
 *   - /hash-test    → devolvía hashes BCrypt generados en vivo
 *   - /debug-hash   → exponía el hash real de la BD y resultado del match
 *   - /test-bcrypt  → tenía una contraseña hardcodeada en el código fuente
 *
 * CORRECCIÓN (Smell 3 - SRP): cambiarPassword y recuperarPassword ahora
 * delegan a PasswordService (que el equipo ya había extraído), en lugar
 * de llamar directamente a AuthService.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;

    public AuthController(AuthService authService, PasswordService passwordService) {
        this.authService = authService;
        this.passwordService = passwordService;
    }

    // ============ LOGIN (RF-01) ============
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credenciales,
            HttpServletRequest request) {
        try {
            String correo = credenciales.get("correo");
            String password = credenciales.get("password");
            String dispositivo = request.getHeader("User-Agent");
            String ip = request.getRemoteAddr();
            return ResponseEntity.ok(authService.login(correo, password, dispositivo, ip));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ REFRESH TOKEN (RF-10) ============
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(authService.refreshToken(request.get("refreshToken")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ LOGOUT (RF-02) ============
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            return ResponseEntity.ok(authService.logout(jwt));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ CAMBIAR CONTRASEÑA (RF-04) — delega a PasswordService ============
    @PostMapping("/cambiar-password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> datos) {
        try {
            String jwt = token.replace("Bearer ", "");
            return ResponseEntity.ok(passwordService.cambiarPassword(
                    jwt,
                    datos.get("passwordActual"),
                    datos.get("passwordNueva")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ RECUPERAR CONTRASEÑA (RF-03) — delega a PasswordService ============
    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> recuperarPassword(
            @RequestBody Map<String, String> datos) {
        try {
            return ResponseEntity.ok(passwordService.recuperarPassword(datos.get("correo")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
