import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      // Cliente auth (login, register, verify-email) apunta directamente a NEXT_PUBLIC_API_URL
      // en api.ts. Este rewrite es solo para SSR/edge requests que pasan por Next.js.
      {
        source: '/api/medical/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'}/medical/:path*`,
      },
      {
        source: '/api/auth/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'}/auth/:path*`,
      },
    ];
  },
};

export default nextConfig;