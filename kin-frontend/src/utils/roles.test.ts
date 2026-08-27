import { describe, expect, it } from "vitest";
import {
  canAccessPath,
  homePathForRole,
  isAdminRole,
  isBusinessRole,
  isHealthRole,
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
});
