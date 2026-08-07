import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const ME_TTL_MS = 30_000;

const meCache = new Map<string, { ok: boolean; expiresAt: number }>();

async function checkSession(token: string): Promise<boolean> {
  const cached = meCache.get(token);
  const now = Date.now();
  if (cached && cached.expiresAt > now) {
    return cached.ok;
  }

  try {
    const res = await fetch(`${API_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const ok = res.ok;
    meCache.set(token, { ok, expiresAt: now + ME_TTL_MS });
    return ok;
  } catch {
    meCache.set(token, { ok: true, expiresAt: now + ME_TTL_MS });
    return true;
  }
}

export default async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("kin_token_v2")?.value;

  if (pathname.startsWith("/dashboard")) {
    if (!token) {
      return NextResponse.next();
    }

    const valid = await checkSession(token);
    if (!valid) {
      const response = NextResponse.redirect(new URL("/login", request.url));
      response.cookies.delete("kin_session_v2");
      response.cookies.delete("kin_token_v2");
      response.cookies.set("kin_force_logout", "true", {
        path: "/",
        maxAge: 60,
        sameSite: "lax",
      });
      return response;
    }
  }

  if (pathname === "/login" && token) {
    const valid = await checkSession(token);
    if (valid) {
      return NextResponse.redirect(new URL("/dashboard/projects", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/login"],
};
