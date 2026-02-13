package com.campus360.identidad.controller;

import com.campus360.identidad.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credenciales) {
        String correo = credenciales.get("correo");
        String password = credenciales.get("password");
        return authService.login(correo, password);
    }
    
    @GetMapping("/validate")
    public Map<String, String> validateToken(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        return authService.validateToken(jwt);
    }
    
    @PostMapping("/logout")
    public Map<String, String> logout() {
        // ENDPOINT STUB
        return Map.of("mensaje", "Sesión cerrada exitosamente (STUB)");
    }
}
