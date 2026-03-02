package com.campus360.identidad.controller;

import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.UsuarioRepository;
import com.campus360.identidad.service.AuthService;
import com.campus360.identidad.service.PasswordService;
import com.campus360.identidad.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthService authService, 
                          PasswordService passwordService,
                          TokenService tokenService, PasswordEncoder passwordEncoder,
                          UsuarioRepository usuarioRepository) {
        this.authService = authService;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    // ============ LOGIN ============
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credenciales,
            HttpServletRequest request) {
        try {
            String correo = credenciales.get("correo");
            String password = credenciales.get("password");
            String dispositivo = request.getHeader("User-Agent");
            String ip = request.getRemoteAddr();
            return ResponseEntity.ok(authService.login(correo, password, dispositivo, ip));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ REFRESH TOKEN ============
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(authService.refreshToken(request.get("refreshToken")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ VALIDAR TOKEN ============
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        return ResponseEntity.ok(tokenService.validateToken(jwt));
    }

    // ============ LOGOUT ============
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            return ResponseEntity.ok(authService.logout(jwt));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ CAMBIAR CONTRASEÑA (RF-04) ============
    @PostMapping("/cambiar-password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> datos) {
        try {
            String jwt = token.replace("Bearer ", "");
            String passwordActual = datos.get("passwordActual");
            String passwordNueva = datos.get("passwordNueva");
            return ResponseEntity.ok(passwordService.cambiarPassword(jwt, passwordActual, passwordNueva));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ RECUPERAR CONTRASEÑA (RF-03) ============
    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> recuperarPassword(
            @RequestBody Map<String, String> datos) {
        String correo = datos.get("correo");
        return ResponseEntity.ok(passwordService.recuperarPassword(correo));
    }

    // ============ HASH TEST (TEMPORAL - ELIMINAR EN PRODUCCION) ============
    @GetMapping("/hash-test")
    public String hashTest() {
        return passwordEncoder.encode("123456");
    }

    // ============ DEBUG HASH COMPLETO (TEMPORAL - ELIMINAR EN PRODUCCION) ============
    @GetMapping("/debug-hash")
    public ResponseEntity<?> debugHash(@RequestParam String correo, @RequestParam String password) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("correoInput", correo);
        debug.put("passwordInput", password);
        debug.put("passwordLength", password.length());
        debug.put("passwordBytes", password.getBytes());
        debug.put("usuarioEncontrado", usuario != null);
        
        if (usuario != null) {
            String hashBD = usuario.getPasswordHash();
            debug.put("hashBD", hashBD);
            debug.put("hashBDLength", hashBD.length());
            debug.put("matchBCrypt", passwordEncoder.matches(password, hashBD));
            
            // Probar con hash hardcodeado correcto
            String hashCorrecto = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq";
            debug.put("hashCorrectoHardcodeado", hashCorrecto);
            debug.put("matchHashCorrecto", passwordEncoder.matches(password, hashCorrecto));
            
            // Generar nuevo hash para comparar formato
            String nuevoHash = passwordEncoder.encode(password);
            debug.put("nuevoHashGenerado", nuevoHash);
            debug.put("matchNuevoHash", passwordEncoder.matches(password, nuevoHash));
        }
        
        return ResponseEntity.ok(debug);
    }

    // ============ TEST BCRYPT SIMPLE (TEMPORAL - ELIMINAR EN PRODUCCION) ============
    @GetMapping("/test-bcrypt")
    public ResponseEntity<?> testBcrypt() {
        String password = "123456";
        String hashFromDB = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq";
        
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hashFromDB", hashFromDB);
        result.put("matches", passwordEncoder.matches(password, hashFromDB));
        
        return ResponseEntity.ok(result);
    }
}