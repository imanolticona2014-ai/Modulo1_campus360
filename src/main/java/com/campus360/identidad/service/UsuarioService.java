package com.campus360.identidad.service;

import com.campus360.identidad.domain.Rol;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.RolRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus360.identidad.domain.UsuarioFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de usuarios (RF-05, RF-06, RF-12).
 *
 * CORRECCIÓN (Smell 4 - Security): generarPasswordTemporal() ahora usa
 * SecureRandom en lugar de java.util.Random. SecureRandom está diseñado
 * para criptografía y produce salidas impredecibles, a diferencia de
 * Random que puede ser reproducido si se conoce el seed.
 *
 * Adicionalmente se eliminó el patrón fijo "A[digit]@xxxx" que reducía
 * drásticamente la entropía de la contraseña generada.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaClient auditoriaClient;
    private final NotificacionClient notificacionClient;
    private final UsuarioFactory usuarioFactory;

    // CORRECCIÓN (Smell 4): SecureRandom en lugar de Random
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaClient auditoriaClient,
                          NotificacionClient notificacionClient,
                          UsuarioFactory usuarioFactory) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaClient = auditoriaClient;
        this.notificacionClient = notificacionClient;
        this.usuarioFactory = usuarioFactory;
    }

    // ============ CREAR USUARIO (RF-05) ============
    @Transactional
    public Map<String, Object> crearUsuario(Map<String, String> datos) {
        String correo = datos.get("correo");
        String nombre = datos.get("nombre");
        String apellidos = datos.get("apellidos");
        String rolId = datos.get("rolId");
        String rolNombre = datos.get("rol");

        if (correo == null || correo.isBlank())
            throw new RuntimeException("El correo es obligatorio");
        if (nombre == null || nombre.isBlank())
            throw new RuntimeException("El nombre es obligatorio");

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RuntimeException("Ya existe un usuario con el correo: " + correo);
        }

        Rol rol = null;
        if (rolId != null && !rolId.isBlank()) {
            rol = rolRepository.findById(rolId)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + rolId));
        } else if (rolNombre != null && !rolNombre.isBlank()) {
            rol = rolRepository.findByNombre(rolNombre.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolNombre));
        }

        String passwordTemporal = generarPasswordTemporal();
        String hashPassword = passwordEncoder.encode(passwordTemporal);
        String apellidoSeguro = apellidos != null ? apellidos : "";

        Usuario usuario;
        if (rol != null && "DOCENTE".equalsIgnoreCase(rol.getNombre())) {
            usuario = usuarioFactory.crearDocente(correo, hashPassword, nombre, apellidoSeguro, rol);
        } else if (rol != null && "ADMIN".equalsIgnoreCase(rol.getNombre())) {
            usuario = usuarioFactory.crearAdmin(correo, hashPassword, nombre, apellidoSeguro, rol);
        } else {
            usuario = usuarioFactory.crearEstudiante(correo, hashPassword, nombre, apellidoSeguro, rol);
        }

        usuarioRepository.save(usuario);

        notificacionClient.enviarBienvenida(correo, nombre, passwordTemporal);

        String rolInfo = rol != null ? rol.getNombre() : "Sin rol";
        auditoriaClient.registrar("USUARIO_CREADO", correo, "N/A",
                "Usuario creado por admin. Rol: " + rolInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("correo", usuario.getCorreo());
        response.put("nombre", usuario.getNombres() + " " + usuario.getApellidos());
        response.put("rol", rolInfo);
        response.put("rolId", rol != null ? rol.getId() : null);
        response.put("estado", usuario.getEstado().toString());
        response.put("passwordTemporal", passwordTemporal);
        response.put("mensaje", "Usuario creado exitosamente");

        return response;
    }

    // ============ LISTAR USUARIOS (RF-05) ============
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::usuarioToMap)
                .collect(Collectors.toList());
    }

    // ============ OBTENER USUARIO POR ID (RF-05) ============
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return usuarioToMap(usuario);
    }

    // ============ ACTUALIZAR USUARIO (RF-05) ============
    @Transactional
    public Map<String, Object> actualizarUsuario(String id, Map<String, String> datos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (datos.containsKey("nombre") && !datos.get("nombre").isBlank()) {
            usuario.setNombres(datos.get("nombre"));
        }
        if (datos.containsKey("apellidos")) {
            usuario.setApellidos(datos.get("apellidos"));
        }
        if (datos.containsKey("correo") && !datos.get("correo").isBlank()) {
            String nuevoCorreo = datos.get("correo");
            if (!nuevoCorreo.equals(usuario.getCorreo())
                    && usuarioRepository.existsByCorreo(nuevoCorreo)) {
                throw new RuntimeException("Ya existe un usuario con el correo: " + nuevoCorreo);
            }
            usuario.setCorreo(nuevoCorreo);
        }

        usuarioRepository.save(usuario);

        auditoriaClient.registrar("USUARIO_ACTUALIZADO", usuario.getCorreo(), "N/A",
                "Datos de usuario actualizados");

        Map<String, Object> response = usuarioToMap(usuario);
        response.put("mensaje", "Usuario actualizado exitosamente");
        return response;
    }

    // ============ DESACTIVAR USUARIO - BAJA LÓGICA (RF-05, RN-09) ============
    @Transactional
    public Map<String, String> desactivarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setEstado(Usuario.EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);

        auditoriaClient.registrar("USUARIO_DESACTIVADO", usuario.getCorreo(), "N/A",
                "Usuario desactivado (baja lógica)");

        return Map.of(
                "mensaje", "Usuario desactivado exitosamente",
                "usuarioId", id,
                "estado", "INACTIVO"
        );
    }

    // ============ REACTIVAR USUARIO ============
    @Transactional
    public Map<String, String> reactivarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueoHasta(null);
        usuarioRepository.save(usuario);

        auditoriaClient.registrar("USUARIO_REACTIVADO", usuario.getCorreo(), "N/A",
                "Usuario reactivado por admin");

        return Map.of(
                "mensaje", "Usuario reactivado exitosamente",
                "usuarioId", id,
                "estado", "ACTIVO"
        );
    }

    // ============ DESBLOQUEAR USUARIO (RF-12) ============
    @Transactional
    public Map<String, String> desbloquearUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueoHasta(null);
        usuarioRepository.save(usuario);

        auditoriaClient.registrar("USUARIO_DESBLOQUEADO", usuario.getCorreo(), "N/A",
                "Cuenta desbloqueada manualmente por admin");

        return Map.of(
                "mensaje", "Usuario desbloqueado exitosamente",
                "usuarioId", id,
                "estado", "ACTIVO"
        );
    }

    // ============ LISTAR ROLES (RF-06) ============
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarRoles() {
        return rolRepository.findAll().stream()
                .map(rol -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rol.getId());
                    map.put("nombre", rol.getNombre());
                    map.put("permisos", rol.getPermisos());
                    map.put("totalUsuarios", rol.getUsuarios().size());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ============ ASIGNAR ROL (RF-06) ============
    @Transactional
    public Map<String, String> asignarRol(String usuarioId, String rolId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        if (usuario.getRol() != null && usuario.getRol().getId().equals(rolId)) {
            throw new RuntimeException("El usuario ya tiene asignado el rol: " + rol.getNombre());
        }

        String rolAnterior = usuario.getRol() != null ? usuario.getRol().getNombre() : "Sin rol";
        usuario.setRol(rol);
        usuarioRepository.save(usuario);

        auditoriaClient.registrar("ROL_ASIGNADO", usuario.getCorreo(), "N/A",
                "Rol cambiado de '" + rolAnterior + "' a '" + rol.getNombre() + "'");

        return Map.of(
                "mensaje", "Rol asignado exitosamente",
                "usuarioId", usuarioId,
                "rolAnterior", rolAnterior,
                "rolNuevo", rol.getNombre()
        );
    }

    // ============ MÉTODOS PRIVADOS ============

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("correo", u.getCorreo());
        map.put("nombre", u.getNombres());
        map.put("apellidos", u.getApellidos());
        map.put("nombreCompleto", u.getNombres() + " " + u.getApellidos());
        map.put("rol", u.getRol() != null ? u.getRol().getNombre() : "Sin rol");
        map.put("estado", u.getEstado().toString());
        map.put("intentosFallidos", u.getIntentosFallidos());
        map.put("bloqueadoHasta", u.getBloqueoHasta());
        return map;
    }

    /**
     * Genera una contraseña temporal segura.
     *
     * CORRECCIÓN (Smell 4): Ahora usa SecureRandom (criptográficamente
     * seguro) en lugar de Random (predecible). El carácter en cada posición
     * se elige de forma verdaderamente aleatoria. La política (mayúscula +
     * número + especial) se garantiza insertando al menos uno de cada tipo
     * en posiciones aleatorias, no fijas.
     */
    private String generarPasswordTemporal() {
        String mayusculas = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String minusculas = "abcdefghijkmnpqrstuvwxyz";
        String numeros = "23456789";
        String especiales = "!@#$%";
        String todos = mayusculas + minusculas + numeros + especiales;

        // Garantizar al menos uno de cada tipo requerido por la política
        List<Character> chars = new ArrayList<>();
        chars.add(mayusculas.charAt(SECURE_RANDOM.nextInt(mayusculas.length())));
        chars.add(numeros.charAt(SECURE_RANDOM.nextInt(numeros.length())));
        chars.add(especiales.charAt(SECURE_RANDOM.nextInt(especiales.length())));

        // Completar hasta 10 caracteres con caracteres aleatorios del pool completo
        for (int i = 0; i < 7; i++) {
            chars.add(todos.charAt(SECURE_RANDOM.nextInt(todos.length())));
        }

        // Mezclar la lista para que la posición de los caracteres obligatorios
        // no sea predecible (antes siempre eran posiciones 0, 1 y 2)
        Collections.shuffle(chars, SECURE_RANDOM);

        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }
}
