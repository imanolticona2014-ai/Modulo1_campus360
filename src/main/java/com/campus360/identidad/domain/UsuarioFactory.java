package com.campus360.identidad.domain;

import org.springframework.stereotype.Component;

@Component
public class UsuarioFactory {

    // Fabrica un Estudiante con sus valores por defecto
    public Usuario crearEstudiante(String correo, String passwordHash, String nombres, String apellidos, Rol rolEstudiante) {
        return Usuario.builder()
                .correo(correo)
                .passwordHash(passwordHash)
                .nombres(nombres)
                .apellidos(apellidos)
                .rol(rolEstudiante)
                .estado(Usuario.EstadoUsuario.ACTIVO) // Estado por defecto
                .intentosFallidos(0)                  // Empieza limpio
                .build();
    }

    // Fabrica un Docente
    public Usuario crearDocente(String correo, String passwordHash, String nombres, String apellidos, Rol rolDocente) {
        return Usuario.builder()
                .correo(correo)
                .passwordHash(passwordHash)
                .nombres(nombres)
                .apellidos(apellidos)
                .rol(rolDocente)
                .estado(Usuario.EstadoUsuario.ACTIVO)
                .intentosFallidos(0)
                .build();
    }

    // Fabrica un Administrador
    public Usuario crearAdmin(String correo, String passwordHash, String nombres, String apellidos, Rol rolAdmin) {
        return Usuario.builder()
                .correo(correo)
                .passwordHash(passwordHash)
                .nombres(nombres)
                .apellidos(apellidos)
                .rol(rolAdmin)
                .estado(Usuario.EstadoUsuario.ACTIVO)
                .intentosFallidos(0)
                .build();
    }
}