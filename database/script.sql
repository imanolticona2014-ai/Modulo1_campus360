-- ============================================
-- SCRIPT COMPLETO - BASE DE DATOS IAM
-- Módulo G1 - Identidad y Accesos
-- ============================================

-- ============================================
-- 1. CREAR BASE DE DATOS
-- ============================================
CREATE DATABASE IF NOT EXISTS iamdb
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

SHOW DATABASES;

USE iamdb;

-- ============================================
-- 2. CREAR TABLAS
-- ============================================

-- Tabla de roles
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    permisos JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id VARCHAR(36) PRIMARY KEY,
    correo VARCHAR(100) UNIQUE NOT NULL,
    dni VARCHAR(8) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    rol_id VARCHAR(36),
    estado ENUM('ACTIVO', 'INACTIVO', 'BLOQUEADO') DEFAULT 'ACTIVO',
    intentos_fallidos INT DEFAULT 0,
    bloqueo_hasta TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rol_id) REFERENCES roles(id)
);

-- Tabla de tokens
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
-- 3. INSERTAR ROLES POR DEFECTO
-- ============================================
INSERT INTO roles (id, nombre, permisos) VALUES
(UUID(), 'ESTUDIANTE', '["VER_PERFIL", "SOLICITAR_TRAMITE", "VER_MIS_CURSOS"]'),
(UUID(), 'DOCENTE', '["VER_PERFIL", "VER_CURSOS", "CALIFICAR"]'),
(UUID(), 'ADMIN', '["ALL", "GESTIONAR_USUARIOS", "GESTIONAR_ROLES", "VER_AUDITORIA"]');

-- ============================================
-- 4. INSERTAR USUARIOS DE PRUEBA
-- ============================================

-- Usuario estudiante (inicialmente sin rol)
INSERT INTO usuarios (id, correo, password_hash, nombres, apellidos) VALUES
(UUID(), 'estudiante@campus360.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9', 'Juan', 'Perez');

-- Más estudiantes (con rol asignado)
INSERT INTO usuarios (id, correo, password_hash, nombres, apellidos, rol_id, estado) 
VALUES 
(UUID(), 'maria.lopez@campus360.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9', 'Maria', 'Lopez', 
 (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'), 'ACTIVO'),
 
(UUID(), 'carlos.rodriguez@campus360.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9', 'Carlos', 'Rodriguez', 
 (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE'), 'ACTIVO');

-- Docente
INSERT INTO usuarios (id, correo, password_hash, nombres, apellidos, rol_id, estado) 
VALUES (UUID(), 'profesor.ana@campus360.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9', 'Ana', 'Torres', 
 (SELECT id FROM roles WHERE nombre = 'DOCENTE'), 'ACTIVO');

-- Administrador
INSERT INTO usuarios (id, correo, password_hash, nombres, apellidos, rol_id, estado) 
VALUES (UUID(), 'admin.sistema@campus360.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.F5PQsB2Ys5X9X5X9X5X9X5X9X5X9', 'Marco', 'Tafur', 
 (SELECT id FROM roles WHERE nombre = 'ADMIN'), 'ACTIVO');

-- ============================================
-- 5. CORRECCIONES Y ACTUALIZACIONES
-- ============================================

-- Asignar rol ESTUDIANTE a Juan Pérez
UPDATE usuarios 
SET rol_id = (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE')
WHERE correo = 'estudiante@campus360.com';

-- ============================================
-- 6. CONSULTAS DE VERIFICACIÓN
-- ============================================

-- Ver todos los usuarios con sus roles
SELECT u.id, u.correo, u.nombres, u.apellidos, r.nombre AS rol, u.estado
FROM usuarios u
LEFT JOIN roles r ON u.rol_id = r.id;

-- Ver solo administradores
SELECT u.id, u.correo, u.nombres, u.apellidos, r.nombre AS rol
FROM usuarios u
JOIN roles r ON u.rol_id = r.id
WHERE r.nombre = 'ADMIN';

-- Verificar que no hay usuarios sin rol
SELECT * FROM usuarios WHERE rol_id IS NULL;

-- Ver todos los roles
SELECT * FROM roles;

-- Ver tokens (inicialmente vacía)
SELECT * FROM tokens;

SELECT * FROM tokens WHERE id = 14;



