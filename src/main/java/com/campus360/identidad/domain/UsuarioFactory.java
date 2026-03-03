package com.campus360.identidad.domain;

import org.springframework.stereotype.Component;

/**
 * Fábrica de usuarios — Patrón Factory Method (Sección 8.1 del entregable).
 *
 * CORRECCIÓN (Smell 5 - Duplicate Code): Los tres métodos originales tenían
 * exactamente el mismo cuerpo, lo que hacía que el patrón Factory no
 * aportara valor real. Se unificó la lógica en un método base privado
 * (crearUsuarioBase) y los métodos públicos pueden extenderlo cuando los
 * roles tengan reglas realmente distintas.
 *
 * Ejemplo de cómo extender en el futuro sin duplicar:
 * Si los docentes deben empezar INACTIVOS hasta validar su correo
 * institucional, solo se modifica crearDocente() aquí, sin tocar
 * UsuarioService ni ningún otro componente.
 */
@Component
public class UsuarioFactory {

    /** Crea un estudiante con el estado por defecto. */
    public Usuario crearEstudiante(String correo, String passwordHash,
                                   String nombres, String apellidos, Rol rol) {
        return crearUsuarioBase(correo, passwordHash, nombres, apellidos, rol,
                Usuario.EstadoUsuario.ACTIVO);
    }

    /**
     * Crea un docente.
     * En el futuro, si los docentes requieren estado INACTIVO hasta verificar
     * su correo, el cambio se hace únicamente aquí.
     */
    public Usuario crearDocente(String correo, String passwordHash,
                                String nombres, String apellidos, Rol rol) {
        return crearUsuarioBase(correo, passwordHash, nombres, apellidos, rol,
                Usuario.EstadoUsuario.ACTIVO);
    }

    /**
     * Crea un administrador.
     * En el futuro podría requerir un proceso de aprobación adicional
     * antes de quedar ACTIVO.
     */
    public Usuario crearAdmin(String correo, String passwordHash,
                              String nombres, String apellidos, Rol rol) {
        return crearUsuarioBase(correo, passwordHash, nombres, apellidos, rol,
                Usuario.EstadoUsuario.ACTIVO);
    }

    // ============ MÉTODO BASE PRIVADO ============

    /**
     * Lógica compartida de construcción de un Usuario.
     *
     * CORRECCIÓN (Smell 5): los tres métodos públicos eran copias exactas
     * del mismo código. Este método privado centraliza la construcción y
     * elimina la duplicación. Si la firma del Builder cambia, solo se
     * actualiza aquí.
     */
    private Usuario crearUsuarioBase(String correo, String passwordHash,
                                     String nombres, String apellidos,
                                     Rol rol, Usuario.EstadoUsuario estado) {
        return Usuario.builder()
                .correo(correo)
                .passwordHash(passwordHash)
                .nombres(nombres)
                .apellidos(apellidos)
                .rol(rol)
                .estado(estado)
                .intentosFallidos(0)
                .build();
    }
}
