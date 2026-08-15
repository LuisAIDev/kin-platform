import { test, expect } from '@playwright/test';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const TEST_EMAIL = `flow-${Date.now()}@kin.test`;
const TEST_PASSWORD = 'TestPass123!';
const PROJECT_TITLE = `Proyecto E2E ${Date.now()}`;

test.describe('Dashboard flow', () => {
  test.beforeAll(async ({ request }) => {
    // 1) Registro (el correo lo captura LoggingEmailSender en perfil test)
    const reg = await request.post(`${API_URL}/auth/register`, {
      data: { email: TEST_EMAIL, password: TEST_PASSWORD, fullName: 'Flow User' },
    });
    expect(reg.ok()).toBeTruthy();

    // 2) Recuperar el enlace de verificación capturado por el test hook (solo perfil test)
    const linkRes = await request.get(
      `${API_URL}/auth/test/verification-link?email=${encodeURIComponent(TEST_EMAIL)}`
    );
    expect(linkRes.ok()).toBeTruthy();
    const { link } = await linkRes.json();

    // 3) Ejercitar el endpoint REAL de verificación
    const token = new URL(link).searchParams.get('token');
    expect(token).toBeTruthy();
    const verifyRes = await request.get(
      `${API_URL}/auth/verify-email?token=${encodeURIComponent(token!)}`
    );
    expect(verifyRes.ok()).toBeTruthy();
  });

  test('registro → verificación → login → crea proyecto → logout', async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.locator('input[type="email"]').fill(TEST_EMAIL);
    await page.locator('input[type="password"]').fill(TEST_PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await page.waitForURL(/\/dashboard/);

    // Crear proyecto
    await page.goto('/dashboard/projects/new');
    await page.locator('input#title').fill(PROJECT_TITLE);
    await page.locator('select#category').selectOption({ index: 1 });
    await page.getByRole('button', { name: /crear|guardar|siguiente/i }).first().click();

    // El proyecto aparece en la lista
    await page.goto('/dashboard/projects');
    await expect(page.getByText(PROJECT_TITLE).first()).toBeVisible({ timeout: 15000 });

    // Logout
    await page.getByRole('button', { name: 'Cerrar sesión' }).first().click();
    await page.waitForURL(/\/login/);
  });
});
