package com.campus360.identidad.service;

import com.campus360.identidad.domain.Rol;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.RolRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    
    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }
    
    public Map<String, Object> crearUsuario(Map<String, String> datos) {
        // ENDPOINT STUB - Respuesta dummy
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID().toString());
        usuario.setCorreo(datos.get("correo"));
        usuario.setNombres(datos.get("nombre"));
        usuario.setApellidos(datos.get("apellidos") != null ? datos.get("apellidos") : "");
        
        // Buscar rol por nombre
        String rolNombre = datos.get("rol");
        Optional<Rol> rolOpt = rolRepository.findByNombre(rolNombre);
        if (rolOpt.isPresent()) {
            usuario.setRol(rolOpt.get());
        }
        
        usuario.setPasswordHash("$2a$10$dummyhashparapruebas");
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        
        Map<String, Object> response = new HashMap<>();
        
        // Crear mapa de respuesta simple
        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("id", usuario.getId());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("nombre", usuario.getNombres());
        usuarioMap.put("rol", rolNombre);
        
        response.put("usuario", usuarioMap);
        response.put("mensaje", "Usuario creado exitosamente (STUB)");
        
        return response;
    }
    
    public List<Map<String, Object>> listarRoles() {
        // ENDPOINT STUB - Respuesta dummy
        List<Map<String, Object>> rolesList = new ArrayList<>();
        
        // Rol ESTUDIANTE
        Map<String, Object> estudiante = new HashMap<>();
        estudiante.put("id", "1");
        estudiante.put("nombre", "ESTUDIANTE");
        estudiante.put("permisos", Arrays.asList("VER_PERFIL", "SOLICITAR_TRAMITE"));
        rolesList.add(estudiante);
        
        // Rol ADMIN
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", "2");
        admin.put("nombre", "ADMIN");
        admin.put("permisos", Arrays.asList("ALL"));
        rolesList.add(admin);
        
        // Rol DOCENTE
        Map<String, Object> docente = new HashMap<>();
        docente.put("id", "3");
        docente.put("nombre", "DOCENTE");
        docente.put("permisos", Arrays.asList("VER_PERFIL", "VER_CURSOS"));
        rolesList.add(docente);
        
        return rolesList;
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