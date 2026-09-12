import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
    return [
      // Proxy para TODAS las rutas /api/v1/* (cookies first-party).
      // Va PRIMERO: Next.js evalúa en orden, así las peticiones /api/v1/* 
      // se resuelven aquí (mismo origen → cookie first-party) antes que otros.
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