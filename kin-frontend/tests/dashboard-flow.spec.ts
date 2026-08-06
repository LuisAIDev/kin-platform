import { test, expect } from '@playwright/test';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const TEST_EMAIL = `flow-${Date.now()}@kin.test`;
const TEST_PASSWORD = 'TestPass123';
const PROJECT_TITLE = `Proyecto E2E ${Date.now()}`;

test.describe('Dashboard flow', () => {
  test.beforeAll(async ({ request }) => {
    const res = await request.post(`${API_URL}/auth/register`, {
      data: { email: TEST_EMAIL, password: TEST_PASSWORD, fullName: 'Flow User' },
    });
    expect(res.ok()).toBeTruthy();
  });

  test('registro → login → crea proyecto → logout', async ({ page }) => {
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
