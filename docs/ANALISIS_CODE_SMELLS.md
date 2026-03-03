# Campus360 — G1 Identidad y Accesos
## Análisis de Code Smells + Código Refactorizado

---

## RESUMEN DE PROBLEMAS ENCONTRADOS

Se identificaron **5 code smells principales** distribuidos en varios archivos del proyecto. A continuación se documenta cada uno con el código original (antes) y el código corregido (después), listo para sustituir en el repositorio.

---

## SMELL 1 — Endpoints de debug expuestos en producción
**Archivo:** `AuthController.java`  
**Tipo:** Security Smell / Dead Code

### Problema
El controlador tiene 3 endpoints de depuración que exponen información crítica: hashes BCrypt reales de la BD, resultado de comparaciones de contraseñas, y la contraseña `123456` hardcodeada. Cualquiera que conozca la URL puede llamarlos sin autenticación.

```java
// ❌ ANTES — endpoints que NO deberían existir en ningún ambiente
@GetMapping("/hash-test")
public String hashTest() {
    return passwordEncoder.encode("123456");
}

@GetMapping("/debug-hash")
public ResponseEntity<?> debugHash(@RequestParam String correo, @RequestParam String password) {
    // ... expone el hash real de la BD y resultado del match
    debug.put("hashBD", hashBD);
    debug.put("matchBCrypt", passwordEncoder.matches(password, hashBD));
    // ...
}

@GetMapping("/test-bcrypt")
public ResponseEntity<?> testBcrypt() {
    String password = "123456";
    String hashFromDB = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq";
    // hash real hardcodeado en el código fuente — grave vulnerabilidad
}
```

### Solución
Eliminar los tres métodos. Si en algún momento del desarrollo necesitan verificar BCrypt, pueden hacerlo directamente en un test unitario, nunca en un endpoint.

---

## SMELL 2 — Constructor de `RestTemplate` dentro del servicio (acoplamiento fuerte)
**Archivos:** `AuditoriaClient.java`, `NotificacionClient.java`  
**Tipo:** Tight Coupling / Violación de DIP (Dependency Inversion Principle)

### Problema
Ambas clases crean su propio `RestTemplate` con `new RestTemplate()` en el constructor. Esto impide configurar timeouts, interceptores o mocks en pruebas. Viola DIP porque la clase de alto nivel depende de la creación directa de una clase de bajo nivel.

```java
// ❌ ANTES — en AuditoriaClient.java y NotificacionClient.java
public AuditoriaClient() {
    this.restTemplate = new RestTemplate(); // creado manualmente, imposible de testear/configurar
}
```

### Solución
Inyectar `RestTemplate` como dependencia a través del constructor, y declararlo como `@Bean` con timeout configurado en `SecurityConfig` (o una clase `AppConfig` separada).

---

## SMELL 3 — `AuthService` hace demasiado (God Class parcial)
**Archivo:** `AuthService.java`  
**Tipo:** Large Class / Violación de SRP

### Problema
`AuthService` maneja autenticación, gestión de sesiones, recuperación de contraseña, validación de política de contraseñas y estadísticas. Son al menos 4 responsabilidades distintas en una sola clase de ~200 líneas.

Adicionalmente, el método `obtenerTodasLasSesiones()` hace un `findAll()` y filtra en memoria, lo que es ineficiente y no escalaría con muchos tokens.

```java
// ❌ ANTES — en AuthService.java, un solo servicio hace todo esto:
public Map<String, Object> login(...) { ... }
public Map<String, Object> validateToken(...) { ... }
public Map<String, String> refreshToken(...) { ... }
public Map<String, String> logout(...) { ... }
public Map<String, String> cambiarPassword(...) { ... }
public Map<String, String> recuperarPassword(...) { ... }
public List<Map<String, Object>> obtenerSesionesActivas(...) { ... }  // <- sesiones
public List<Map<String, Object>> obtenerTodasLasSesiones() { ... }   // <- sesiones
public List<Map<String, Object>> buscarSesionesPorTermino(...) { ... } // <- sesiones
public void revocarSesion(...) { ... }                                  // <- sesiones
public void revocarTodasLasSesiones(...) { ... }                        // <- sesiones
public Map<String, Object> obtenerEstadisticasSesiones() { ... }       // <- sesiones

// Filtro en memoria — no escala:
public List<Map<String, Object>> obtenerTodasLasSesiones() {
    return tokenRepository.findAll().stream()    // carga TODOS los tokens a RAM
            .filter(t -> !t.getRevocado() && ...)
            .map(this::tokenToMap)
            .collect(Collectors.toList());
}
```

### Solución
Extraer toda la gestión de sesiones a un `SesionService` dedicado. Mover la validación de contraseña a una clase `PasswordPolicyValidator`. Mover el filtro de memoria a una query JPQL en el repositorio.

---

## SMELL 4 — `generarPasswordTemporal()` usa `Random` en lugar de `SecureRandom`
**Archivo:** `UsuarioService.java`  
**Tipo:** Security Smell

### Problema
`java.util.Random` es predecible. Si un atacante conoce el seed (que en Java está basado en el tiempo), puede calcular las contraseñas temporales generadas. Para credenciales de seguridad se debe usar `java.security.SecureRandom`.

```java
// ❌ ANTES
private String generarPasswordTemporal() {
    String chars = "ABCDE...";
    StringBuilder sb = new StringBuilder();
    Random random = new Random(); // ← predecible, no apto para seguridad
    sb.append("A");    // ← siempre empieza con 'A' — reduce entropía
    sb.append(random.nextInt(9) + 1); // ← siempre dígito en posición 2
    sb.append("@");    // ← siempre '@' en posición 3 — patrón predecible
    // ...
}
```

### Solución
Usar `SecureRandom` y generar la contraseña de forma aleatoria verdadera, garantizando la política (mayúscula + número + especial) sin fijar posiciones.

---

## SMELL 5 — `UsuarioFactory` sin diferenciación real entre tipos de usuario
**Archivo:** `UsuarioFactory.java`  
**Tipo:** Duplicate Code / Feature Envy

### Problema
Los tres métodos de la fábrica (`crearEstudiante`, `crearDocente`, `crearAdmin`) tienen exactamente el mismo cuerpo. No hay ninguna diferencia entre ellos. Esto hace que el patrón Factory no aporte valor real y sea código duplicado puro.

```java
// ❌ ANTES — los tres métodos son idénticos
public Usuario crearEstudiante(String correo, String passwordHash, ...) {
    return Usuario.builder().correo(correo).passwordHash(passwordHash)
            .nombres(nombres).apellidos(apellidos).rol(rolEstudiante)
            .estado(Usuario.EstadoUsuario.ACTIVO).intentosFallidos(0).build();
}

public Usuario crearDocente(String correo, String passwordHash, ...) {
    return Usuario.builder().correo(correo).passwordHash(passwordHash)
            .nombres(nombres).apellidos(apellidos).rol(rolDocente)
            .estado(Usuario.EstadoUsuario.ACTIVO).intentosFallidos(0).build();
    // ← idéntico al anterior
}

public Usuario crearAdmin(String correo, String passwordHash, ...) {
    return Usuario.builder().correo(correo).passwordHash(passwordHash)
            .nombres(nombres).apellidos(apellidos).rol(rolAdmin)
            .estado(Usuario.EstadoUsuario.ACTIVO).intentosFallidos(0).build();
    // ← idéntico al anterior
}
```

### Solución
Unificar en un único método base, y si en el futuro los roles tienen reglas distintas (ej. docentes empiezan inactivos hasta verificar correo), extenderlo desde ahí.

---

## CÓDIGO REFACTORIZADO — ARCHIVOS LISTOS PARA REEMPLAZAR

Los archivos con las correcciones aplicadas están en la carpeta `/codigo-refactorizado/` junto a este documento.

### Resumen de cambios por archivo:

| Archivo | Smells corregidos |
|---|---|
| `AuthController.java` | Smell 1: eliminados 3 endpoints de debug |
| `AuditoriaClient.java` | Smell 2: RestTemplate inyectado |
| `NotificacionClient.java` | Smell 2: RestTemplate inyectado |
| `AppConfig.java` (nuevo) | Smell 2: Bean de RestTemplate con timeout |
| `SesionService.java` (nuevo) | Smell 3: responsabilidades de sesión extraídas |
| `PasswordPolicyValidator.java` (nuevo) | Smell 3: validación de contraseña extraída |
| `AuthService.java` | Smell 3: eliminado código de sesiones y validación |
| `UsuarioService.java` | Smell 4: SecureRandom en lugar de Random |
| `UsuarioFactory.java` | Smell 5: método unificado, eliminada duplicación |
