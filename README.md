# 🔐 G1 — Identidad y Accesos | Campus360

Módulo de autenticación y gestión de usuarios para la plataforma **Campus360 - UNMSM**.  
Desarrollado con Spring Boot + JWT + BCrypt + MySQL.

---

## 📋 Requisitos previos

Asegúrate de tener instalado lo siguiente antes de ejecutar el proyecto:

| Herramienta | Versión recomendada |
|-------------|-------------------|
| Java JDK | 17 o superior |
| Maven | 3.8 o superior |
| MySQL | 8.0 o superior |
| IntelliJ IDEA | Cualquier versión reciente |

---

## ⚙️ Configuración de la base de datos

### 1. Crear la base de datos

Abre MySQL Workbench o cualquier cliente MySQL y ejecuta el archivo `script.sql` ubicado en la raíz del proyecto:

```sql
-- Puedes ejecutarlo desde MySQL Workbench o desde la terminal:
mysql -u root -p < script.sql
```

Esto creará automáticamente:
- La base de datos `iamdb`
- Las tablas `roles`, `rol_permisos`, `usuarios` y `tokens`
- Los usuarios y roles de prueba

### 2. Configurar la conexión

Abre el archivo `src/main/resources/application.properties` y ajusta los datos de conexión según tu entorno:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/iamdb?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA
```

> ⚠️ El puerto por defecto de MySQL es `3306`. Si el tuyo es diferente, cámbialo en la URL.

---

## 🚀 Cómo ejecutar el proyecto

### Opción A — Desde IntelliJ IDEA (recomendado)

1. Clona o descarga el repositorio
2. Abre IntelliJ IDEA → `File > Open` → selecciona la carpeta del proyecto
3. Espera que Maven descargue las dependencias automáticamente
4. Abre la clase `G1IdentidadApplication.java`
5. Haz clic en el botón ▶️ **Run**

### Opción B — Desde la terminal con Maven

```bash
mvn spring-boot:run
```

### Opción C — Compilar y ejecutar el JAR

```bash
mvn clean package -DskipTests
java -jar target/g1-identidad-accesos-*.jar
```

Una vez iniciado, verás en la consola:
```
Módulo G1 - Identidad y Accesos iniciado en http://localhost:8080
```

---

## 🌐 Acceso al frontend

Abre tu navegador y ve a:

```
http://localhost:8080
```

---

## 👥 Usuarios de prueba

| Correo | Contraseña | Rol | Estado |
|--------|-----------|-----|--------|
| admin.sistema@campus360.com | Admin123@ | ADMIN | Activo |
| estudiante@campus360.com | Estudiante1# | ESTUDIANTE | Activo |
| profesor.ana@campus360.com | Docente1@ | DOCENTE | Activo |
| maria.lopez@campus360.com | Maria456! | ESTUDIANTE | Activo |
| carlos.rodriguez@campus360.com | Carlos789$ | ESTUDIANTE | Activo |
| bloqueado@campus360.com | Admin123@ | ESTUDIANTE | 🔴 Bloqueado |
| inactivo@campus360.com | Admin123@ | ESTUDIANTE | ⚪ Inactivo |

---

## 📡 Endpoints principales

### Autenticación
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Iniciar sesión |
| POST | `/api/v1/auth/logout` | Cerrar sesión |
| POST | `/api/v1/auth/refresh` | Renovar token |
| GET  | `/api/v1/auth/validate` | Validar token |
| POST | `/api/v1/auth/cambiar-password` | Cambiar contraseña |
| POST | `/api/v1/auth/recuperar-password` | Recuperar contraseña |

### Usuarios (requiere token ADMIN)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/usuarios` | Listar todos los usuarios |
| POST | `/api/v1/usuarios` | Crear usuario |
| PUT | `/api/v1/usuarios/{id}` | Actualizar usuario |
| DELETE | `/api/v1/usuarios/{id}` | Desactivar usuario |
| PUT | `/api/v1/usuarios/{id}/reactivar` | Reactivar usuario |
| PUT | `/api/v1/usuarios/{id}/desbloquear` | Desbloquear usuario |
| GET | `/api/v1/usuarios/roles` | Listar roles |
| POST | `/api/v1/usuarios/{id}/roles/{rolId}` | Asignar rol |

### Sesiones
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/sesiones/todas` | Ver todas las sesiones activas |
| GET | `/api/v1/sesiones/usuario/{id}` | Sesiones de un usuario |
| DELETE | `/api/v1/sesiones/{tokenId}` | Revocar sesión |
| GET | `/api/v1/sesiones/estadisticas` | Estadísticas del sistema |

---

## 🏗️ Estructura del proyecto

```
src/
├── main/
│   ├── java/com/campus360/identidad/
│   │   ├── config/          # SecurityConfig, DataInitializer
│   │   ├── controller/      # AuthController, UsuarioController, SesionController
│   │   ├── domain/          # Entidades: Usuario, Rol, Token
│   │   ├── repository/      # UsuarioRepository, RolRepository, TokenRepository
│   │   └── service/         # AuthService, UsuarioService, JwtService, etc.
│   └── resources/
│       ├── static/          # Frontend (HTML, CSS, JS)
│       └── application.properties
└── script.sql               # Script de base de datos
```

---

## 🔗 Integraciones con otros módulos

Este módulo se conecta con:
- **G7 — Notificaciones** → `http://localhost:8070` (envío de correos)
- **G9 — Auditoría** → `http://localhost:8090` (registro de eventos)

> Si estos módulos no están disponibles, el sistema funciona igual y muestra los eventos en la consola como `[MOCK]`.

---

## 🛠️ Stack tecnológico

- **Backend:** Java 17 + Spring Boot + Spring Security
- **Autenticación:** JWT (JSON Web Tokens) + BCrypt
- **Base de datos:** MySQL 8 + Spring Data JPA / Hibernate
- **Frontend:** HTML + CSS + JavaScript vanilla

---

## 👨‍💻 Equipo — Grupo 1

Módulo desarrollado para el curso de **Diseño de Sistemas — UNMSM**
