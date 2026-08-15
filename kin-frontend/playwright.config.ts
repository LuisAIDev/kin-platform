import { defineConfig } from '@playwright/test';

const e2eApiUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081/api/v1';
process.env.NEXT_PUBLIC_API_URL = e2eApiUrl;

export default defineConfig({
  testDir: './tests',
  // tests/diagnostics/* se excluye de la suite principal. Para ejecutarlo como
  // prueba de regresión, correr con PLAYWRIGHT_DIAGNOSTICS=1.
  testIgnore: process.env.PLAYWRIGHT_DIAGNOSTICS === '1' ? [] : /diagnostics\//,

  use: {
    baseURL: 'http://localhost:3100',
    headless: true,
  },

  webServer: [
    {
      command: 'npm run dev -- -p 3100',
      url: 'http://localhost:3100',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
      env: { NEXT_PUBLIC_API_URL: e2eApiUrl },
    },
  ],
});
