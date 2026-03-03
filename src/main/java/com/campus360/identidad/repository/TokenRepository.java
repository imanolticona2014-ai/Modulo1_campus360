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

/**
 * Repositorio de tokens.
 *
 * ADICIÓN (necesaria por Smell 3): Se agrega findAllActivas() para que
 * SesionService pueda filtrar tokens activos directamente en la BD
 * en lugar de hacer findAll() y filtrar en memoria.
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    List<Token> findByUsuarioId(String usuarioId);

    @Query("SELECT t FROM Token t WHERE t.usuario.id = :usuarioId AND t.revocado = false AND t.fechaExpiracion > :ahora")
    List<Token> findTokensActivosByUsuarioId(@Param("usuarioId") String usuarioId,
                                              @Param("ahora") LocalDateTime ahora);

    /**
     * Devuelve todos los tokens activos del sistema (no revocados y no expirados).
     * Reemplaza el patrón findAll() + filtro en memoria del código original.
     */
    @Query("SELECT t FROM Token t WHERE t.revocado = false AND t.fechaExpiracion > :ahora")
    List<Token> findAllActivas(@Param("ahora") LocalDateTime ahora);

    Optional<Token> findByToken(String token);

    Optional<Token> findByRefreshToken(String refreshToken);

    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revocado = true WHERE t.id = :tokenId")
    void revocarToken(@Param("tokenId") Long tokenId);

    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revocado = true WHERE t.usuario.id = :usuarioId")
    void revocarTokensByUsuarioId(@Param("usuarioId") String usuarioId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.fechaExpiracion < :fechaLimite")
    void eliminarTokensExpirados(@Param("fechaLimite") LocalDateTime fechaLimite);
}
