import { test, expect, type APIRequestContext } from "@playwright/test";

// ============================================================
// E2E Módulo Médico — Solicitud profesional → ADMIN aprueba/rechaza → Portal.
//
// Entorno: backend perfil `test` (:8081 por defecto) con el fixture
// `admin-e2e@kin.test` (rol ADMIN, verificado) sembrado por TestAdminSeeder.
// El test crea usuarios reales efímeros (correo con timestamp) contra la BD
// aislada de test (kin_e2e). NUNCA toca producción.
// ============================================================

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api/v1";
const PASSWORD = "TestPass123!";
const ADMIN_EMAIL = "admin-e2e@kin.test"; // fixture @Profile("test")

const now = Date.now();
const APPROVED_EMAIL = `app-${now}@kin.test`;
const REJECTED_EMAIL = `rej-${now}@kin.test`;
const FREE_EMAIL = `free-${now}@kin.test`;

let approvedToken = "";
let rejectedToken = "";
let freeToken = "";
let adminToken = "";

async function registerAndVerify(request: APIRequestContext, email: string, fullName: string) {
  const reg = await request.post(`${API_URL}/auth/register`, {
    data: { email, password: PASSWORD, fullName },
  });
  expect(reg.ok(), `register ${email} → HTTP ${reg.status()}`).toBeTruthy();

  const linkRes = await request.get(
    `${API_URL}/auth/test/verification-link?email=${encodeURIComponent(email)}`,
  );
  expect(linkRes.ok(), `verification-link ${email} → HTTP ${linkRes.status()}`).toBeTruthy();
  const { link } = await linkRes.json();
  const token = new URL(link).searchParams.get("token");
  expect(token).toBeTruthy();

  const verifyRes = await request.get(`${API_URL}/auth/verify-email?token=${encodeURIComponent(token!)}`);
  expect(verifyRes.ok(), `verify-email ${email} → HTTP ${verifyRes.status()}`).toBeTruthy();
}

async function login(request: APIRequestContext, email: string) {
  const res = await request.post(`${API_URL}/auth/login`, {
    data: { email, password: PASSWORD },
  });
  expect(res.ok(), `login ${email} → HTTP ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

async function applyAsPhysician(
  request: APIRequestContext,
  token: string,
  license: string,
  specialty: string,
) {
  const res = await request.post(`${API_URL}/health/physician/application`, {
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    data: {
      licenseNumber: license,
      specialty,
      country: "México",
      phone: "555-0000",
      healthDataConsent: true,
    },
  });
  expect(res.ok(), `application ${license} → HTTP ${res.status()}`).toBeTruthy();
}

async function me(request: APIRequestContext, token: string) {
  const res = await request.get(`${API_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  return res.json();
}

async function pendingIdByEmail(
  request: APIRequestContext,
  token: string,
  email: string,
): Promise<string> {
  const res = await request.get(`${API_URL}/admin/users/physicians/pending`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok(), `pending → HTTP ${res.status()}`).toBeTruthy();
  const list = await res.json();
  const mine = list.find((p: { email: string }) => p.email === email);
  expect(mine, `solicitud de ${email} presente en pendientes`).toBeDefined();
  return mine.id;
}

// ---------------------------------------------------------------------------
// Setup único a nivel de archivo (correos efímeros; sin re-registros).
// ---------------------------------------------------------------------------
test.beforeAll(async ({ request }) => {
  adminToken = await login(request, ADMIN_EMAIL);

  await registerAndVerify(request, APPROVED_EMAIL, "Médico Aprobado");
  approvedToken = await login(request, APPROVED_EMAIL);

  await registerAndVerify(request, REJECTED_EMAIL, "Médico Rechazado");
  rejectedToken = await login(request, REJECTED_EMAIL);

  await registerAndVerify(request, FREE_EMAIL, "Usuario Libre");
  freeToken = await login(request, FREE_EMAIL);
});

test.describe("Solicitud → PENDING → ADMIN aprueba → Portal (E2E-03..08)", () => {
  test.describe.configure({ mode: "serial" });

  test("E2E-03: solicitud profesional queda PENDING (endpoint real)", async ({ request }) => {
    await applyAsPhysician(request, approvedToken, "CEDULA-12345", "Medicina Interna");
    const m = await me(request, approvedToken);
    expect(m.verificationStatus).toBe("PENDING");
    expect(m.physicianCapability).toBe(false);
  });

  test("E2E-04: PENDING no accede a /health/physician/patients (403)", async ({ request }) => {
    const res = await request.get(`${API_URL}/health/physician/patients`, {
      headers: { Authorization: `Bearer ${approvedToken}` },
    });
    expect(res.status()).toBe(403);
  });

  test("E2E-05: ADMIN ve la solicitud pendiente del usuario aprobado", async ({ request }) => {
    await pendingIdByEmail(request, adminToken, APPROVED_EMAIL);
  });

  test("E2E-06: ADMIN aprueba la solicitud", async ({ request }) => {
    const id = await pendingIdByEmail(request, adminToken, APPROVED_EMAIL);
    const approve = await request.post(`${API_URL}/admin/users/physicians/${id}/approve`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(approve.status(), `approve → HTTP ${approve.status()}`).toBe(200);
  });

  test("E2E-07: aprobado obtiene physicianCapability=true y 200 en /patients", async ({ request }) => {
    const m = await me(request, approvedToken);
    expect(m.verificationStatus).toBe("APPROVED");
    expect(m.physicianCapability).toBe(true);

    const patients = await request.get(`${API_URL}/health/physician/patients`, {
      headers: { Authorization: `Bearer ${approvedToken}` },
    });
    expect(patients.status(), `GET /patients → HTTP ${patients.status()}`).toBe(200);
  });

  test("E2E-08: usuario FREE sin solicitud no accede (403)", async ({ request }) => {
    const res = await request.get(`${API_URL}/health/physician/patients`, {
      headers: { Authorization: `Bearer ${freeToken}` },
    });
    expect(res.status()).toBe(403);
  });
});

test.describe("Rechazo ADMIN → REJECTED bloqueado y puede re-solicitar (E2E-09..11)", () => {
  test.describe.configure({ mode: "serial" });

  test("E2E-09: REJECTED envía solicitud y ADMIN la rechaza", async ({ request }) => {
    await applyAsPhysician(request, rejectedToken, "CEDULA-99999", "Pediatría");
    const id = await pendingIdByEmail(request, adminToken, REJECTED_EMAIL);
    const reject = await request.post(`${API_URL}/admin/users/physicians/${id}/reject`, {
      headers: { Authorization: `Bearer ${adminToken}`, "Content-Type": "application/json" },
      data: { reason: "Cédula inválida" },
    });
    expect(reject.status(), `reject → HTTP ${reject.status()}`).toBe(200);

    const m = await me(request, rejectedToken);
    expect(m.verificationStatus).toBe("REJECTED");
    expect(m.physicianCapability).toBe(false);
  });

  test("E2E-10: REJECTED → endpoint médico → 403", async ({ request }) => {
    const res = await request.get(`${API_URL}/health/physician/patients`, {
      headers: { Authorization: `Bearer ${rejectedToken}` },
    });
    expect(res.status()).toBe(403);
  });

  test("E2E-11: REJECTED puede volver a solicitar (nueva PENDING)", async ({ request }) => {
    await applyAsPhysician(request, rejectedToken, "CEDULA-88888", "Pediatría");
    const m = await me(request, rejectedToken);
    expect(m.verificationStatus).toBe("PENDING");
  });
});

test.describe("Segregación de verticales (UI)", () => {
  test("Empresa no muestra el formulario de solicitud médica", async ({ page }) => {
    await page.goto("/dashboard/empresa");
    await expect(page.getByText("Solicitar registro como profesional")).not.toBeVisible();
    await expect(page.getByText("cédula profesional", { exact: false })).not.toBeVisible();
    await expect(page.getByText("Enviar solicitud")).not.toBeVisible();
  });

  test("Salud (paciente) no muestra el formulario de solicitud médica", async ({ page }) => {
    await page.goto("/dashboard/salud");
    await expect(page.getByText("Solicitar registro como profesional")).not.toBeVisible();
    await expect(page.getByText("cédula profesional", { exact: false })).not.toBeVisible();
    await expect(page.getByText("Enviar solicitud")).not.toBeVisible();
  });
});

test.describe("Ownership médico-paciente (E2E-12)", () => {
  test("MÉDICO aprobado obtiene lista de pacientes paginada (200)", async ({ request }) => {
    const patients = await request.get(`${API_URL}/health/physician/patients`, {
      headers: { Authorization: `Bearer ${approvedToken}` },
    });
    expect(patients.status()).toBe(200);
    const data = await patients.json();
    expect(data).toHaveProperty("content");
    expect(Array.isArray(data.content)).toBeTruthy();
  });

  test("MÉDICO NO accede a un paciente no asignado (403/404, no revela existencia)", async ({
    request,
  }) => {
    const fakePatientId = "00000000-0000-0000-0000-000000000000";
    const res = await request.get(`${API_URL}/health/physician/patients/${fakePatientId}/summary`, {
      headers: { Authorization: `Bearer ${approvedToken}` },
    });
    expect([403, 404]).toContain(res.status());
  });
});
