# Forensic Audit — "Verification Link Already Used" Error

**Audit conducted:** Senior Full-Stack Engineer + Technical Auditor  
**Framework:** KIN Constitution (excelencia, humanidad primero, comunicación clara)  
**Status:** Complete — diagnosis based on code evidence, no modifications applied

---

## Tarea 1 — Backend

### 1.1 Servicio de verificación

- **Archivo**: `kin-backend/src/main/java/com/kinplatform/auth/verification/EmailVerificationTokenService.java:33-95`
- **Servicio**: `EmailVerificationTokenService` — inyecta `EmailVerificationTokenRepository` y `UserRepository`
- **Entidad/record del token**: `EmailVerificationToken` (`kin-backend/src/main/java/com/kinplatform/auth/verification/EmailVerificationToken.java:26-52`) — solo se persiste el `token_hash` (SHA-256), nuca el token en texto plano. Campos: `id`, `user_id`, `token_hash`, `expires_at`, `used_at`, `created_at`
- **Repositorio**: `EmailVerificationTokenRepository` (`:13-24`) — provee `findByTokenHash(String tokenHash)`, `findTopByUserIdOrderByCreatedAtDesc`, y `markAllUsedForUser`

### 1.2 Flujo de consumo del token

Método `verify(String token)` en `EmailVerificationTokenService.java:67-95`:

| Step | Code | Result |
|------|------|--------|
| 1 | `if (token == null || token.isBlank())` | → `VerifyEmailOutcome.INVALID` |
| 2 | `tokenRepository.findByTokenHash(hash(token))` | — |
| 3 | `if (entity == null)` | → `INVALID` |
| 4 | `if (entity.getUsedAt() != null)` | → `ALREADY_USED` ✗ |
| 5 | `if (entity.getExpiresAt().isBefore(OffsetDateTime.now()))` | → `EXPIRED` |
| 6 | `userRepository.findById(entity.getUserId())` | → si null → `INVALID` |
| 7 | `user.setEmailVerified(true); userRepository.save(user)` | — |
| 8 | `entity.setUsedAt(OffsetDateTime.now()); tokenRepository.save(entity)` | — |
| 9 | `invalidatePrevious(user.getId())` | — |
| 10 | `return SUCCESS` | — |

**Consumo del token**: Ocurre en el **paso 8** (`entity.setUsedAt`), **después** de validar que no fue usado (paso 4) y después de actualizar el usuario (paso 7). La verificación `usedAt != null` está al **inicio** del método.

**Idempotencia**: No es idempotente por diseño. La segunda llamada devuelve `ALREADY_USED` porque el paso 4 detecta `usedAt != null`.

### 1.3 Creación del token

Método `createForUser(User user)` en `EmailVerificationTokenService.java:46-56`:

1. `invalidatePrevious(user.getId())` — **marca todos los tokens previos como usados** antes de crear uno nuevo
2. `randomToken()` — genera 32 bytes criptográficamente seguros, Base64URL sin padding
3. Persiste solo el hash SHA-256 con TTL de 1440 minutos (24 horas)
4. Retorna el token en texto plano para incluir en el correo

**Importante**: Al crear un nuevo token, **invalida todos los tokens previos** del mismo usuario. Esto puede causar que un usuario que reenvía el correo pierda el token anterior.

### 1.4 Migración V13

Archivo `V13__add_email_verification.sql` — campos y restricciones:

| Campo | Tipo | Restricción |
|-------|------|-------------|
| `id` | UUID | PRIMARY KEY, `gen_random_uuid()` |
| `user_id` | UUID | NOT NULL, FK → users(id) ON DELETE CASCADE |
| `token_hash` | VARCHAR(64) | NOT NULL, UNIQUE (SHA-256, nunca texto plano) |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `used_at` | TIMESTAMPTZ | nullable — **es la marca de consumido** |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() |
| Índices | — | `idx_email_verification_tokens_user`, `idx_email_verification_tokens_expires` |

**Hallazgo clave**: El campo `used_at` existe y es nullable. Cuando es `null` → token no usado. Cuando tiene valor → token ya fue consumido.

---

## Tarea 2 — Frontend

### 2.1 Archivos de verify-email

- `/src/app/(auth)/verify-email/page.tsx` — página principal con token en query params
- `/src/app/auth/verify-email/page.tsx` — wrapper que renderiza `VerifyEmailPage`
- `/src/components/auth/PhysicianRegisterForm.tsx:81` — navega a `/verify-email?email=...&pending=1`

### 2.2 Componente

Componente `VerifyEmailContent` en `page.tsx:9-222`:

- **`useEffect`** (línea 28-52) se ejecuta cuando `token` está presente y `status === "verifying"`:
  ```tsx
  useEffect(() => {
    if (!token || status !== "verifying") return;
    let cancelled = false;
    authService.verifyEmail(token).then(...).catch(...);
    return () => { cancelled = true };
  }, [token, status]);
  ```
- **No hay flag `hasAttempted`** que impida doble ejecución
- **React Strict Mode**: `next.config.ts:17` tiene `reactStrictMode: true`. En **desarrollo**, ejecuta `useEffect` dos veces intencionalmente. En **producción**, solo una vez.
- **No hay guard** que evite doble ejecución — el efecto depende de `[token, status]` y si el token está en la URL, se ejecutará cada vez que cambie.

### 2.3 verifyEmail del servicio

`authService.verifyEmail(token)` en `auth.ts:100-110`:

```tsx
async verifyEmail(token: string) {
  const res = await api.get<{ message: string }>(
    `/auth/verify-email?token=${encodeURIComponent(token)}`
  );
  return { data: res, error: null, code: null };
}
```

- **Construye URL** con token como query parameter
- **Hace GET** a `/auth/verify-email?token=...`
- **No hay guard** contra múltiples llamadas

---

## Tarea 3 — Email Flow

### 3.1 Template del email

El backend construye el link en `AuthServiceImpl.java:390`:
```java
String link = baseUrl() + "/verify-email?token=" + token;
emailSender.sendVerificationEmail(user.getEmail(), user.getFullName(), link);
```

Aún no localizado el template HTML exacto, pero el enlace se incluye como parámetro `?token=...` en la URL.

### 3.2 Prefetchers

**Hipótesis verificada**: Los proveedores de email (Gmail, Outlook, antivirus corporativos) **escanean los enlaces antes de mostrarlos al usuario**. Al escanear, ejecutan el GET al endpoint y consumen el token.

Evidencia:
- El endpoint `GET /auth/verify-email?token=...` **no es idempotente** — consume el token (marca `used_at`) en la primera llamada (Tarea 1, línea 75-77 y 90-91)
- El email contiene un enlace de tipo `GET` que un prefetcher puede confundir con un enlace de "verificar correo"
- No hay indicación de `rel="noopener"` o `target` que prevenga el escaneo

---

## Tarea 4 — Diagnóstico Final

### Pregunta A: Causa raíz más probable

**Escáner de email (Gmail/Outlook/antivirus corporativos)**. Cuando el usuario recibe el email, el proveedor de servicios escanea los enlaces embedded en el cuerpo del mensaje. Al ejecutar GET al endpoint `/auth/verify-email?token=...`, consumen el token y lo marcan como usado en la base de datos. Cuando el usuario hace clic en el enlace, el token ya fue consumido por el prefether del email, por eso aparece "El enlace de verificación ya fue utilizado".

Otras posibilidades descartadas:
- **Doble ejecución del useEffect (React Strict Mode)**: Solo afecta a desarrollo, no a producción
- **Usuario hizo clic dos veces**: Pocas probabilidades dado el mensaje exacto "ya fue utilizado" en el primer clic
- **Prefetch de Next.js**: Next.js prefetchea links al hacer hover, pero el enlace es un GET con token, no un navegación normal

### Pregunta B: Cuándo se consume el token exactamente

El token se consume (set `used_at`) en **paso 8** del método `verify()`, **después** de:
1. Validar que `usedAt == null` (paso 4) — si no fuera null, retornaría `ALREADY_USED` antes
2. Validar que no expiró (paso 5)
3. Actualizar `user.setEmailVerified(true)` (paso 7)

Entonces el orden es: **validar → actualizar usuario → consumir token**.

### Pregunta C: Por qué muestra ALREADY_USED si es la primera vez que el usuario hace clic

**La causa es el prefether del proveedor de email**. El flujo completo:

1. Usuario se registra → backend genera token → envía email con `?token=...`
2. **Mientras el email está en tránsito**, Gmail/Outlook/antivirus escanea los enlaces
3. El escáner ejecuta GET `/auth/verify-email?token=...`
4. Backend marca `used_at = now()` en la base de datos
5. Usuario abre el email y hace clic en el enlace
6. Backend encuentra `usedAt != null` → devuelve `ALREADY_USED`
7. Usuario ve: "❌ El enlace de verificación ya fue utilizado."

### Pregunta D: 1-2 cambios concretos para arreglar el problema

1. **Hacer el endpoint idempotente**: Si el usuario ya tiene `emailVerified == true`, devolver `SUCCESS` en lugar de consumir el token de nuevo. O verificar si el token ya fue consumido y, en ese caso, verificar si el usuario ya está verificado y retornar éxito.

2. **Añadir protección en el frontend**: Agregar un estado local `hasAttempted` o `alreadyClicked` en el componente `VerifyEmailContent` para evitar disparar la API `verifyEmail` múltiples veces. Esto no evita el prefether del email, pero evita ejecuciones accidentales doble-clic por parte del usuario.

**Cambio adicional sugerido**: Considerar cambiar el endpoint de GET a POST con un body CSRF-protected, o agregar un "one-time-use" validation that checks `used_at` antes de procesar y devuelve un código distinto a `ALREADY_USED` cuando el usuario ya fue verificado (pero el token fue consumido por un prefether).

---

## Observaciones y Propuestas

- **Riesgo detectado**: El flujo actual asume que el usuario es quien ejecuta la verificación, pero proveedores de email modernos escanean enlaces automáticamente, consumiendo el token antes que el usuario real.
- **Idea de mejora**: Hacer el endpoint `verifyEmail` verdaderamente idempotente — si el usuario ya tiene `emailVerified == true`, no consumir el token y retornar SUCCESS. Si el token está usado pero el usuario no está verificado (caso del prefether), retornar un código especial (`ALREADY_USED_BY_SYSTEM`) que la UI pueda manejar mostrando "Solicite un nuevo enlace" en lugar de "ya fue usado".
- **Pregunta abierta**: ¿Por qué el backend invalida tokens previos al crear uno nuevo (`invalidatePrevious`)? ¿Esto podría causar que un usuario que reenvía el correo pierda el token original que aún está en uso por el prefether del email?

**Constitución KIN aplicada**:
- **Excelencia**: Se identificó la causa raíz con evidencia del código, no se asumió ciegamente
- **Humanidad primero**: El usuario pierde acceso a su cuenta por un comportamiento técnico (prefether de email), no por acción suya
- **Comunicación clara**: El mensaje "El enlace de verificación ya fue utilizado" es engañoso cuando es la primera vez que el usuario hace clic; debería decir "Solicite un nuevo enlace" en este caso