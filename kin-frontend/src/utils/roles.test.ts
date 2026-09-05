import { describe, expect, it } from "vitest";
import {
  canAccessPath,
  hasPhysicianCapability,
  homePathForRole,
  isAccountPendingReview,
  isAccountRejected,
  isAccountUnderReview,
  isAdminRole,
  isBusinessRole,
  isHealthRole,
  isPhysicianRole,
  resolveVertical,
  verticalForRole,
} from "./roles";

describe("roles utils", () => {
  describe("verticalForRole", () => {
    it("mapea roles empresariales a la vertical empresa", () => {
      expect(verticalForRole("FREE")).toBe("empresa");
      expect(verticalForRole("PREMIUM")).toBe("empresa");
      expect(verticalForRole("FACILITADOR")).toBe("empresa");
      expect(verticalForRole("USER")).toBe("empresa");
    });

    it("mapea roles de salud a la vertical salud", () => {
      expect(verticalForRole("PATIENT")).toBe("salud");
      expect(verticalForRole("PHYSICIAN")).toBe("salud");
    });

    it("mapea ADMIN a la vertical admin y undefined a empresa", () => {
      expect(verticalForRole("ADMIN")).toBe("admin");
      expect(verticalForRole(undefined)).toBe("empresa");
      expect(verticalForRole(null)).toBe("empresa");
    });
  });

  describe("homePathForRole", () => {
    it("devuelve el hub de cada vertical", () => {
      expect(homePathForRole("FREE")).toBe("/dashboard/empresa");
      expect(homePathForRole("PATIENT")).toBe("/dashboard/salud");
      expect(homePathForRole("PHYSICIAN")).toBe("/dashboard/salud");
      expect(homePathForRole("ADMIN")).toBe("/dashboard/admin");
    });
  });

  describe("resolveVertical (ROLE ≠ VERTICAL)", () => {
    it("sin selección conserva el comportamiento previo (vertical del rol)", () => {
      expect(resolveVertical("FREE")).toBe("empresa");
      expect(resolveVertical("PATIENT")).toBe("salud");
      expect(resolveVertical("PHYSICIAN")).toBe("salud");
      expect(resolveVertical("ADMIN")).toBe("admin");
      expect(resolveVertical("FREE", null)).toBe("empresa");
      expect(resolveVertical("FREE", undefined)).toBe("empresa");
    });

    it("roles empresariales respetan la selección de vertical", () => {
      expect(resolveVertical("FREE", "salud")).toBe("salud");
      expect(resolveVertical("PREMIUM", "salud")).toBe("salud");
      expect(resolveVertical("FACILITADOR", "salud")).toBe("salud");
      expect(resolveVertical("FREE", "empresa")).toBe("empresa");
    });

    it("ADMIN ignora la selección (mantiene su hub)", () => {
      expect(resolveVertical("ADMIN", "salud")).toBe("admin");
      expect(resolveVertical("ADMIN", "empresa")).toBe("admin");
    });

    it("roles de salud ignoran la selección", () => {
      expect(resolveVertical("PATIENT", "empresa")).toBe("salud");
      expect(resolveVertical("PHYSICIAN", "empresa")).toBe("salud");
    });

    it("valores inválidos no se aceptan", () => {
      expect(resolveVertical("FREE", "admin" as never)).toBe("empresa");
    });
  });

  describe("homePathForRole con vertical seleccionada", () => {
    it("FREE + salud → /dashboard/salud; FREE + empresa → /dashboard/empresa", () => {
      expect(homePathForRole("FREE", "salud")).toBe("/dashboard/salud");
      expect(homePathForRole("FREE", "empresa")).toBe("/dashboard/empresa");
      expect(homePathForRole("PREMIUM", "salud")).toBe("/dashboard/salud");
    });

    it("sin selección conserva el home del rol", () => {
      expect(homePathForRole("FREE")).toBe("/dashboard/empresa");
      expect(homePathForRole("PATIENT")).toBe("/dashboard/salud");
    });
  });

  describe("isAdminRole / isHealthRole / isBusinessRole", () => {
    it("distingue admin", () => {
      expect(isAdminRole("ADMIN")).toBe(true);
      expect(isAdminRole("PATIENT")).toBe(false);
    });

    it("distingue salud", () => {
      expect(isHealthRole("PATIENT")).toBe(true);
      expect(isHealthRole("PHYSICIAN")).toBe(true);
      expect(isHealthRole("FREE")).toBe(false);
    });

    it("distingue empresa", () => {
      expect(isBusinessRole("FREE")).toBe(true);
      expect(isBusinessRole("PATIENT")).toBe(false);
      expect(isBusinessRole("ADMIN")).toBe(false);
    });
  });

  describe("canAccessPath", () => {
    it("ADMIN accede a todas las rutas", () => {
      expect(canAccessPath("ADMIN", "/dashboard/empresa")).toBe(true);
      expect(canAccessPath("ADMIN", "/dashboard/salud")).toBe(true);
      expect(canAccessPath("ADMIN", "/dashboard/projects")).toBe(true);
      expect(canAccessPath("ADMIN", "/dashboard/patient/health")).toBe(true);
      expect(canAccessPath("ADMIN", "/dashboard/physician")).toBe(true);
      expect(canAccessPath("ADMIN", "/dashboard/admin/pricing")).toBe(true);
    });

    it("empresa ve solo rutas empresariales", () => {
      expect(canAccessPath("FREE", "/dashboard/empresa")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/projects")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/analytics")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/insights")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/settings")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/patient/health")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/physician")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/admin/pricing")).toBe(false);
    });

    it("paciente ve solo rutas de salud de paciente", () => {
      expect(canAccessPath("PATIENT", "/dashboard/salud")).toBe(true);
      expect(canAccessPath("PATIENT", "/dashboard/patient/health")).toBe(true);
      expect(canAccessPath("PATIENT", "/dashboard/patient/messages")).toBe(true);
      expect(canAccessPath("PATIENT", "/dashboard/settings")).toBe(true);
      expect(canAccessPath("PATIENT", "/dashboard/physician")).toBe(false);
      expect(canAccessPath("PATIENT", "/dashboard/empresa")).toBe(false);
      expect(canAccessPath("PATIENT", "/dashboard/projects")).toBe(false);
      expect(canAccessPath("PATIENT", "/dashboard/analytics")).toBe(false);
      expect(canAccessPath("PATIENT", "/dashboard/admin")).toBe(false);
    });

    it("médico ve solo rutas de salud de médico", () => {
      expect(canAccessPath("PHYSICIAN", "/dashboard/salud")).toBe(true);
      expect(canAccessPath("PHYSICIAN", "/dashboard/physician")).toBe(true);
      expect(canAccessPath("PHYSICIAN", "/dashboard/physician/messages")).toBe(true);
      expect(canAccessPath("PHYSICIAN", "/dashboard/settings")).toBe(true);
      expect(canAccessPath("PHYSICIAN", "/dashboard/patient/health")).toBe(false);
      expect(canAccessPath("PHYSICIAN", "/dashboard/projects")).toBe(false);
      expect(canAccessPath("PHYSICIAN", "/dashboard/empresa")).toBe(false);
    });

    it("la raíz /dashboard se permite (el hub decide el redirect)", () => {
      expect(canAccessPath("FREE", "/dashboard")).toBe(true);
      expect(canAccessPath("PATIENT", "/dashboard")).toBe(true);
    });
  });

  describe("canAccessPath con vertical seleccionada (empresario en Salud)", () => {
    it("FREE + salud accede al hub y a la subárea de paciente", () => {
      expect(canAccessPath("FREE", "/dashboard/salud", "salud")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/patient/health", "salud")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/patient/triage", "salud")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/patient/messages", "salud")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/patient/appointments", "salud")).toBe(true);
      expect(canAccessPath("FREE", "/dashboard/settings", "salud")).toBe(true);
    });

    it("PREMIUM/FACILITADOR + salud acceden a la subárea de paciente", () => {
      expect(canAccessPath("PREMIUM", "/dashboard/salud", "salud")).toBe(true);
      expect(canAccessPath("FACILITADOR", "/dashboard/patient/health", "salud")).toBe(true);
    });

    it("FREE + salud NO accede a rutas physician ni admin (sin elevación)", () => {
      expect(canAccessPath("FREE", "/dashboard/physician", "salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/physician/messages", "salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/admin", "salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/admin/pricing", "salud")).toBe(false);
    });

    it("FREE + salud NO accede a rutas empresariales (contexto aislado)", () => {
      expect(canAccessPath("FREE", "/dashboard/empresa", "salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/projects", "salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/analytics", "salud")).toBe(false);
    });

    it("PHYSICIAN conserva sus restricciones (no accede a subárea de paciente)", () => {
      expect(canAccessPath("PHYSICIAN", "/dashboard/physician", "salud")).toBe(true);
      expect(canAccessPath("PHYSICIAN", "/dashboard/patient/health", "salud")).toBe(false);
      expect(canAccessPath("PHYSICIAN", "/dashboard/projects", "salud")).toBe(false);
    });

    it("sin selección, un empresario sigue sin acceder a Salud", () => {
      expect(canAccessPath("FREE", "/dashboard/salud")).toBe(false);
      expect(canAccessPath("FREE", "/dashboard/patient/health")).toBe(false);
    });
  });

  describe("estado pendiente de revisión del médico", () => {
    it("detecta médico con cuenta pendiente", () => {
      expect(isAccountPendingReview({ role: "PHYSICIAN", verificationStatus: "PENDING" })).toBe(true);
      expect(isAccountPendingReview({ role: "PHYSICIAN", verificationStatus: "APPROVED" })).toBe(false);
      expect(isAccountPendingReview({ role: "PATIENT", verificationStatus: "PENDING" })).toBe(false);
      expect(isAccountPendingReview(null)).toBe(false);
    });

    it("detecta médico con cuenta rechazada", () => {
      expect(isAccountRejected({ role: "PHYSICIAN", verificationStatus: "REJECTED" })).toBe(true);
      expect(isAccountRejected({ role: "PHYSICIAN", verificationStatus: "PENDING" })).toBe(false);
    });

    it("considera 'en revisión' tanto PENDING como REJECTED", () => {
      expect(isAccountUnderReview({ role: "PHYSICIAN", verificationStatus: "PENDING" })).toBe(true);
      expect(isAccountUnderReview({ role: "PHYSICIAN", verificationStatus: "REJECTED" })).toBe(true);
      expect(isAccountUnderReview({ role: "PHYSICIAN", verificationStatus: "APPROVED" })).toBe(false);
      expect(isAccountUnderReview({ role: "PHYSICIAN" })).toBe(false);
    });
  });

  describe("capacidad profesional (physicianCapability, Alternativa B)", () => {
    it("hasPhysicianCapability: usa physicianCapability del backend como fuente de verdad", () => {
      expect(hasPhysicianCapability({ role: "FREE", physicianCapability: true })).toBe(true);
      expect(hasPhysicianCapability({ role: "PREMIUM", physicianCapability: true })).toBe(true);
      expect(hasPhysicianCapability({ role: "PATIENT", physicianCapability: true })).toBe(true);
      expect(hasPhysicianCapability({ role: "PHYSICIAN", physicianCapability: true })).toBe(true);
    });

    it("hasPhysicianCapability: PENDING/REJECTED no tienen capacidad aunque role sea el adecuado", () => {
      expect(hasPhysicianCapability({ role: "FREE", physicianCapability: false })).toBe(false);
      expect(hasPhysicianCapability({ role: "PHYSICIAN", physicianCapability: false })).toBe(false);
      expect(hasPhysicianCapability({ role: "FREE", verificationStatus: "PENDING" })).toBe(false);
    });

    it("hasPhysicianCapability: fallback a role PHYSICIAN si la sesión antigua no trae el campo", () => {
      expect(hasPhysicianCapability({ role: "PHYSICIAN" })).toBe(true);
      expect(hasPhysicianCapability({ role: "FREE" })).toBe(false);
      expect(hasPhysicianCapability(null)).toBe(false);
      expect(hasPhysicianCapability(undefined)).toBe(false);
    });

    it("isPhysicianRole con objeto usa la capacidad (no solo role)", () => {
      expect(isPhysicianRole({ role: "FREE", physicianCapability: true })).toBe(true);
      expect(isPhysicianRole({ role: "FREE", physicianCapability: false })).toBe(false);
      expect(isPhysicianRole({ role: "PHYSICIAN", physicianCapability: true })).toBe(true);
      expect(isPhysicianRole({ role: "PHYSICIAN", physicianCapability: false })).toBe(false);
    });

    it("isPhysicianRole con string conserva compatibilidad (role === PHYSICIAN)", () => {
      expect(isPhysicianRole("PHYSICIAN")).toBe(true);
      expect(isPhysicianRole("FREE")).toBe(false);
      expect(isPhysicianRole(null)).toBe(false);
    });
  });

  describe("canAccessPath con capacidad profesional (FREE+PENDING / FREE+APPROVED / PHYSICIAN legacy)", () => {
    it("FREE+APPROVED (physicianCapability true) puede acceder al portal médico", () => {
      const freeApproved = { role: "FREE", physicianCapability: true, verificationStatus: "APPROVED" };
      expect(canAccessPath(freeApproved, "/dashboard/physician")).toBe(true);
      expect(canAccessPath(freeApproved, "/dashboard/physician/messages")).toBe(true);
      expect(canAccessPath(freeApproved, "/dashboard/physician", "salud")).toBe(true);
    });

    it("FREE+PENDING NO puede acceder al portal médico, pero sí ve su dashboard de Empresas", () => {
      const freePending = { role: "FREE", physicianCapability: false, verificationStatus: "PENDING" };
      expect(canAccessPath(freePending, "/dashboard/physician")).toBe(false);
      expect(canAccessPath(freePending, "/dashboard/physician/messages")).toBe(false);
      expect(canAccessPath(freePending, "/dashboard/physician", "salud")).toBe(false);
      expect(canAccessPath(freePending, "/dashboard/empresa")).toBe(true);
      expect(canAccessPath(freePending, "/dashboard/projects")).toBe(true);
    });

    it("PHYSICIAN+APPROVED (legacy) puede acceder al portal médico", () => {
      const legacy = { role: "PHYSICIAN", physicianCapability: true, verificationStatus: "APPROVED" };
      expect(canAccessPath(legacy, "/dashboard/physician")).toBe(true);
      expect(canAccessPath(legacy, "/dashboard/salud")).toBe(true);
      // No accede a la subárea de paciente (rol PHYSICIAN legacy).
      expect(canAccessPath(legacy, "/dashboard/patient/health")).toBe(false);
    });

    it("PREMIUM+APPROVED puede acceder al portal médico", () => {
      const premiumApproved = { role: "PREMIUM", physicianCapability: true, verificationStatus: "APPROVED" };
      expect(canAccessPath(premiumApproved, "/dashboard/physician")).toBe(true);
    });

    it("PATIENT+APPROVED puede acceder al portal médico", () => {
      const patientApproved = { role: "PATIENT", physicianCapability: true, verificationStatus: "APPROVED" };
      expect(canAccessPath(patientApproved, "/dashboard/physician")).toBe(true);
    });

    it("sin capacidad, un FREE no accede al portal médico aunque seleccione salud", () => {
      expect(canAccessPath({ role: "FREE", physicianCapability: false }, "/dashboard/physician", "salud")).toBe(false);
    });
  });
});
