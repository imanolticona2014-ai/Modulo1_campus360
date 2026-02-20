package com.campus360.identidad.repository;

import com.campus360.identidad.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findAll();

    // Buscar tokens de un usuario
    List<Token> findByUsuarioId(String usuarioId);
    
    // Buscar tokens activos (no revocados y no expirados)
    @Query("SELECT t FROM Token t WHERE t.usuario.id = :usuarioId AND t.revocado = false AND t.fechaExpiracion > :ahora")
    List<Token> findTokensActivosByUsuarioId(@Param("usuarioId") String usuarioId, @Param("ahora") LocalDateTime ahora);
    
    // Buscar por token
    Optional<Token> findByToken(String token);
    
    // Buscar por refresh token
    Optional<Token> findByRefreshToken(String refreshToken);
    
    // Revocar un token específico
    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revocado = true WHERE t.id = :tokenId")
    void revocarToken(@Param("tokenId") Long tokenId);
    
    // Revocar todos los tokens de un usuario
    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revocado = true WHERE t.usuario.id = :usuarioId")
    void revocarTokensByUsuarioId(@Param("usuarioId") String usuarioId);
    
    // Eliminar tokens expirados (para limpieza programada)
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.fechaExpiracion < :fechaLimite")
    void eliminarTokensExpirados(@Param("fechaLimite") LocalDateTime fechaLimite);
}