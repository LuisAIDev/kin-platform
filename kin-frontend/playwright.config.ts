import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // tests/diagnostics/* se excluye de la suite principal. Para ejecutarlo como
  // prueba de regresión, correr con PLAYWRIGHT_DIAGNOSTICS=1.
  testIgnore: process.env.PLAYWRIGHT_DIAGNOSTICS === '1' ? [] : /diagnostics\//,

  use: {
    baseURL: 'http://localhost:3000',
    headless: !!process.env.CI,
  },

  webServer: [
    {
      command: 'npm run dev',
      url: 'http://localhost:3000',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
  ],
});
