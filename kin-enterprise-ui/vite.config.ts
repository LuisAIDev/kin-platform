/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite + Vitest configuration for the Enterprise Dashboard.
// The backend API base URL is configured at runtime via VITE_API_URL
// (defaults to the local KIN backend).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
    css: false,
    coverage: {
      provider: "v8",
      exclude: [
        "dist/**",
        "src/main.tsx",
        "src/test/**",
        "src/**/*.test.*",
        "src/vite-env.d.ts",
        "vite.config.ts",
      ],
    },
  },
});
