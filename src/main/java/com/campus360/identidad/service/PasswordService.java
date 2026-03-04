package com.campus360.identidad.service;

import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.exception.AccesoNoAutorizadoException;
import com.campus360.identidad.exception.RecursoNoEncontradoException;
import com.campus360.identidad.exception.ReglaNegocioException;
import com.campus360.identidad.repository.TokenRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Servicio de gestión de contraseñas (RF-03, RF-04).
 *
 * CORRECCIÓN CU-03: recuperarPassword() ahora llama a
 * notificacionClient.enviarRecuperacionPassword() para delegar
 * el envío del correo al módulo G7, cumpliendo el paso 4 del
 * flujo principal del CU-03 y ejecutando el CU-09 incluido.
 * Antes solo hacía un System.out.println sin integración real.
 */
@Service
public class PasswordService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaClient auditoriaClient;
    private final NotificacionClient notificacionClient;

    public PasswordService(UsuarioRepository usuarioRepository,
                           TokenRepository tokenRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           AuditoriaClient auditoriaClient,
                           NotificacionClient notificacionClient) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaClient = auditoriaClient;
        this.notificacionClient = notificacionClient;
    }

    // ============ CAMBIAR CONTRASEÑA (RF-04) ============
    @Transactional
    public Map<String, String> cambiarPassword(String tokenStr,
                                                String passwordActual,
                                                String passwordNueva) {
        if (!jwtService.esValido(tokenStr)) {
            throw new AccesoNoAutorizadoException("Token inválido");
        }

        String correo = jwtService.extraerCorreo(tokenStr);
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual es incorrecta");
        }

        validarPoliticaPassword(passwordNueva);

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        tokenRepository.revocarTokensByUsuarioId(usuario.getId());

        auditoriaClient.registrar("CAMBIO_PASSWORD", correo, "N/A", "Contraseña actualizada");

        return Map.of("mensaje",
                "Contraseña actualizada exitosamente. Por favor inicie sesión nuevamente.");
    }

    // ============ RECUPERAR CONTRASEÑA (RF-03) ============
    public Map<String, String> recuperarPassword(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null || usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            auditoriaClient.registrar("RECUPERACION_PASSWORD", correo, "N/A",
                    "Usuario no disponible");
            // Respuesta genérica — no revela si el correo existe (RNF-09)
            return Map.of("mensaje", "Si el correo existe, recibirás instrucciones en breve.");
        }

        String tokenRecuperacion = jwtService.generarToken(
                usuario.getId(), correo, "RECUPERACION");

        // CORRECCIÓN CU-03: delega el envío del correo al módulo G7
        // cumpliendo el flujo principal paso 4 y el CU-09 incluido
        notificacionClient.enviarRecuperacionPassword(
                correo,
                usuario.getNombres(),
                tokenRecuperacion
        );

        auditoriaClient.registrar("RECUPERACION_PASSWORD", correo, "N/A",
                "Token de recuperación generado y notificación enviada a G7");

        return Map.of("mensaje", "Si el correo existe, recibirás instrucciones en breve.");
    }

    // ============ MÉTODO PRIVADO ============
    private void validarPoliticaPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ReglaNegocioException(
                    "La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ReglaNegocioException(
                    "La contraseña debe tener al menos una letra mayúscula");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ReglaNegocioException(
                    "La contraseña debe tener al menos un número");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new ReglaNegocioException(
                    "La contraseña debe tener al menos un carácter especial");
        }
    }
}
