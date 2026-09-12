import { NextResponse } from 'next/server';

const PRODUCTION_BACKEND = 'https://kin-backend-lwmy.onrender.com/api/v1';
const LOCAL_BACKEND = 'http://localhost:8080/api/v1';

/**
 * Endpoint de diagnóstico (solo lectura) para verificar a qué backend apunta
 * el frontend en producción y si es alcanzable desde Vercel.
 *
 * Accesible en: GET /api/health-check
 */
export async function GET() {
  const apiBase =
    process.env.VERCEL === '1'
      ? PRODUCTION_BACKEND
      : process.env.NEXT_PUBLIC_API_URL || LOCAL_BACKEND;

  try {
    const res = await fetch(`${apiBase}/actuator/health`, {
      method: 'GET',
      cache: 'no-store',
    });
    const data = await res.json().catch(() => null);
    return NextResponse.json({
      apiBase,
      backendStatus: res.status,
      backendResponse: data,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return NextResponse.json(
      {
        apiBase,
        error: String(error),
        timestamp: new Date().toISOString(),
      },
      { status: 500 }
    );
  }
}
