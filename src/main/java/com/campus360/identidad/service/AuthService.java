package com.campus360.identidad.service;

import com.campus360.identidad.domain.Token;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.exception.AccesoNoAutorizadoException;
import com.campus360.identidad.exception.RecursoNoEncontradoException;
import com.campus360.identidad.exception.ReglaNegocioException;
import com.campus360.identidad.repository.TokenRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaClient auditoriaClient;
    private final AuthPersistenceService authPersistenceService;

    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_BLOQUEO = 15;

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


    // ============ VALIDAR TOKEN (RF-09) ============
    public Map<String, Object> validateToken(String tokenStr) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtService.esValido(tokenStr)) {
            response.put("valido", false);
            response.put("mensaje", "Token inválido o expirado");
            return response;
        }

        Token token = tokenRepository.findByToken(tokenStr).orElse(null);
        if (token == null || token.getRevocado()) {
            response.put("valido", false);
            response.put("mensaje", "Token revocado");
            return response;
        }

        response.put("valido", true);
        response.put("usuarioId", jwtService.extraerUsuarioId(tokenStr));
        response.put("correo", jwtService.extraerCorreo(tokenStr));
        response.put("rol", jwtService.extraerRol(tokenStr));
        response.put("expiracion", jwtService.extraerExpiracion(tokenStr));
        response.put("mensaje", "Token válido");

        return response;
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

    // ============ CAMBIAR CONTRASEÑA (RF-04) ============
    @Transactional
    public Map<String, String> cambiarPassword(String tokenStr, String passwordActual, String passwordNueva) {
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

        return Map.of("mensaje", "Contraseña actualizada exitosamente. Por favor inicie sesión nuevamente.");
    }

    // ============ RECUPERAR CONTRASEÑA (RF-03) ============
    public Map<String, String> recuperarPassword(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null || usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            auditoriaClient.registrar("RECUPERACION_PASSWORD", correo, "N/A", "Usuario no disponible");
            return Map.of("mensaje", "Si el correo existe, recibirás instrucciones en breve.");
        }

        String tokenRecuperacion = jwtService.generarToken(usuario.getId(), correo, "RECUPERACION");

        auditoriaClient.registrar("RECUPERACION_PASSWORD", correo, "N/A", "Token de recuperación generado");

        System.out.println("📧 [MOCK G7] Enviando correo a: " + correo);
        System.out.println("📧 [MOCK G7] Token recuperación: " + tokenRecuperacion);

        return Map.of("mensaje", "Si el correo existe, recibirás instrucciones en breve.");
    }

    // ============ GESTIÓN DE SESIONES (CU-07) ============
    public List<Map<String, Object>> obtenerSesionesActivas(String usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Token> tokens = tokenRepository.findTokensActivosByUsuarioId(usuarioId, ahora);
        return tokens.stream().map(this::tokenToMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerTodasLasSesiones() {
        LocalDateTime ahora = LocalDateTime.now();
        return tokenRepository.findAll().stream()
                .filter(t -> !t.getRevocado() && t.getFechaExpiracion().isAfter(ahora))
                .map(this::tokenToMap)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buscarSesionesPorTermino(String termino) {
        LocalDateTime ahora = LocalDateTime.now();
        return tokenRepository.findAll().stream()
                .filter(t -> !t.getRevocado() && t.getFechaExpiracion().isAfter(ahora))
                .filter(t -> {
                    Usuario u = t.getUsuario();
                    String b = termino.toLowerCase();
                    return u.getId().toLowerCase().contains(b) ||
                            u.getCorreo().toLowerCase().contains(b) ||
                            (u.getNombres() + " " + u.getApellidos()).toLowerCase().contains(b);
                })
                .map(this::tokenToMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revocarSesion(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        token.setRevocado(true);
        tokenRepository.save(token);
        auditoriaClient.registrar("SESION_REVOCADA", token.getUsuario().getCorreo(),
                "N/A", "Sesión revocada por admin - Token ID: " + tokenId);
    }

    @Transactional
    public void revocarTodasLasSesiones(String usuarioId) {
        tokenRepository.revocarTokensByUsuarioId(usuarioId);
        auditoriaClient.registrar("TODAS_SESIONES_REVOCADAS", usuarioId,
                "N/A", "Todas las sesiones revocadas por admin");
    }

    public Map<String, Object> obtenerEstadisticasSesiones() {
        List<Token> todos = tokenRepository.findAll();
        LocalDateTime ahora = LocalDateTime.now();

        long activas = 0, expiradas = 0, revocadas = 0;
        for (Token t : todos) {
            if (t.getRevocado()) revocadas++;
            else if (t.getFechaExpiracion().isBefore(ahora)) expiradas++;
            else activas++;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todos.size());
        stats.put("activas", activas);
        stats.put("expiradas", expiradas);
        stats.put("revocadas", revocadas);
        return stats;
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

            if (intentos >= MAX_INTENTOS) {
                authPersistenceService.bloquearCuenta(usuario, ip);
                throw new AccesoNoAutorizadoException(
                        "Cuenta bloqueada por " + MAX_INTENTOS +
                        " intentos fallidos. Espere " + MINUTOS_BLOQUEO + " minutos.");
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

    private Map<String, Object> tokenToMap(Token token) {
        LocalDateTime ahora = LocalDateTime.now();
        Map<String, Object> sesion = new HashMap<>();
        sesion.put("id", token.getId());
        sesion.put("tokenPreview", token.getToken().substring(0, Math.min(30, token.getToken().length())) + "...");
        sesion.put("usuarioId", token.getUsuario().getId());
        sesion.put("usuarioCorreo", token.getUsuario().getCorreo());
        sesion.put("usuarioNombre", token.getUsuario().getNombres() + " " + token.getUsuario().getApellidos());
        sesion.put("fechaCreacion", token.getFechaCreacion());
        sesion.put("fechaExpiracion", token.getFechaExpiracion());
        sesion.put("minutosRestantes", java.time.Duration.between(ahora, token.getFechaExpiracion()).toMinutes());
        sesion.put("dispositivo", token.getDispositivo() != null ? token.getDispositivo() : "Desconocido");
        sesion.put("ip", token.getIpAddress() != null ? token.getIpAddress() : "No registrada");
        return sesion;
    }

    private void validarPoliticaPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ReglaNegocioException("La contraseña debe tener al menos una letra mayúscula");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ReglaNegocioException("La contraseña debe tener al menos un número");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new ReglaNegocioException("La contraseña debe tener al menos un carácter especial");
        }
    }
}
