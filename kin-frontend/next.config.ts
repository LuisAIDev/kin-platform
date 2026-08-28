import type { NextConfig } from "next";
import path from "path";

const nextConfig: NextConfig = {
  turbopack: {
    root: path.resolve(__dirname),
  },
  // Solo dev: permite que el harness E2E acceda al dev server desde el host
  // 127.0.0.1 (same-site con la API 127.0.0.1:8080). Sin efecto en el build
  // de producción.
  allowedDevOrigins: ["127.0.0.1"],
};

export default nextConfig;
