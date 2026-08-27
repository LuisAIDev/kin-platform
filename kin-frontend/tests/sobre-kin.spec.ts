import { test, expect } from '@playwright/test';

test.describe('Sobre KIN', () => {
  test('la página pública renderiza el contenido principal', async ({ page }) => {
    await page.goto('/sobre-kin');

    await expect(page.getByRole('heading', { level: 1, name: 'Sobre KIN' })).toBeVisible();
    await expect(
      page.getByText(
        'Una plataforma tecnológica creada para transformar ideas e información dispersa en proyectos más estructurados, analizables y accionables.',
      ),
    ).toBeVisible();
    await expect(page.getByText('Luis Orlando Guerra González').first()).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Engineering Showcase' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: '¿Por qué nació KIN?' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'La evolución de KIN' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Knowledge Engine: conocimiento externo controlado' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Capacidad de adaptarse a diferentes contextos' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Tecnologías y capacidades demostradas' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Arquitectura de KIN', exact: true }),
    ).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Lo que KIN demuestra' })).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Mi visión como desarrollador' }),
    ).toBeVisible();
    await expect(page.getByText('Y todavía queda mucho por aprender.')).toBeVisible();
  });

  test('navegación hacia Sobre KIN desde la portada', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: 'Sobre KIN' }).first().click();

    await page.waitForURL(/\/sobre-kin/);
    await expect(page.getByRole('heading', { level: 1, name: 'Sobre KIN' })).toBeVisible();
  });

  test('el perfil enlaza recursos externos reales', async ({ page }) => {
    await page.goto('/sobre-kin');

    const github = page.getByRole('link', { name: /GitHub/ });
    const linkedin = page.getByRole('link', { name: /LinkedIn/ });

    await expect(github).toBeVisible();
    await expect(linkedin).toBeVisible();
    await expect(github).toHaveAttribute('href', /github\.com/);
    await expect(linkedin).toHaveAttribute('href', /linkedin\.com/);
    await expect(github).toHaveAttribute('target', '_blank');
    await expect(linkedin).toHaveAttribute('target', '_blank');
  });

  test('responsive: la página no desborda horizontalmente en móvil', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/sobre-kin');

    const overflows = await page.evaluate(() => {
      const doc = document.documentElement;
      return doc.scrollWidth > doc.clientWidth;
    });
    expect(overflows).toBe(false);
  });
});
