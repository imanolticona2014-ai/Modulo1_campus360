package com.campus360.identidad.repository;

import com.campus360.identidad.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    
    Optional<Usuario> findByCorreo(String correo);
    
    boolean existsByCorreo(String correo);
    
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.intentosFallidos = u.intentosFallidos + 1 WHERE u.correo = :correo")
    void incrementarIntentos(@Param("correo") String correo);
    
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.intentosFallidos = 0 WHERE u.correo = :correo")
    void resetearIntentos(@Param("correo") String correo);
    
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.estado = 'BLOQUEADO', u.bloqueoHasta = :hasta WHERE u.correo = :correo")
    void bloquearCuenta(@Param("correo") String correo, @Param("hasta") LocalDateTime hasta);
}