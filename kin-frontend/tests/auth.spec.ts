import { test, expect } from '@playwright/test';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const TEST_EMAIL = `test-${Date.now()}@kin.test`;
const TEST_PASSWORD = 'TestPass123!';

test.describe('Login flow', () => {
  test.beforeAll(async ({ request }) => {
    const res = await request.post(`${API_URL}/auth/register`, {
      data: {
        email: TEST_EMAIL,
        password: TEST_PASSWORD,
        fullName: 'Test User',
      },
    });
    expect(res.ok()).toBeTruthy();
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
    await expect(passwordInput).toHaveAttribute('placeholder', 'Contrasena');
  });

  test('debe mostrar "Invalid email or password" al intentar login con credenciales invalidas', async ({ page }) => {
    await page.locator('input[type="email"]').fill('nonexistent@test.com');
    await page.locator('input[type="password"]').fill('wrongpassword');
    await page.getByRole('button', { name: 'Entrar' }).click();

    const errorEl = page.locator('p.text-red-600');
    await expect(errorEl).toBeVisible();
    await expect(errorEl).toHaveText('Invalid email or password');
  });

  test('debe loguear con credenciales validas y redirigir a /dashboard', async ({ page }) => {
    await page.locator('input[type="email"]').fill(TEST_EMAIL);
    await page.locator('input[type="password"]').fill(TEST_PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();

    await page.waitForURL(/\/dashboard/);
    await expect(page.locator('p.text-red-600')).not.toBeVisible();
  });
});
