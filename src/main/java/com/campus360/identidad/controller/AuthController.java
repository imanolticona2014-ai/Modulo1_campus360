package com.campus360.identidad.controller;

import com.campus360.identidad.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public Map<String, Object> login(@RequestBody Map<String, String> credenciales, 
                                     HttpServletRequest request) {
        String correo = credenciales.get("correo");
        String password = credenciales.get("password");
        String dispositivo = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        
        return authService.login(correo, password, dispositivo, ip);
    }
    
    @PostMapping("/refresh")
    public Map<String, String> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return authService.refreshToken(refreshToken);
    }
    
    @GetMapping("/validate")
    public Map<String, String> validateToken(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        return authService.validateToken(jwt);
    }
    
    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        return authService.logout(jwt);
    }
}