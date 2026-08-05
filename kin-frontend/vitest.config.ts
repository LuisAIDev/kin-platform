import path from "node:path";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Configuración de tests unitarios del módulo Enterprise integrado en
// kin-frontend (Fase 10, M3F). El alias "@" apunta a ./src, igual que en
// tsconfig, para que los tests usen las mismas rutas que la aplicación.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(process.cwd(), "src") },
  },
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
    // Los E2E de Playwright viven en tests/ y se ejecutan con `npx playwright test`.
    exclude: ["tests/**", "node_modules/**", ".next/**", "dist/**"],
    coverage: {
      provider: "v8",
      include: [
        "src/components/**",
        "src/hooks/**",
        "src/services/**",
        "src/utils/**",
      ],
      exclude: [
        "src/components/**/*.test.*",
        "src/hooks/**/*.test.*",
        "src/services/**/*.test.*",
        "src/test/**",
      ],
    },
  },
});
