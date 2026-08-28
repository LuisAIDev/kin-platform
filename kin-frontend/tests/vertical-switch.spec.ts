import { test, expect } from '@playwright/test';

// Requiere el entorno E2E aislado (:3100 frontend / :8081 backend perfil test).
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081/api/v1';
const TEST_EMAIL = `vertical-${Date.now()}@kin.test`;
const TEST_PASSWORD = 'TestPass123!';

test.describe('Cambio de vertical (Empresa ↔ Salud) para un mismo usuario', () => {
  test.beforeAll(async ({ request }) => {
    // Registro empresarial (rol FREE por defecto) + verificación de email real.
    const reg = await request.post(`${API_URL}/auth/register`, {
      data: { email: TEST_EMAIL, password: TEST_PASSWORD, fullName: 'Vertical Test' },
    });
    expect(reg.ok()).toBeTruthy();

    const linkRes = await request.get(
      `${API_URL}/auth/test/verification-link?email=${encodeURIComponent(TEST_EMAIL)}`,
    );
    expect(linkRes.ok()).toBeTruthy();
    const { link } = await linkRes.json();
    const token = new URL(link).searchParams.get('token');
    expect(token).toBeTruthy();

    const verifyRes = await request.get(
      `${API_URL}/auth/verify-email?token=${encodeURIComponent(token!)}`,
    );
    expect(verifyRes.ok()).toBeTruthy();
  });

  test('el proyecto de Empresa NO determina la vertical: el usuario elige Salud y KIN lo respeta', async ({
    page,
  }) => {
    // 1) Login de un usuario empresarial.
    await page.goto('/login');
    await page.locator('input[type="email"]').fill(TEST_EMAIL);
    await page.locator('input[type="password"]').fill(TEST_PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();

    // Sin vertical previa → Empresa (comportamiento seguro por defecto).
    await page.waitForURL(/\/dashboard\/empresa/);

    // 2) El usuario tiene un proyecto EXISTENTE en Empresa. Se crea desde el
    // contexto autenticado del navegador (credentials: 'include') para
    // reutilizar la cookie HttpOnly de la sesión, en lugar de page.request.
    const created = await page.evaluate(async (apiUrl) => {
      const res = await fetch(`${apiUrl}/projects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          title: 'Proyecto Vertical E2E',
          description: 'Contexto Empresa',
          category: 'EMPRESARIAL',
        }),
      });
      return { ok: res.ok, status: res.status };
    }, API_URL);
    expect(created.ok).toBeTruthy();
    await page.reload();
    await expect(page.getByText('Proyecto Vertical E2E')).toBeVisible();

    // 3) Selecciona Salud en el selector del Sidebar.
    await page.getByRole('button', { name: 'Vertical Salud' }).click();

    // 4) KIN debe permanecer en /dashboard/salud (NO redirigir a Empresa).
    await page.waitForURL(/\/dashboard\/salud/);
    expect(page.url()).toContain('/dashboard/salud');
    await expect(page.getByRole('heading', { name: /Hola,/ })).toBeVisible();
    // 5) No redirige a /login ni muestra proyectos de Empresa.
    expect(page.url()).not.toContain('/login');
    await expect(page.getByText('Proyecto Vertical E2E')).toHaveCount(0);

    // 6) Vuelve a Empresa → sus proyectos de Empresa siguen existiendo.
    await page.getByRole('button', { name: 'Vertical Empresa' }).click();
    await page.waitForURL(/\/dashboard\/empresa/);
    await expect(page.getByText('Proyecto Vertical E2E')).toBeVisible();

    // 7) Vuelve a Salud → permanece en Salud.
    await page.getByRole('button', { name: 'Vertical Salud' }).click();
    await page.waitForURL(/\/dashboard\/salud/);
    expect(page.url()).toContain('/dashboard/salud');
  });
});
