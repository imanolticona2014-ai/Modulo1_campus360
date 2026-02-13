package com.campus360.identidad.service;

import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    
    private final UsuarioRepository usuarioRepository;
    
    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
    
    public Map<String, Object> login(String correo, String password) {
        // ENDPOINT STUB - Respuesta dummy
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseGet(() -> new Usuario("999", correo, "Usuario Demo", "ESTUDIANTE"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ");
        response.put("refreshToken", "refresh-" + UUID.randomUUID().toString());
        response.put("usuario", usuario);
        response.put("mensaje", "Login exitoso (STUB)");
        
        return response;
    }
    
    public Map<String, String> validateToken(String token) {
        // ENDPOINT STUB - Respuesta dummy
        Map<String, String> response = new HashMap<>();
        response.put("valido", "true");
        response.put("usuarioId", "1");
        response.put("rol", "ESTUDIANTE");
        response.put("mensaje", "Token válido (STUB)");
        
        return response;
    }
}
