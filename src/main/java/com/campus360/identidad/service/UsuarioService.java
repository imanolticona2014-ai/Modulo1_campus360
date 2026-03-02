package com.campus360.identidad.service;

import com.campus360.identidad.domain.Rol;
import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.RolRepository;
import com.campus360.identidad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus360.identidad.domain.UsuarioFactory;
import com.campus360.identidad.exception.RecursoNoEncontradoException;
import com.campus360.identidad.exception.ReglaNegocioException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaClient auditoriaClient;
    private final NotificacionClient notificacionClient;
    private final UsuarioFactory usuarioFactory; 

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

        // ✅ FIX: Aceptar "rolId" (ID del rol) como campo principal.
        // También se mantiene compatibilidad con "rol" (nombre) como fallback.
        String rolId = datos.get("rolId");
        String rolNombre = datos.get("rol");

        // Validar campos obligatorios
        if (correo == null || correo.isBlank()) throw new ReglaNegocioException("El correo es obligatorio");
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");

        // Validar unicidad de correo
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo: " + correo);
        }

        // ✅ FIX: Buscar rol por ID primero (más robusto), fallback por nombre
        Rol rol = null;
        if (rolId != null && !rolId.isBlank()) {
            // Buscar por ID — estándar REST
            rol = rolRepository.findById(rolId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + rolId));
        } else if (rolNombre != null && !rolNombre.isBlank()) {
            // Fallback: buscar por nombre (retrocompatibilidad)
            rol = rolRepository.findByNombre(rolNombre.toUpperCase())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado: " + rolNombre));
        }

        // Generar contraseña temporal
        String passwordTemporal = generarPasswordTemporal();

        // Crear usuario
        // ==========================================
        // USO DEL PATRÓN FACTORY Y BUILDER
        // ==========================================
        String hashPassword = passwordEncoder.encode(passwordTemporal);
        String apellidoSeguro = apellidos != null ? apellidos : "";
        Usuario usuario;

        if (rol != null && "DOCENTE".equalsIgnoreCase(rol.getNombre())) {
            usuario = usuarioFactory.crearDocente(correo, hashPassword, nombre, apellidoSeguro, rol);
        } else if (rol != null && "ADMIN".equalsIgnoreCase(rol.getNombre())) {
            usuario = usuarioFactory.crearAdmin(correo, hashPassword, nombre, apellidoSeguro, rol);
        } else {
            // Por defecto se crea como ESTUDIANTE
            usuario = usuarioFactory.crearEstudiante(correo, hashPassword, nombre, apellidoSeguro, rol);
        }
        // ==========================================

        usuarioRepository.save(usuario);

        // Notificar al usuario (G7)
        notificacionClient.enviarBienvenida(correo, nombre, passwordTemporal);

        // Registrar en auditoría (G9)
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
    public List<Map<String, Object>> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::usuarioToMap)
                .collect(Collectors.toList());
    }

    // ============ OBTENER USUARIO POR ID (RF-05) ============
    public Map<String, Object> obtenerUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
        return usuarioToMap(usuario);
    }

    // ============ ACTUALIZAR USUARIO (RF-05) ============
    @Transactional
    public Map<String, Object> actualizarUsuario(String id, Map<String, String> datos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        if (datos.containsKey("nombre") && !datos.get("nombre").isBlank()) {
            usuario.setNombres(datos.get("nombre"));
        }
        if (datos.containsKey("apellidos")) {
            usuario.setApellidos(datos.get("apellidos"));
        }
        if (datos.containsKey("correo") && !datos.get("correo").isBlank()) {
            String nuevoCorreo = datos.get("correo");
            if (!nuevoCorreo.equals(usuario.getCorreo()) && usuarioRepository.existsByCorreo(nuevoCorreo)) {
                throw new ReglaNegocioException("Ya existe un usuario con el correo: " + nuevoCorreo);
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        resetearCuenta(usuario); 

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        resetearCuenta(usuario); 
        auditoriaClient.registrar("USUARIO_DESBLOQUEADO", usuario.getCorreo(), "N/A",
                "Cuenta desbloqueada manualmente por admin");

        return Map.of(
                "mensaje", "Usuario desbloqueado exitosamente",
                "usuarioId", id,
                "estado", "ACTIVO"
        );
    }

    // ============ LISTAR ROLES (RF-06) ============
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado"));

        // Verificar que no sea el mismo rol
        if (usuario.getRol() != null && usuario.getRol().getId().equals(rolId)) {
            throw new ReglaNegocioException("El usuario ya tiene asignado el rol: " + rol.getNombre());
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
    private void resetearCuenta(Usuario usuario) {
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueoHasta(null);
        usuarioRepository.save(usuario);
    }
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

    private String generarPasswordTemporal() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        // Garantizar política: mayúscula + número + especial + resto
        sb.append("A"); // mayúscula
        sb.append(random.nextInt(9) + 1); // número
        sb.append("@"); // especial
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
