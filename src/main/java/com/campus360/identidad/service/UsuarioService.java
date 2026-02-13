package com.campus360.identidad.service;

import com.campus360.identidad.domain.Rol;
import com.campus360.identidad.domain.Usuario;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioService {
    
    public Map<String, Object> crearUsuario(Map<String, String> datos) {
        // ENDPOINT STUB - Respuesta dummy
        Usuario usuario = new Usuario(
            UUID.randomUUID().toString(),
            datos.get("correo"),
            datos.get("nombre"),
            datos.get("rol")
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("usuario", usuario);
        response.put("mensaje", "Usuario creado exitosamente (STUB)");
        
        return response;
    }
    
    public List<Rol> listarRoles() {
        // ENDPOINT STUB - Respuesta dummy
        return Arrays.asList(
            new Rol("1", "ESTUDIANTE", new String[]{"VER_PERFIL", "SOLICITAR_TRAMITE"}),
            new Rol("2", "ADMIN", new String[]{"ALL"}),
            new Rol("3", "DOCENTE", new String[]{"VER_PERFIL", "VER_CURSOS"})
        );
    }
    
    public Map<String, String> asignarRol(String usuarioId, String rolId) {
        // ENDPOINT STUB - Respuesta dummy
        Map<String, String> response = new HashMap<>();
        response.put("usuarioId", usuarioId);
        response.put("rolId", rolId);
        response.put("estado", "ASIGNADO");
        response.put("mensaje", "Rol asignado exitosamente (STUB)");
        
        return response;
    }
}
