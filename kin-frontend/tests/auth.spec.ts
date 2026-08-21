import { test, expect } from '@playwright/test';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const TEST_EMAIL = `test-${Date.now()}@kin.test`;
const TEST_PASSWORD = 'TestPass123!';

test.describe('Login flow', () => {
  test.beforeAll(async ({ request }) => {
    // 1) Registro (emailVerified=false; el correo lo captura LoggingEmailSender en test)
    const reg = await request.post(`${API_URL}/auth/register`, {
      data: {
        email: TEST_EMAIL,
        password: TEST_PASSWORD,
        fullName: 'Test User',
      },
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

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('debe renderizar el formulario de login con campos email y password', async ({ page }) => {
    const emailInput = page.locator('input[type="email"]');
    const passwordInput = page.locator('input[type="password"]');
    const submitButton = page.getByRole('button', { name: 'Entrar' });

    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitButton).toBeVisible();

    await expect(emailInput).toHaveAttribute('placeholder', 'Email');
    await expect(passwordInput).toHaveAttribute('placeholder', 'Contraseña');
  });

  test('debe mostrar "Invalid email or password" al intentar login con credenciales invalidas', async ({ page }) => {
    await page.locator('input[type="email"]').fill('nonexistent@test.com');
    await page.locator('input[type="password"]').fill('wrongpassword');
    await page.getByRole('button', { name: 'Entrar' }).click();

    const errorEl = page.locator('p.text-red-600');
    await expect(errorEl).toBeVisible();
    await expect(errorEl).toHaveText('Invalid email or password');
  });

  test('debe loguear con credenciales validas (cuenta verificada) y redirigir a /dashboard', async ({ page }) => {
    await page.locator('input[type="email"]').fill(TEST_EMAIL);
    await page.locator('input[type="password"]').fill(TEST_PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();

    await page.waitForURL(/\/dashboard/);
    await expect(page.locator('p.text-red-600')).not.toBeVisible();
  });
});
