# Especificación Extendida del Endpoint `/auth/me`

## Resumen

El endpoint `GET /api/v1/auth/me` se extiende para incluir el campo `verticalAccess` que indica a qué verticales (productos) tiene acceso el usuario autenticado.

## Respuesta Extendida

### Ejemplo de Respuesta Exitosa (200 OK)

```json
{
  "email": "usuario@ejemplo.com",
  "fullName": "Juan Pérez",
  "role": "FREE",
  "emailVerified": true,
  "verificationStatus": null,
  "physicianCapability": true,
  "verticalAccess": ["empresas", "medical"]
}
```

### Ejemplo: Usuario solo Empresas (FREE)

```json
{
  "email": "empresa@ejemplo.com",
  "fullName": "Empresa Demo",
  "role": "FREE",
  "emailVerified": true,
  "verificationStatus": null,
  "physicianCapability": false,
  "verticalAccess": ["empresas"]
}
```

### Ejemplo: Usuario solo Médico (PHYSICIAN)

```json
{
  "email": "medico@ejemplo.com",
  "fullName": "Dr. Juan Pérez",
  "role": "PHYSICIAN",
  "emailVerified": true,
  "verificationStatus": "APPROVED",
  "physicianCapability": true,
  "verticalAccess": ["medical"]
}
```

### Ejemplo: Usuario Híbrido (FREE + APPROVED Physician)

```json
{
  "email": "hibrido@ejemplo.com",
  "fullName": "Usuario Híbrido",
  "role": "FREE",
  "emailVerified": true,
  "verificationStatus": "APPROVED",
  "physicianCapability": true,
  "verticalAccess": ["empresas", "medical"]
}
```

### Ejemplo: Usuario Paciente (PATIENT)

```json
{
  "email": "paciente@ejemplo.com",
  "fullName": "Paciente Demo",
  "role": "PATIENT",
  "emailVerified": true,
  "verificationStatus": null,
  "physicianCapability": false,
  "verticalAccess": ["medical"]
}
```

## Lógica de Cálculo de `verticalAccess`

El campo `verticalAccess` es un array de strings que indica a qué verticales (productos) tiene acceso el usuario.

### Reglas de Cálculo

```java
// Pseudocódigo de la lógica en AuthService.getCurrentUser()

Set<String> verticalAccess = new HashSet<>();

// 1. Acceso a Empresas
// Roles empresariales + ADMIN siempre tienen acceso a Empresas
if (role != null && (role.equals("FREE") || role.equals("PREMIUM") 
        || role.equals("FACILITADOR") || role.equals("ADMIN"))) {
    verticalAccess.add("empresas");
}

// 2. Acceso a Medical (Salud)
// Cualquier usuario con capacidad médica o rol de salud
if (role != null && (role.equals("PATIENT") || role.equals("PHYSICIAN"))) {
    verticalAccess.add("medical");
}

// 3. Capacidad Médica (physicianCapability)
// Usuario con capacidad médica aprobada (FREE/PREMIUM/PATIENT + APPROVED)
if (physicianCapability == true) {
    verticalAccess.add("medical");
}

// 4. ADMIN tiene acceso a todo
if ("ADMIN".equals(role)) {
    verticalAccess.add("empresas");
    verticalAccess.add("medical");
}

// 5. Usuario sin suscripción activa pero con role empresarial
// (usa rol del usuario, no suscripción)
```

### Tabla de Decisión

| Role | physicianCapability | verticalAccess |
|------|---------------------|----------------|
| `FREE` | `false` | `["empresas"]` |
| `PREMIUM` | `false` | `["empresas"]` |
| `FACILITADOR` | `false` | `["empresas"]` |
| `ADMIN` | `false` / `true` | `["empresas", "medical"]` |
| `PATIENT` | `false` | `["medical"]` |
| `PHYSICIAN` | `true` | `["medical"]` |
| `FREE` | `true` (APPROVED) | `["empresas", "medical"]` |
| `PREMIUM` | `true` (APPROVED) | `["empresas", "medical"]` |
| `PATIENT` | `true` (APPROVED) | `["medical"]` |

### Notas Importantes

1. **El rol `USER` legacy** no existe en el nuevo sistema; se mapea a `FREE`.
2. **`physicianCapability`** se deriva de `PhysicianAccess.isPhysician(user)` en el backend:
   - `true` si `physician_verification_status = APPROVED`
   - `false` en caso contrario (PENDING, REJECTED, o null)
3. **`verticalAccess` nunca es null**; siempre array (vacío si no hay acceso).
4. **Orden del array**: No garantizado, pero típicamente `["empresas", "medical"]`.

---

## Implementación en Backend (Referencia)

### AuthService.getCurrentUser()

```java
public UserDTO getCurrentUser(String token) {
    if (token == null || !jwtService.isTokenValid(token)) {
        throw new IllegalArgumentException("Invalid or expired token");
    }
    
    var email = jwtService.extractEmail(token);
    var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    
    // Calcular physicianCapability
    boolean physicianCapability = PhysicianAccess.isPhysician(user);
    
    // Calcular verticalAccess
    List<String> verticalAccess = new ArrayList<>();
    String role = user.getRole().name();
    
    // Empresas
    if (role.equals("FREE") || role.equals("PREMIUM") 
            || role.equals("FACILITADOR") || role.equals("ADMIN")) {
        verticalAccess.add("empresas");
    }
    
    // Medical: PATIENT, PHYSICIAN, o physicianCapability
    if (role.equals("PATIENT") || role.equals("PHYSICIAN") 
            || Boolean.TRUE.equals(physicianCapability)) {
        verticalAccess.add("medical");
    }
    
    // ADMIN tiene ambos
    if ("ADMIN".equals(user.getRole())) {
        verticalAccess.add("empresas");
        verticalAccess.add("medical");
    }
    
    return UserDTO.builder()
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .emailVerified(user.isEmailVerified())
            .verificationStatus(user.getPhysicianVerificationStatus())
            .physicianCapability(physicianCapability)
            .verticalAccess(verticalAccess)
            .build();
}
```

### AuthController.me()

```java
@GetMapping("/me")
public ResponseEntity<UserDTO> me(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @CookieValue(value = "kin_token_v2", required = false) String cookieToken) {
    
    String token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
    } else if (cookieToken != null && !cookieToken.isBlank()) {
        token = cookieToken;
    }
    
    if (token == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    var user = authService.getCurrentUser(token);
    return ResponseEntity.ok(user);
}
```

## Frontend - Uso del verticalAccess

### Frontend Empresas (kin-frontend)

```typescript
// En proxy.ts o RoleGuard
const { verticalAccess } = await fetchCurrentUser();

if (!verticalAccess.includes("empresas")) {
    // Redirigir a /login o mostrar error
    router.push("/login");
}
```

### kin-platform-medical.com

```typescript
// En proxy.ts o RoleGuard
const { verticalAccess } = await fetchCurrentUser();

if (!verticalAccess.includes("medical")) {
    // Redirigir a /login o mostrar error
    router.push("/login");
}
```

### Selector de Vertical (Frontend Compartido)

```typescript
// Componente VerticalSelector
const { verticalAccess } = useAuth();

const availableVerticals = verticalAccess || [];

const renderVerticalSelector = () => (
  <select 
    value={selectedVertical}
    onChange={(e) => setSelectedVertical(e.target.value)}
    disabled={availableVerticals.length <= 1}
  >
    {availableVerticals.includes("empresas") && (
      <option value="empresas">KIN Empresas</option>
    )}
    {availableVerticals.includes("medical") && (
      <option value="medical">KIN Medical</option>
    )}
  </select>
);
```

## Códigos de Estado HTTP

| Código | Significado |
|--------|-------------|
| `200 OK` | Usuario autenticado, respuesta con datos |
| `401 Unauthorized` | Token inválido, expirado o no proporcionado |
| `403 Forbidden` | Token válido pero usuario sin acceso (raro en /me) |

## Cacheo

- **Backend**: El endpoint no debe cachearse (headers `Cache-Control: no-store`)
- **Frontend**: Cachear en memoria con TTL corto (5s como en `ME_TTL_MS`)
- **Proxy/Next.js**: No cachear la respuesta

## Versionado

| Versión | Cambios | Fecha |
|---------|---------|-------|
| 1.0 | Versión inicial con verticalAccess | 2026-09 |
```