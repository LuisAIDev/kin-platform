import { test, expect } from "@playwright/test";

/**
 * E2E de KIN Medical (Commit 2).
 *
 * El test de la landing se ejecuta siempre (página estática, sin backend).
 * Los flujos autenticados requieren un backend accesible y cuentas sembradas;
 * se habilitan con E2E_AUTH=1 y credenciales por variable de entorno:
 *   E2E_PATIENT_EMAIL / E2E_PATIENT_PASSWORD
 *   E2E_PHYSICIAN_EMAIL / E2E_PHYSICIAN_PASSWORD
 *   E2E_FREE_EMAIL / E2E_FREE_PASSWORD
 */

const PATIENT_EMAIL = process.env.E2E_PATIENT_EMAIL ?? "paciente@test.com";
const PATIENT_PASSWORD = process.env.E2E_PATIENT_PASSWORD ?? "Test123!";
const PHYSICIAN_EMAIL = process.env.E2E_PHYSICIAN_EMAIL ?? "medico@test.com";
const PHYSICIAN_PASSWORD = process.env.E2E_PHYSICIAN_PASSWORD ?? "Test123!";
const FREE_EMAIL = process.env.E2E_FREE_EMAIL ?? "free@test.com";
const FREE_PASSWORD = process.env.E2E_FREE_PASSWORD ?? "Test123!";

const authEnabled = process.env.E2E_AUTH === "1";

async function login(page: import("@playwright/test").Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("Correo electrónico").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: /Iniciar sesión/i }).click();
}

test.describe("Landing KIN Medical", () => {
  test("muestra el hero y los CTAs B2B", async ({ page }) => {
    await page.goto("/");

    await expect(
      page.getByRole("heading", {
        name: /KIN Medical: infraestructura inteligente para tu práctica clínica/i,
      })
    ).toBeVisible();

    await expect(page.getByRole("link", { name: "Soy Médico" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Soy Paciente" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Quiero para mi Clínica" })).toBeVisible();
  });

  test('"Soy Médico" redirige al registro de médico', async ({ page }) => {
    await page.goto("/");
    await page.getByRole("link", { name: "Soy Médico" }).click();
    await expect(page).toHaveURL(/\/register\/salud\/medico$/);
  });

  test('"Soy Paciente" redirige al registro de paciente', async ({ page }) => {
    await page.goto("/");
    await page.getByRole("link", { name: "Soy Paciente" }).click();
    await expect(page).toHaveURL(/\/register\/salud\/paciente$/);
  });

  test("muestra las 3 secciones B2B (médicos, clínicas, IPS/EPS)", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("#medicos")).toBeVisible();
    await expect(page.locator("#clinicas")).toBeVisible();
    await expect(page.locator("#ips")).toBeVisible();
  });
});

test.describe("Flujos autenticados (requieren backend + cuentas sembradas)", () => {
  test.skip(!authEnabled, "Requiere E2E_AUTH=1 y backend con cuentas de prueba");

  test("login como paciente redirige a /dashboard/patient/health", async ({ page }) => {
    await login(page, PATIENT_EMAIL, PATIENT_PASSWORD);
    await expect(page).toHaveURL(/\/dashboard\/patient\/health$/);
  });

  test("login como médico redirige a /dashboard/physician", async ({ page }) => {
    await login(page, PHYSICIAN_EMAIL, PHYSICIAN_PASSWORD);
    await expect(page).toHaveURL(/\/dashboard\/physician$/);
  });

  test("usuario sin acceso a Medical no entra al portal médico", async ({ page }) => {
    await login(page, FREE_EMAIL, FREE_PASSWORD);
    // No debe aterrizar en el portal médico.
    await expect(page).not.toHaveURL(/\/dashboard\/physician$/);
  });

  test("triaje completo como paciente muestra resultados", async ({ page }) => {
    await login(page, PATIENT_EMAIL, PATIENT_PASSWORD);
    await page.goto("/dashboard/patient/triage");
    await page.getByText(/fiebre/i).first().click();
    await page.getByRole("button", { name: /Analizar/i }).click();
    await expect(page.getByText(/probabilidad|resultado|condición/i).first()).toBeVisible({
      timeout: 15000,
    });
  });

  test("portal médico muestra la lista de pacientes", async ({ page }) => {
    await login(page, PHYSICIAN_EMAIL, PHYSICIAN_PASSWORD);
    await page.goto("/dashboard/physician");
    await expect(page.getByText(/paciente/i).first()).toBeVisible({ timeout: 15000 });
  });
});
