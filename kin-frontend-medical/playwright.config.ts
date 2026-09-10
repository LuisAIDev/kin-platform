import { defineConfig } from "@playwright/test";

const e2eApiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
process.env.NEXT_PUBLIC_API_URL = e2eApiUrl;

export default defineConfig({
  testDir: "./tests",
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:3001",
    headless: true,
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3001",
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
    env: { NEXT_PUBLIC_API_URL: e2eApiUrl },
  },
});
