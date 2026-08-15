import { afterEach, describe, expect, it, vi } from "vitest";
import { authService } from "@/services/auth";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const sessionUser = {
  token: "t",
  email: "a@b.c",
  fullName: "Ana",
  role: "FREE",
  emailVerified: true,
};

const pendingUser = {
  token: null,
  email: "a@b.c",
  fullName: "Ana",
  role: "FREE",
  emailVerified: false,
};

const { mockedForceLogout } = vi.hoisted(() => ({ mockedForceLogout: vi.fn() }));
vi.mock("@/services/session", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/session")>();
  return { ...actual, forceLogout: mockedForceLogout };
});

describe("authService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    mockedForceLogout.mockReset();
  });

  it("register: éxito NO guarda sesión (verificación requerida)", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(pendingUser, 201));

    const result = await authService.register({ fullName: "Ana", email: "a@b.c", password: "x" });

    expect(result.error).toBeNull();
    expect(result.data?.token).toBeNull();
    expect(result.data?.emailVerified).toBe(false);
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
    expect(sessionStorage.getItem("kin_pending_email")).toBe("a@b.c");
  });

  it("register: error devuelve mensaje sin guardar sesión", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ error: "El email ya está registrado" }, 400)
    );

    const result = await authService.register({ fullName: "Ana", email: "a@b.c", password: "x" });

    expect(result.data).toBeNull();
    expect(result.error).toBe("El email ya está registrado");
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
  });

  it("login: éxito guarda sesión", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(sessionUser));

    const result = await authService.login({ email: "a@b.c", password: "x" });

    expect(result.error).toBeNull();
    expect(result.data?.token).toBe("t");
    expect(localStorage.getItem("kin_token_v2")).toBe("t");
  });

  it("login: EMAIL_VERIFICATION_REQUIRED devuelve el código sin forzar logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(
        {
          error: "Tu correo electrónico aún no ha sido verificado. Revisa tu bandeja de entrada para activar tu cuenta.",
          code: "EMAIL_VERIFICATION_REQUIRED",
        },
        403
      )
    );

    const result = await authService.login({ email: "a@b.c", password: "x" });

    expect(result.data).toBeNull();
    expect(result.code).toBe("EMAIL_VERIFICATION_REQUIRED");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("login: error 401 devuelve mensaje y fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ error: "Invalid email or password" }, 401)
    );

    const result = await authService.login({ email: "a@b.c", password: "x" });

    expect(result.data).toBeNull();
    expect(result.error).toBe("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("verifyEmail: éxito devuelve mensaje", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ message: "Correo verificado correctamente. Ya puedes iniciar sesión." })
    );

    const result = await authService.verifyEmail("tok");

    expect(result.error).toBeNull();
    expect(result.data?.message).toContain("Correo verificado correctamente");
    expect(String((globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][0])).toContain(
      "/auth/verify-email?token=tok"
    );
  });

  it("verifyEmail: error devuelve mensaje", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ error: "El enlace de verificación ha expirado. Solicita uno nuevo.", code: "EXPIRED" }, 400)
    );

    const result = await authService.verifyEmail("expired");

    expect(result.data).toBeNull();
    expect(result.error).toContain("expirado");
  });

  it("resendVerification: éxito devuelve mensaje genérico", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ message: "Si existe una cuenta asociada a este correo y necesita verificación, recibirás un nuevo mensaje." })
    );

    const result = await authService.resendVerification("a@b.c");

    expect(result.error).toBeNull();
    expect(result.data?.message).toContain("recibirás un nuevo mensaje");
  });

  it("logout: limpia sesión (sin token no llama a la API)", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");

    await authService.logout();

    expect(fetchMock).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
  });

  it("logout: con token llama a /auth/logout y limpia", async () => {
    localStorage.setItem("kin_token_v2", "t");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 200 }));

    await authService.logout();

    expect(String(fetchMock.mock.calls[0][0])).toContain("/auth/logout");
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
  });

  it("getToken/getUser: leen de localStorage", () => {
    localStorage.setItem("kin_token_v2", "tok");
    localStorage.setItem("kin_user_v2", JSON.stringify(sessionUser));

    expect(authService.getToken()).toBe("tok");
    expect(authService.getUser()?.email).toBe("a@b.c");
    expect(authService.getUser()?.fullName).toBe("Ana");
  });

  it("getUser: sin datos devuelve null", () => {
    expect(authService.getUser()).toBeNull();
  });
});
