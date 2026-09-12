import type { NextConfig } from 'next';

// Backend por entorno.
//
// IMPORTANTE: `rewrites()` se evalúa en BUILD-TIME. En Vercel usamos SIEMPRE
// la URL de producción hardcodeada para no depender de que NEXT_PUBLIC_API_URL
// esté disponible en build (si faltaba, el fallback era localhost → 404).
const PRODUCTION_BACKEND = 'https://kin-backend-lwmy.onrender.com/api/v1';
const LOCAL_BACKEND = 'http://localhost:8080/api/v1';

const isVercel = process.env.VERCEL === '1';
const apiBase = isVercel
  ? PRODUCTION_BACKEND
  : process.env.NEXT_PUBLIC_API_URL || LOCAL_BACKEND;

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      // Proxy same-origin para TODAS las rutas /api/v1/* (cookies first-party).
      // Va PRIMERO: Next.js evalúa en orden.
      {
        source: '/api/v1/:path*',
        destination: `${apiBase}/:path*`,
      },
      // Rewrites existentes (mantener compatibilidad).
      {
        source: '/api/medical/:path*',
        destination: `${apiBase}/medical/:path*`,
      },
      {
        source: '/api/auth/:path*',
        destination: `${apiBase}/auth/:path*`,
      },
    ];
  },
};

export default nextConfig;
