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
        // CORRECCIÓN: usar credenciales reales del script.sql.
        // La contraseña de estudiante@campus360.com es "Estudiante1#" según script.sql,
        // no "123456" ni "password123" como tenía el test original.
        Map<String, Object> response = authService.login(
            "estudiante@campus360.com",
            "Estudiante1#",
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

        // CORRECCIÓN: el orden en que la BD devuelve los roles no está garantizado.
        // En lugar de asumir que el primero es ESTUDIANTE, verificamos que los
        // tres roles esperados existen en la lista sin importar su posición.
        List<String> nombres = roles.stream()
                .map(r -> (String) r.get("nombre"))
                .toList();

        assertThat(nombres).containsExactlyInAnyOrder("ESTUDIANTE", "DOCENTE", "ADMIN");

        // Verificar que cada rol tiene los campos esperados
        roles.forEach(rol -> {
            assertThat(rol).containsKeys("id", "nombre", "permisos", "totalUsuarios");
            assertThat(rol.get("totalUsuarios")).isInstanceOf(Integer.class);
        });
    }
}
