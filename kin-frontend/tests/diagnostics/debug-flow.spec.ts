/**
 * DIAGNÓSTICO — flujo E2E completo (login → dashboard → nueva categoría).
 *
 * Este spec se creó para diagnosticar el problema de Redis/Subscription:
 * - `/api/v1/subscriptions/status` devolvía HTTP 500 porque Spring Boot
 *   auto-configuraba RedisCacheManager (spring-boot-starter-data-redis en el
 *   classpath) sin servidor Redis, y `@Cacheable` de SubscriptionValidatorService
 *   fallaba al conectar. El fix fue `spring.cache.type=simple` en
 *   `kin-backend/src/main/resources/application.yml`.
 * - Tras el fix se verificó que el login permanece autenticado, que no hay
 *   redirect inesperado hacia /login, y que /categories carga las opciones.
 *
 * NO se ejecuta en la suite principal: `tests/diagnostics/**` está excluido vía
 * `testIgnore` en `playwright.config.ts`. Para usarlo en futuras regresiones,
 * correrlo de forma explícita:
 *
 *   cd kin-frontend && PLAYWRIGHT_DIAGNOSTICS=1 npx playwright test tests/diagnostics/debug-flow.spec.ts
 */
import { test } from '@playwright/test';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const EMAIL = `dbg-${Date.now()}@kin.test`;
const PASS = 'TestPass123';

test('debug flow', async ({ page, request }) => {
  const reg = await request.post(`${API_URL}/auth/register`, {
    data: { email: EMAIL, password: PASS, fullName: 'Debug' },
  });
  console.log('[DBG] register status', reg.status());

  page.on('response', (res) => {
    console.log('[DBG] response', res.status(), res.url());
  });
  page.on('requestfailed', (req) => {
    console.log('[DBG] requestfailed', req.method(), req.url(), req.failure()?.errorText);
  });
  page.on('console', (msg) => {
    if (msg.type() === 'error' || msg.type() === 'warning') {
      console.log('[DBG] console', msg.type(), msg.text());
    }
  });

  await page.goto('/login');
  await page.locator('input[type="email"]').fill(EMAIL);
  await page.locator('input[type="password"]').fill(PASS);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.waitForURL(/\/dashboard/);
  console.log('[DBG] after login URL', page.url());

  await page.goto('/dashboard/projects/new');
  console.log('[DBG] after goto new URL', page.url());
  await page.locator('input#title').fill('Debug Project');

  await page.waitForTimeout(3000);
  console.log('[DBG] before select URL', page.url());
  const optionCount = await page.locator('select#category option').count();
  console.log('[DBG] category option count', optionCount);
  const selectHtml = await page.locator('select#category').evaluate((el: HTMLSelectElement) => el.innerHTML);
  console.log('[DBG] select html', selectHtml);

  const state = await page.evaluate(() => ({
    url: window.location.href,
    token: localStorage.getItem('kin_token_v2') ? 'present' : 'absent',
  }));
  console.log('[DBG] state', JSON.stringify(state));

  await page.locator('select#category').selectOption({ index: 1 });
  console.log('[DBG] selected, URL now', page.url());
});
