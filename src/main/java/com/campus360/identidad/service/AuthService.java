package com.campus360.identidad.service;

import com.campus360.identidad.config.AuthConstants;
import com.campus360.identidad.domain.Token;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.exception.AccesoNoAutorizadoException;
import com.campus360.identidad.exception.ReglaNegocioException;
import com.campus360.identidad.repository.TokenRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de autenticación (RF-01, RF-02, RF-09, RF-10).
 *
 * CORRECCIONES APLICADAS:
 *
 * Smell 3 - SRP: Esta clase ya no contiene lógica de sesiones (está en
 * SessionService), ni lógica de contraseñas (está en PasswordService),
 * ni validación de token (está en TokenService). AuthService ahora solo
 * maneja: login, logout y refresh token.
 *
 * Magic Numbers eliminados: MAX_INTENTOS y MINUTOS_BLOQUEO se leen de
 * AuthConstants en lugar de estar hardcodeados aquí.
 *
 * Excepciones propias: se usan AccesoNoAutorizadoException y
 * ReglaNegocioException en lugar de RuntimeException genérico.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaClient auditoriaClient;
    private final AuthPersistenceService authPersistenceService;

    public AuthService(UsuarioRepository usuarioRepository,
                       TokenRepository tokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuditoriaClient auditoriaClient,
                       AuthPersistenceService authPersistenceService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaClient = auditoriaClient;
        this.authPersistenceService = authPersistenceService;
    }

    // ============ LOGIN (RF-01) ============
    @Transactional
    public Map<String, Object> login(String correo, String password, String dispositivo, String ip) {

        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null) {
            auditoriaClient.registrar("LOGIN_FALLIDO", correo, ip, "Usuario no existe");
            throw new AccesoNoAutorizadoException("Credenciales inválidas");
        }

        // 2. Verificar si está bloqueado (con desbloqueo automático si ya pasó el tiempo)
        if (usuario.getEstado() == Usuario.EstadoUsuario.BLOQUEADO) {
            if (usuario.getBloqueoHasta() != null
                    && LocalDateTime.now().isBefore(usuario.getBloqueoHasta())) {
                long minutosRestantes = java.time.Duration
                        .between(LocalDateTime.now(), usuario.getBloqueoHasta()).toMinutes() + 1;
                throw new ReglaNegocioException(
                        "Cuenta bloqueada temporalmente. Intente en " + minutosRestantes + " minutos.");
            } else {
                usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
                usuario.setIntentosFallidos(0);
                usuario.setBloqueoHasta(null);
                usuarioRepository.save(usuario);
            }
        }

        // 3. Verificar si está inactivo
        if (usuario.getEstado() == Usuario.EstadoUsuario.INACTIVO) {
            auditoriaClient.registrar("LOGIN_FALLIDO", correo, ip, "Usuario inactivo");
            throw new AccesoNoAutorizadoException("Credenciales inválidas");
        }

        // 4. Verificar contraseña con BCrypt
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;

            if (intentos >= AuthConstants.MAX_INTENTOS) {
                authPersistenceService.bloquearCuenta(usuario, ip);
                throw new ReglaNegocioException("Cuenta bloqueada por " + AuthConstants.MAX_INTENTOS
                        + " intentos fallidos. Espere " + AuthConstants.MINUTOS_BLOQUEO + " minutos.");
            }

            authPersistenceService.guardarIntentoFallido(usuario, intentos, correo, ip);
            throw new AccesoNoAutorizadoException("Credenciales inválidas");
        }

        // 5. Login exitoso — resetear intentos
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        // 6. Generar tokens JWT
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "ESTUDIANTE";
        String tokenStr = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), rol);
        String refreshTokenStr = jwtService.generarRefreshToken(usuario.getId());

        // 7. Guardar token en BD
        Token token = new Token(tokenStr, refreshTokenStr, usuario, dispositivo, ip);
        tokenRepository.save(token);

        // 8. Registrar en auditoría
        auditoriaClient.registrar("LOGIN_EXITOSO", correo, ip, "Login correcto - Rol: " + rol);

        // 9. Preparar respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenStr);
        response.put("refreshToken", refreshTokenStr);
        response.put("tokenId", token.getId());

        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("id", usuario.getId());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("nombre",
                usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""));
        usuarioMap.put("rol", rol);

        response.put("usuario", usuarioMap);
        response.put("mensaje", "Login exitoso");

        return response;
    }

    // ============ LOGOUT (RF-02) ============
    @Transactional
    public Map<String, String> logout(String tokenStr) {
        Token token = tokenRepository.findByToken(tokenStr).orElse(null);

        if (token != null) {
            token.setRevocado(true);
            tokenRepository.save(token);
            auditoriaClient.registrar("LOGOUT", token.getUsuario().getCorreo(),
                    token.getIpAddress(), "Sesión cerrada");
        }

        return Map.of("mensaje", "Sesión cerrada exitosamente");
    }

    // ============ REFRESH TOKEN (RF-10) ============
    @Transactional
    public Map<String, String> refreshToken(String refreshTokenStr) {
        Token token = tokenRepository.findByRefreshToken(refreshTokenStr)
                .orElseThrow(() -> new AccesoNoAutorizadoException("Refresh token inválido"));

        if (token.getRevocado()) {
            throw new AccesoNoAutorizadoException("Refresh token revocado");
        }

        if (!jwtService.esValido(refreshTokenStr)) {
            throw new AccesoNoAutorizadoException("Refresh token expirado");
        }

        Usuario usuario = token.getUsuario();
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "ESTUDIANTE";

        String nuevoToken = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), rol);
        String nuevoRefreshToken = jwtService.generarRefreshToken(usuario.getId());

        token.setRevocado(true);
        tokenRepository.save(token);

        Token nuevoTokenEntity = new Token(nuevoToken, nuevoRefreshToken, usuario,
                token.getDispositivo(), token.getIpAddress());
        tokenRepository.save(nuevoTokenEntity);

        Map<String, String> response = new HashMap<>();
        response.put("token", nuevoToken);
        response.put("refreshToken", nuevoRefreshToken);
        response.put("mensaje", "Token renovado exitosamente");
        return response;
    }
}
