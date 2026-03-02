package com.campus360.identidad.service;

import com.campus360.identidad.domain.Token;
import com.campus360.identidad.repository.TokenRepository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;

    public TokenService(TokenRepository tokenRepository, JwtService jwtService) {
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
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
}
