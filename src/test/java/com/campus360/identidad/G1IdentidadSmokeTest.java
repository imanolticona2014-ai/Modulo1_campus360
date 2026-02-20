package com.campus360.identidad;

import com.campus360.identidad.controller.AuthController;
import com.campus360.identidad.controller.SesionController;
import com.campus360.identidad.controller.UsuarioController;
import com.campus360.identidad.service.AuthService;
import com.campus360.identidad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class G1IdentidadSmokeTest {

    @Autowired
    private AuthController authController;
    
    @Autowired
    private UsuarioController usuarioController;
    
    @Autowired
    private SesionController sesionController;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Test
    void contextLoads() {
        assertThat(authController).isNotNull();
        assertThat(usuarioController).isNotNull();
        assertThat(sesionController).isNotNull();
        assertThat(authService).isNotNull();
        assertThat(usuarioService).isNotNull();
    }
    
    @Test
    void testLoginEndpoint() {
        Map<String, Object> response = authService.login(
            "test@campus360.com", 
            "password123", 
            "Test Device", 
            "127.0.0.1"
        );
        
        assertThat(response).isNotNull();
        assertThat(response).containsKey("token");
        assertThat(response).containsKey("refreshToken");
        assertThat(response).containsKey("tokenId");
        assertThat(response.get("mensaje")).isEqualTo("Login exitoso");
    }
    
    @Test
    void testListarRolesEndpoint() {
        List<Map<String, Object>> roles = usuarioService.listarRoles();
        
        assertThat(roles).isNotNull();
        assertThat(roles).hasSize(3);
        
        Map<String, Object> primerRol = roles.get(0);
        assertThat(primerRol.get("nombre")).isEqualTo("ESTUDIANTE");
    }
}