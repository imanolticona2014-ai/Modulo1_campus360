package com.campus360.identidad.config;

import com.campus360.identidad.domain.Rol;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.RolRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public DataInitializer(RolRepository rolRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Crear roles si no existen
        if (rolRepository.count() == 0) {
            Rol estudiante = new Rol("ESTUDIANTE", 
                Arrays.asList("VER_PERFIL", "SOLICITAR_TRAMITE", "VER_MIS_CURSOS"));
            
            Rol docente = new Rol("DOCENTE", 
                Arrays.asList("VER_PERFIL", "VER_CURSOS", "CALIFICAR"));
            
            Rol admin = new Rol("ADMIN", 
                Arrays.asList("ALL", "GESTIONAR_USUARIOS", "GESTIONAR_ROLES", "VER_AUDITORIA"));
            
            rolRepository.saveAll(Arrays.asList(estudiante, docente, admin));
            
            System.out.println("Roles creados en MySQL");
        }

        // Crear usuario de prueba si no existe
        if (!usuarioRepository.existsByCorreo("estudiante@campus360.com")) {
            Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE").orElseThrow();
            
            Usuario usuario = new Usuario();
            usuario.setCorreo("estudianteprueba@campus360.com");
            usuario.setNombres("Estudiante");
            usuario.setApellidos("Prueba");
            usuario.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9"); // "password123"
            usuario.setRol(rolEstudiante);
            usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
            
            usuarioRepository.save(usuario);
            
            System.out.println("Usuario de prueba creado en MySQL");
        }
    }
}