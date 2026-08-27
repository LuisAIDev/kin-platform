import { test, expect } from '@playwright/test';

test.describe('Arquitectura técnica', () => {
  test('la página pública renderiza el contenido principal', async ({ page }) => {
    await page.goto('/arquitectura');

    await expect(
      page.getByRole('heading', { level: 1, name: 'Arquitectura técnica de KIN' }),
    ).toBeVisible();
    await expect(
      page.getByText(
        'Una plataforma real de inteligencia y estructuración estratégica construida con arquitectura determinista, IA aplicada, seguridad, testing automatizado y cloud.',
      ),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Java decide. El LLM únicamente comunica.' }),
    ).toBeVisible();
    await expect(page.getByText('etapas de procesamiento')).toBeVisible();
    await expect(
      page.getByRole('heading', { name: /Knowledge Engine/ }),
    ).toBeVisible();
    await expect(page.getByText('categorías de proyecto')).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'KIN en una mirada técnica' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Calidad verificada con pruebas reales' }),
    ).toBeVisible();
    await expect(
      page.getByRole('heading', {
        name: '¿Qué demuestra KIN desde el punto de vista profesional?',
      }),
    ).toBeVisible();
  });

  test('navegación desde Sobre KIN mediante el CTA de arquitectura', async ({ page }) => {
    await page.goto('/sobre-kin');
    await page.getByRole('link', { name: 'Explorar arquitectura técnica' }).click();

    await page.waitForURL(/\/arquitectura/);
    await expect(
      page.getByRole('heading', { level: 1, name: 'Arquitectura técnica de KIN' }),
    ).toBeVisible();
  });

  test('el CTA de evaluación enlaza al mecanismo de contacto existente', async ({ page }) => {
    await page.goto('/arquitectura');

    const cta = page.getByRole('link', { name: 'Solicitar evaluación técnica' });
    await expect(cta).toBeVisible();
    await expect(cta).toHaveAttribute('href', /^mailto:/);
  });

  test('responsive: la página no desborda horizontalmente en móvil', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/arquitectura');

    const overflows = await page.evaluate(() => {
      const doc = document.documentElement;
      return doc.scrollWidth > doc.clientWidth;
    });
    expect(overflows).toBe(false);
  });
});
