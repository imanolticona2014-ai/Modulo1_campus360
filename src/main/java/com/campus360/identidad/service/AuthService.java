package com.campus360.identidad.service;

import com.campus360.identidad.config.AuthConstants;
import com.campus360.identidad.domain.Token;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.exception.AccesoNoAutorizadoException;
import com.campus360.identidad.repository.TokenRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

        Usuario usuario = obtenerUsuario(correo, ip);

        validarEstadoUsuario(usuario, correo, ip);
        validarPassword(usuario, password, correo, ip);

        return procesarLoginExitoso(usuario, dispositivo, ip);
    }

    // ============ REFRESH TOKEN (RF-10) — CORREGIDO ============
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

        // Generar NUEVO token y NUEVO refreshToken
        String nuevoToken = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), rol);
        String nuevoRefreshToken = jwtService.generarRefreshToken(usuario.getId());

        // Revocar el token anterior
        token.setRevocado(true);
        tokenRepository.save(token);

        // Crear nuevo registro con tokens completamente nuevos
        Token nuevoTokenEntity = new Token(nuevoToken, nuevoRefreshToken, usuario,
                token.getDispositivo(), token.getIpAddress());
        tokenRepository.save(nuevoTokenEntity);

        Map<String, String> response = new HashMap<>();
        response.put("token", nuevoToken);
        response.put("refreshToken", nuevoRefreshToken);
        response.put("mensaje", "Token renovado exitosamente");
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

    // ============ MÉTODOS PRIVADOS ============
    private Usuario obtenerUsuario(String correo, String ip) {
    Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

    if (usuario == null) {
        auditoriaClient.registrar("LOGIN_FALLIDO", correo, ip, "Usuario no existe");
        throw new AccesoNoAutorizadoException("Credenciales inválidas");
    }

    return usuario;
}

    private void validarEstadoUsuario(Usuario usuario, String correo, String ip) {

        if (usuario.getEstado() == Usuario.EstadoUsuario.BLOQUEADO) {
            if (usuario.getBloqueoHasta() != null && LocalDateTime.now().isBefore(usuario.getBloqueoHasta())) {
                long minutosRestantes = java.time.Duration
                        .between(LocalDateTime.now(), usuario.getBloqueoHasta())
                        .toMinutes() + 1;

                throw new AccesoNoAutorizadoException(
                        "Cuenta bloqueada temporalmente. Intente en " + minutosRestantes + " minutos.");
            } else {
                usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
                usuario.setIntentosFallidos(0);
                usuario.setBloqueoHasta(null);
                usuarioRepository.save(usuario);
            }
        }

        if (usuario.getEstado() == Usuario.EstadoUsuario.INACTIVO) {
            auditoriaClient.registrar("LOGIN_FALLIDO", correo, ip, "Usuario inactivo");
            throw new AccesoNoAutorizadoException("Credenciales inválidas");
        }
    }

    private void validarPassword(Usuario usuario, String password, String correo, String ip) {

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {

            int intentos = usuario.getIntentosFallidos() + 1;

            if (intentos >= AuthConstants.MAX_INTENTOS) {
                authPersistenceService.bloquearCuenta(usuario, ip);
                throw new AccesoNoAutorizadoException(
                    "Cuenta bloqueada por " + AuthConstants.MAX_INTENTOS +
                    " intentos fallidos. Espere " + AuthConstants.MINUTOS_BLOQUEO + " minutos.");
            }

            authPersistenceService.guardarIntentoFallido(usuario, intentos, correo, ip);
            throw new AccesoNoAutorizadoException("Credenciales inválidas");
        }
    }

    private Map<String, Object> procesarLoginExitoso(
            Usuario usuario, String dispositivo, String ip) {

        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        String rol = usuario.getRol() != null
                ? usuario.getRol().getNombre()
                : "ESTUDIANTE";

        String tokenStr = jwtService.generarToken(
                usuario.getId(), usuario.getCorreo(), rol);

        String refreshTokenStr =
                jwtService.generarRefreshToken(usuario.getId());

        Token token = new Token(tokenStr, refreshTokenStr, usuario, dispositivo, ip);
        tokenRepository.save(token);

        auditoriaClient.registrar(
                "LOGIN_EXITOSO", usuario.getCorreo(), ip,
                "Login correcto - Rol: " + rol);

        return construirRespuesta(usuario, rol, tokenStr, refreshTokenStr, token.getId());
    }

    private Map<String, Object> construirRespuesta(
            Usuario usuario,
            String rol,
            String tokenStr,
            String refreshTokenStr,
            Long tokenId) {

        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenStr);
        response.put("refreshToken", refreshTokenStr);
        response.put("tokenId", tokenId);

        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("id", usuario.getId());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("nombre", usuario.getNombres() + " " +
                (usuario.getApellidos() != null ? usuario.getApellidos() : ""));
        usuarioMap.put("rol", rol);

        response.put("usuario", usuarioMap);
        response.put("mensaje", "Login exitoso");

        return response;
    }
}
