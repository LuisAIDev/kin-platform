import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { canAccessPath, homePathForRole } from "./utils/roles";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const ME_TTL_MS = 30_000;

const meCache = new Map<
  string,
  { ok: boolean; verified: boolean; role: string | null; expiresAt: number }
>();

interface MeInfo {
  ok: boolean;
  verified: boolean;
  role: string | null;
}

async function checkSession(token: string): Promise<MeInfo> {
  const cached = meCache.get(token);
  const now = Date.now();
  if (cached && cached.expiresAt > now) {
    return cached;
  }

  try {
    const res = await fetch(`${API_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const ok = res.ok;
    let verified = true;
    let role: string | null = null;
    if (ok) {
      const body = await res.json().catch(() => null);
      verified = body?.emailVerified !== false;
      role = body?.role ?? null;
    }
    const result = { ok, verified, role };
    meCache.set(token, { ...result, expiresAt: now + ME_TTL_MS });
    return result;
  } catch {
    // Error de red al validar la sesión: NO asumir autenticado (evitar falsos
    // positivos que redirigirían /login → /dashboard en bucle). Se trata como
    // no-autenticado: el middleware dejará pasar a /login y/o forzará logout.
    const result = { ok: false, verified: false, role: null };
    meCache.set(token, { ...result, expiresAt: now + ME_TTL_MS });
    return result;
  }
}

function buildLoginRedirect(request: NextRequest) {
  return NextResponse.redirect(new URL("/login", request.url));
}

export default async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("kin_token_v2")?.value;

  if (pathname.startsWith("/dashboard")) {
    if (!token) {
      return buildLoginRedirect(request);
    }

    const { ok, verified, role } = await checkSession(token);

    if (!ok) {
      const response = buildLoginRedirect(request);
      response.cookies.delete("kin_session_v2");
      response.cookies.delete("kin_token_v2");
      response.cookies.set("kin_force_logout", "true", {
        path: "/",
        maxAge: 60,
        sameSite: "lax",
      });
      return response;
    }

    if (!verified) {
      return NextResponse.redirect(new URL("/verify-email", request.url));
    }

    // Segmentación por vertical: bloquea acceso a rutas de otra vertical y
    // redirige la raíz `/dashboard` al home del rol.
    if (pathname === "/dashboard" || !canAccessPath(role, pathname)) {
      return NextResponse.redirect(new URL(homePathForRole(role), request.url));
    }
  }

  if (pathname === "/login" && token) {
    const { ok, role } = await checkSession(token);
    if (ok) {
      return NextResponse.redirect(new URL(homePathForRole(role), request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/login"],
};
