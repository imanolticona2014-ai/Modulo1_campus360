-- ============================================
-- SCRIPT COMPLETO - BASE DE DATOS IAM
-- Módulo G1 - Identidad y Accesos - Campus360
-- VERSIÓN FINAL - Contraseñas únicas por usuario
-- Hashes generados y verificados por Spring Boot BCrypt
-- ============================================
-- Contraseñas (cumplen RN-02: mayúscula + número + símbolo):
--   admin.sistema@campus360.com    → Admin123@
--   estudiante@campus360.com       → Estudiante1#
--   maria.lopez@campus360.com      → Maria456!
--   carlos.rodriguez@campus360.com → Carlos789$
--   profesor.ana@campus360.com     → Docente1@
--   bloqueado@campus360.com        → Admin123@  (estado BLOQUEADO - demo CU-08)
--   inactivo@campus360.com         → Admin123@  (estado INACTIVO - demo RF-05)
-- ============================================

DROP DATABASE IF EXISTS iamdb;

CREATE DATABASE iamdb
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE iamdb;

-- ============================================
-- TABLAS
-- ============================================

CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rol_permisos (
    rol_id VARCHAR(36) NOT NULL,
    permiso VARCHAR(50) NOT NULL,
    FOREIGN KEY (rol_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id VARCHAR(36) PRIMARY KEY,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    rol_id VARCHAR(36),
    estado ENUM('ACTIVO', 'INACTIVO', 'BLOQUEADO') DEFAULT 'ACTIVO',
    intentos_fallidos INT DEFAULT 0,
    bloqueo_hasta DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rol_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token VARCHAR(500) NOT NULL UNIQUE,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    revocado BOOLEAN DEFAULT FALSE,
    dispositivo VARCHAR(255),
    ip_address VARCHAR(45),
    usuario_id VARCHAR(36) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_usuario_id (usuario_id),
    INDEX idx_token (token),
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_busqueda_activos (usuario_id, revocado, fecha_expiracion)
);

-- ============================================
-- ROLES Y PERMISOS
-- ============================================

INSERT INTO roles (id, nombre) VALUES
('rol-estudiante-001', 'ESTUDIANTE'),
('rol-docente-001',    'DOCENTE'),
('rol-admin-001',      'ADMIN');

INSERT INTO rol_permisos (rol_id, permiso) VALUES
('rol-estudiante-001', 'VER_PERFIL'),
('rol-estudiante-001', 'SOLICITAR_TRAMITE'),
('rol-estudiante-001', 'VER_MIS_CURSOS'),
('rol-docente-001',    'VER_PERFIL'),
('rol-docente-001',    'VER_CURSOS'),
('rol-docente-001',    'CALIFICAR'),
('rol-admin-001',      'ALL'),
('rol-admin-001',      'GESTIONAR_USUARIOS'),
('rol-admin-001',      'GESTIONAR_ROLES'),
('rol-admin-001',      'VER_AUDITORIA');

-- ============================================
-- USUARIOS DE PRUEBA
-- ============================================

INSERT INTO usuarios (id, correo, password_hash, nombres, apellidos, rol_id, estado, intentos_fallidos, bloqueo_hasta) VALUES

-- ADMIN → Admin123@
('usr-admin-001',
 'admin.sistema@campus360.com',
 '$2a$10$KCuPJpggm5moJTKgVcQmPuLP467BLLxDpO5j9X.b9h9dw.hf98EnG',
 'Marco', 'Tafur', 'rol-admin-001', 'ACTIVO', 0, NULL),

-- ESTUDIANTE 1 → Estudiante1#
('usr-estudiante-001',
 'estudiante@campus360.com',
 '$2a$10$xco6aouk6Gyl9loegUKYvuQ0rTUjZzd4JBitrBHeTI.oSYP5Y7Vya',
 'Juan', 'Perez', 'rol-estudiante-001', 'ACTIVO', 0, NULL),

-- ESTUDIANTE 2 → Maria456!
('usr-estudiante-002',
 'maria.lopez@campus360.com',
 '$2a$10$vkIcyJdATe3YOtl87DydROCdCebw2pMEKMYjs5LH4AXG/BUnY3DIe',
 'Maria', 'Lopez', 'rol-estudiante-001', 'ACTIVO', 0, NULL),

-- ESTUDIANTE 3 → Carlos789$
('usr-estudiante-003',
 'carlos.rodriguez@campus360.com',
 '$2a$10$tewfZhnFRTvuOEqzh2/GLutpQfRAlgwM2lXr3Rv5cOMFyDmxS92xC',
 'Carlos', 'Rodriguez', 'rol-estudiante-001', 'ACTIVO', 0, NULL),

-- DOCENTE → Docente1@
('usr-docente-001',
 'profesor.ana@campus360.com',
 '$2a$10$E50ZgN4u9jzYOrD9xtxxB.vVRkcGXBtbRheWwtweqOtDM.U7HL4Oi',
 'Ana', 'Torres', 'rol-docente-001', 'ACTIVO', 0, NULL),

-- BLOQUEADO (demo CU-08 / RF-08) → Admin123@
('usr-bloqueado-001',
 'bloqueado@campus360.com',
 '$2a$10$KCuPJpggm5moJTKgVcQmPuLP467BLLxDpO5j9X.b9h9dw.hf98EnG',
 'Luis', 'Bloqueado', 'rol-estudiante-001', 'BLOQUEADO', 3, DATE_ADD(NOW(), INTERVAL 15 MINUTE)),

-- INACTIVO (demo baja lógica RF-05 / RN-09) → Admin123@
('usr-inactivo-001',
 'inactivo@campus360.com',
 '$2a$10$KCuPJpggm5moJTKgVcQmPuLP467BLLxDpO5j9X.b9h9dw.hf98EnG',
 'Rosa', 'Inactiva', 'rol-estudiante-001', 'INACTIVO', 0, NULL);

-- ============================================
-- VERIFICACIÓN FINAL
-- ============================================
SELECT u.correo, r.nombre AS rol, u.estado, u.intentos_fallidos
FROM usuarios u
LEFT JOIN roles r ON u.rol_id = r.id
ORDER BY r.nombre;