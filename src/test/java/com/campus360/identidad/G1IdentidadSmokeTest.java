package com.campus360.identidad;

import com.campus360.identidad.controller.AuthController;
import com.campus360.identidad.controller.UsuarioController;
import com.campus360.identidad.service.AuthService;
import com.campus360.identidad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class G1IdentidadSmokeTest {

    @Autowired
    private AuthController authController;
    
    @Autowired
    private UsuarioController usuarioController;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Test
    void contextLoads() {
        // Verificar que los controladores se cargan
        assertThat(authController).isNotNull();
        assertThat(usuarioController).isNotNull();
        
        // Verificar que los servicios se cargan
        assertThat(authService).isNotNull();
        assertThat(usuarioService).isNotNull();
    }
    
    @Test
    void testLoginEndpoint() {
        Map<String, Object> response = authService.login("test@campus360.com", "password123");
        
        assertThat(response).isNotNull();
        assertThat(response).containsKey("token");
        assertThat(response).containsKey("usuario");
        assertThat(response.get("mensaje")).isEqualTo("Login exitoso (STUB)");
    }
    
    @Test
    void testListarRolesEndpoint() {
        var roles = usuarioService.listarRoles();
        
        assertThat(roles).isNotNull();
        assertThat(roles).hasSize(3);
        assertThat(roles.get(0).getNombre()).isEqualTo("ESTUDIANTE");
    }
}