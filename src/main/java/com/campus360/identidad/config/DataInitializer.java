package com.campus360.identidad.config;

import com.campus360.identidad.repository.RolRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        // Los datos iniciales se cargan desde el script.sql
        // Este inicializador solo verifica que la BD esté lista
        long totalRoles = rolRepository.count();
        long totalUsuarios = usuarioRepository.count();

        System.out.println("==============================================");
        System.out.println("  Campus360 - G1 Identidad y Accesos");
        System.out.println("==============================================");
        System.out.println("  Roles cargados    : " + totalRoles);
        System.out.println("  Usuarios cargados : " + totalUsuarios);
        System.out.println("==============================================");
        System.out.println("  Usuarios de prueba (contraseña: 123456):");
        System.out.println("  - admin.sistema@campus360.com   [ADMIN]");
        System.out.println("  - estudiante@campus360.com      [ESTUDIANTE]");
        System.out.println("  - profesor.ana@campus360.com    [DOCENTE]");
        System.out.println("  - bloqueado@campus360.com       [BLOQUEADO]");
        System.out.println("  - inactivo@campus360.com        [INACTIVO]");
        System.out.println("==============================================");
    }
}
