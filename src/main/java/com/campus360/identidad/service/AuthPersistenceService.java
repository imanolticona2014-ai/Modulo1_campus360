package com.campus360.identidad.service;

import com.campus360.identidad.domain.Usuario;
import com.campus360.identidad.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthPersistenceService {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaClient auditoriaClient;
    private final EntityManager entityManager;

    public AuthPersistenceService(UsuarioRepository usuarioRepository,
                                   AuditoriaClient auditoriaClient,
                                   EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaClient = auditoriaClient;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bloquearCuenta(Usuario usuario, String ip) {
        usuario.setIntentosFallidos(3);
        usuario.setEstado(Usuario.EstadoUsuario.BLOQUEADO);
        usuario.setBloqueoHasta(LocalDateTime.now().plusMinutes(15));
        
        usuarioRepository.save(usuario);
        entityManager.flush();
        
        auditoriaClient.registrar("CUENTA_BLOQUEADA", usuario.getCorreo(), ip,
                "Bloqueada por 3 intentos fallidos");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarIntentoFallido(Usuario usuario, int intentos, String correo, String ip) {
        usuario.setIntentosFallidos(intentos);
        
        usuarioRepository.save(usuario);
        entityManager.flush();
        
        auditoriaClient.registrar("LOGIN_FALLIDO", correo, ip,
                "Contraseña incorrecta. Intento " + intentos + "/3");
    }
}