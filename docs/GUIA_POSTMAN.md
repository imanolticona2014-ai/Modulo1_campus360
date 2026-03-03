# Guía de Postman — Campus360 G1 IAM
## Para quienes no han usado Postman antes

---

## 1. ¿Qué es Postman y para qué sirve aquí?

Postman es una aplicación que te permite enviar peticiones HTTP a tu backend sin necesidad de tener un frontend. Es decir, puedes probar que tu API funciona correctamente **antes de que exista una interfaz visual**. En tu caso, sirve para verificar que el login, el logout, la creación de usuarios, etc., respondan correctamente.

---

## 2. Instalación y primera apertura

1. Descarga Postman desde https://www.postman.com/downloads (versión de escritorio).
2. Instálalo y ábrelo. Te pedirá crear una cuenta — puedes saltarte eso con "Skip and go to the app".
3. Al abrirse verás una pantalla con botones. El que más usarás es el **"+"** para crear una nueva petición.

---

## 3. La interfaz explicada en 4 partes

```
┌──────────────────────────────────────────────────────────┐
│  [GET ▼] [URL de la petición          ] [ Send ]         │  ← Parte 1: método y URL
├──────────────────────────────────────────────────────────┤
│  Params | Authorization | Headers | Body | ...            │  ← Parte 2: pestañas de configuración
├──────────────────────────────────────────────────────────┤
│                                                          │
│  (aquí escribes el JSON si el método lo requiere)        │  ← Parte 3: cuerpo de la petición
│                                                          │
├──────────────────────────────────────────────────────────┤
│  Status: 200 OK  |  Respuesta del servidor               │  ← Parte 4: respuesta
└──────────────────────────────────────────────────────────┘
```

**Parte 1 — El método HTTP:**  
El desplegable a la izquierda de la URL tiene: GET, POST, PUT, DELETE. Cada endpoint dice qué método usar.

**Parte 2 — Headers:**  
Cuando una petición requiere token, vas a la pestaña "Headers", agregas una fila con `Key = Authorization` y `Value = Bearer <token>`.

**Parte 3 — Body:**  
Cuando mandas datos JSON (login, crear usuario, etc.) vas a la pestaña "Body", seleccionas "raw" y en el desplegable a la derecha eliges "JSON".

**Parte 4 — La respuesta:**  
Aparece abajo. Un `200 OK` significa que todo fue bien. Un `400 Bad Request` significa que algo en tu petición está mal (el mensaje de error te dirá qué). Un `401 Unauthorized` significa que el token no es válido o no lo mandaste.

---

## 4. ANTES DE EMPEZAR — Verifica que el proyecto está corriendo

Abre IntelliJ, corre el proyecto y espera ver en la consola:
```
Started G1IdentidadApplication in X.XXX seconds
```

Si ves ese mensaje, el servidor está en `http://localhost:8080`.

---

## 5. Flujo de prueba recomendado (en este orden)

Siempre sigue este orden porque algunos endpoints necesitan el token que genera el login.

```
1. Login           → obtienes el token
2. Validar token   → verificas que el token funciona
3. Listar usuarios → prueba que el sistema funciona
4. Crear usuario   → creas un nuevo registro
5. Logout          → cierras la sesión
```

---

## 6. PETICIONES — Guía paso a paso

---

### 6.1 LOGIN
**Propósito:** Obtener el token JWT para usar en el resto de peticiones.

| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/auth/login` |
| Body | Sí (JSON) |
| Token requerido | No |

**Cómo hacerlo en Postman:**
1. Crea una nueva petición (clic en "+")
2. Cambia el método a **POST**
3. Escribe la URL: `http://localhost:8080/api/v1/auth/login`
4. Ve a la pestaña **Body**
5. Selecciona **raw** y en el desplegable elige **JSON**
6. Pega este JSON:

```json
{
  "correo": "admin.sistema@campus360.com",
  "password": "123456"
}
```

7. Clic en **Send**

**Respuesta esperada (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c3VhcmlvSWQi...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenId": 1,
  "usuario": {
    "id": "uuid-del-admin",
    "correo": "admin.sistema@campus360.com",
    "nombre": "Admin Sistema",
    "rol": "ADMIN"
  },
  "mensaje": "Login exitoso"
}
```

⚠️ **IMPORTANTE:** Copia el valor de `"token"` (sin las comillas). Lo necesitarás en todas las siguientes peticiones.

---

### 6.2 VALIDAR TOKEN
**Propósito:** Verificar que el token recibido es válido (útil para que otros módulos validen contra G1).

| Campo | Valor |
|---|---|
| Método | GET |
| URL | `http://localhost:8080/api/v1/auth/validate` |
| Token requerido | Sí |

**Cómo hacerlo:**
1. Método: **GET**
2. URL: `http://localhost:8080/api/v1/auth/validate`
3. Pestaña **Headers** → agrega una fila:
   - Key: `Authorization`
   - Value: `Bearer eyJhbGciOiJIUzI1NiJ9...` (pega tu token aquí)
4. Clic en **Send**

**Respuesta esperada (200 OK):**
```json
{
  "valido": true,
  "usuarioId": "uuid-del-usuario",
  "correo": "admin.sistema@campus360.com",
  "rol": "ADMIN",
  "expiracion": "2026-02-28T20:30:00.000+00:00",
  "mensaje": "Token válido"
}
```

---

### 6.3 LISTAR USUARIOS
**Propósito:** Ver todos los usuarios registrados en el sistema.

| Campo | Valor |
|---|---|
| Método | GET |
| URL | `http://localhost:8080/api/v1/usuarios` |
| Token requerido | Sí |

**Cómo hacerlo:**
1. Método: **GET**
2. URL: `http://localhost:8080/api/v1/usuarios`
3. Header `Authorization: Bearer <tu-token>`
4. **Send**

**Respuesta esperada (200 OK):** Lista de todos los usuarios con sus datos.

---

### 6.4 CREAR USUARIO
**Propósito:** Registrar un nuevo usuario en el sistema.

| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/usuarios` |
| Token requerido | Sí |

**Cómo hacerlo:**
1. Método: **POST**
2. URL: `http://localhost:8080/api/v1/usuarios`
3. Header `Authorization: Bearer <tu-token>`
4. Body (raw JSON):

```json
{
  "correo": "nuevo.estudiante@campus360.com",
  "nombre": "María",
  "apellidos": "García López",
  "rol": "ESTUDIANTE"
}
```

**Respuesta esperada (200 OK):**
```json
{
  "id": "nuevo-uuid-generado",
  "correo": "nuevo.estudiante@campus360.com",
  "nombre": "María García López",
  "rol": "ESTUDIANTE",
  "estado": "ACTIVO",
  "passwordTemporal": "Xk9@mPqR2",
  "mensaje": "Usuario creado exitosamente"
}
```

---

### 6.5 OBTENER USUARIO POR ID
| Campo | Valor |
|---|---|
| Método | GET |
| URL | `http://localhost:8080/api/v1/usuarios/{id}` |
| Token requerido | Sí |

Reemplaza `{id}` por el UUID real del usuario. Por ejemplo:
`http://localhost:8080/api/v1/usuarios/3f2d1a9c-8b7e-4f6a-a2c1-9d8e7f6b5c4a`

---

### 6.6 ACTUALIZAR USUARIO
| Campo | Valor |
|---|---|
| Método | PUT |
| URL | `http://localhost:8080/api/v1/usuarios/{id}` |
| Token requerido | Sí |

Body (solo los campos que quieres cambiar):
```json
{
  "nombre": "María Fernanda",
  "apellidos": "García Ruiz"
}
```

---

### 6.7 DESACTIVAR USUARIO (baja lógica)
| Campo | Valor |
|---|---|
| Método | DELETE |
| URL | `http://localhost:8080/api/v1/usuarios/{id}` |
| Token requerido | Sí |

No requiere Body. Solo la URL con el ID y el token en el header.

---

### 6.8 DESBLOQUEAR USUARIO
| Campo | Valor |
|---|---|
| Método | PUT |
| URL | `http://localhost:8080/api/v1/usuarios/{id}/desbloquear` |
| Token requerido | Sí |

No requiere Body.

---

### 6.9 ASIGNAR ROL
| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/usuarios/{usuarioId}/roles/{rolId}` |
| Token requerido | Sí |

Primero debes listar los roles (`GET /api/v1/usuarios/roles`) para obtener el ID del rol.

---

### 6.10 CAMBIAR CONTRASEÑA
| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/auth/cambiar-password` |
| Token requerido | Sí |

Body:
```json
{
  "passwordActual": "123456",
  "passwordNueva": "NuevaClave@2026"
}
```

---

### 6.11 RECUPERAR CONTRASEÑA
| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/auth/recuperar-password` |
| Token requerido | No |

Body:
```json
{
  "correo": "estudiante@campus360.com"
}
```

La respuesta siempre es genérica (no revela si el correo existe o no, por seguridad).

---

### 6.12 LOGOUT
| Campo | Valor |
|---|---|
| Método | POST |
| URL | `http://localhost:8080/api/v1/auth/logout` |
| Token requerido | Sí |

No requiere Body. Solo el header `Authorization: Bearer <token>`. Después del logout, ese token queda inválido.

---

## 7. Prueba de bloqueo de cuenta (RF-08)

Esta prueba verifica que la cuenta se bloquea tras 3 intentos fallidos.

**Pasos:**
1. Haz login con contraseña INCORRECTA 3 veces seguidas:
```json
{ "correo": "estudiante@campus360.com", "password": "malacontraseña" }
```
2. En el tercer intento, la respuesta será:
```json
{ "error": "Cuenta bloqueada por 3 intentos fallidos. Espere 15 minutos." }
```
3. Intenta hacer login de nuevo (incluso con la contraseña correcta). Obtendrás:
```json
{ "error": "Cuenta bloqueada temporalmente. Intente en X minutos." }
```
4. Desbloquea manualmente con el admin: `PUT /api/v1/usuarios/{id}/desbloquear`
5. Verifica que puedes hacer login de nuevo.

---

## 8. Organizar peticiones en una Colección

Para no perder tus peticiones, guárdalas en una colección:
1. En la barra lateral izquierda, busca **"Collections"**
2. Clic en **"+"** para crear una nueva colección → nómbrala "Campus360 G1"
3. Cuando tengas una petición lista, clic en **"Save"** (arriba a la derecha) y guárdala dentro de la colección

Así puedes reutilizarlas en cualquier momento sin tener que volver a configurarlas.

---

## 9. Usar Variables de Entorno (truco avanzado pero útil)

En lugar de pegar el token manualmente en cada petición, puedes usar una variable:

1. Clic en el ícono de engranaje arriba a la derecha → **"Environments"**
2. Crea un entorno llamado "Local" y agrega una variable `token`
3. En tus Headers, en lugar de escribir el token completo, escribe: `Bearer {{token}}`
4. Actualiza el valor de la variable después de cada login

---

## 10. Usuarios de prueba disponibles

| Correo | Contraseña | Rol | Estado |
|---|---|---|---|
| admin.sistema@campus360.com | 123456 | ADMIN | ACTIVO |
| estudiante@campus360.com | 123456 | ESTUDIANTE | ACTIVO |
| profesor.ana@campus360.com | 123456 | DOCENTE | ACTIVO |
| bloqueado@campus360.com | 123456 | ESTUDIANTE | BLOQUEADO |
| inactivo@campus360.com | 123456 | ESTUDIANTE | INACTIVO |
