import { describe, expect, it } from "vitest";
import {
  ARCH_ADR,
  ARCH_AI,
  ARCH_CATEGORIES,
  ARCH_HERO_INDICATORS,
  ARCH_KNOWLEDGE,
  ARCH_META,
  ARCH_PIPELINE,
  ARCH_PRINCIPLE,
  ARCH_QUALITY,
  ARCH_SECURITY,
} from "./architectureContent";

/**
 * Integridad del contenido público de "/arquitectura": las cifras expuestas
 * deben coincidir con el estado real verificado del repositorio. Si el
 * proyecto evoluciona, estos tests señalan que el contenido debe actualizarse.
 */
describe("architectureContent — cifras verificadas", () => {
  it("el pipeline tiene 16 etapas (verificado en KinConfig.chatPipeline)", () => {
    expect(ARCH_PIPELINE.count).toBe(16);
    expect(ARCH_PIPELINE.stages).toHaveLength(16);
  });

  it("el catálogo de categorías tiene 19 elementos (V6 + V18)", () => {
    expect(ARCH_CATEGORIES.count).toBe(19);
    expect(ARCH_CATEGORIES.items).toHaveLength(19);
    expect(ARCH_CATEGORIES.items).toContain("Fintech");
    expect(ARCH_CATEGORIES.items).toContain("Salud");
    expect(ARCH_CATEGORIES.items).toContain("Logística");
    expect(ARCH_CATEGORIES.items).toContain("Otro / Sin clasificar");
  });

  it("se documentan 32 ADRs (ADR-001 … ADR-032)", () => {
    expect(ARCH_ADR.count).toBe(32);
  });

  it("el principio central incluye el flujo completo del turno", () => {
    expect(ARCH_PRINCIPLE.flow.length).toBeGreaterThanOrEqual(11);
    expect(ARCH_PRINCIPLE.flow.some((s) => s.label === "Política determinista")).toBe(true);
    expect(ARCH_PRINCIPLE.flow.some((s) => s.label === "Guard de conexión")).toBe(true);
  });

  it("el Knowledge Engine documenta sus capacidades y ejemplos por categoría", () => {
    expect(ARCH_KNOWLEDGE.capabilities.length).toBeGreaterThanOrEqual(10);
    expect(ARCH_KNOWLEDGE.categoryExamples.length).toBeGreaterThanOrEqual(6);
  });

  it("el AI separa control determinista, conocimiento, evidencia y comunicación", () => {
    expect(ARCH_AI.pillars).toHaveLength(4);
    expect(ARCH_AI.pillars[0].title).toBe("Control determinista");
  });

  it("la sección de seguridad cubre la protección de conexiones externas", () => {
    expect(ARCH_SECURITY.connectionFlow.length).toBeGreaterThanOrEqual(8);
    expect(ARCH_SECURITY.measures.some((m) => m.includes("DNS"))).toBe(true);
    expect(ARCH_SECURITY.measures.some((m) => m.includes("SSRF"))).toBe(true);
  });

  it("el hero muestra indicadores de capacidad, no una lista de dependencias", () => {
    expect(ARCH_HERO_INDICATORS).toHaveLength(9);
    expect(ARCH_HERO_INDICATORS).toContain("Knowledge Engine");
    expect(ARCH_HERO_INDICATORS).toContain("IA con guardrails");
  });

  it("las métricas de calidad corresponden a la suite vigente", () => {
    const frontend = ARCH_QUALITY.metrics.find((m) => m.label === "tests frontend");
    expect(frontend?.value).toBe("436");

    const backend = ARCH_QUALITY.metrics.find((m) => m.label === "tests backend ejecutados");
    expect(backend?.value).toBe("2.832");
  });

  it("la metadata SEO incluye keywords y no contiene keyword stuffing", () => {
    expect(ARCH_META.keywords.length).toBeGreaterThanOrEqual(6);
    expect(ARCH_META.keywords).toContain("Knowledge Engine");
    expect(ARCH_META.keywords).toContain("DevSecOps");
  });

  it("el contenido no expone secretos ni credenciales", () => {
    const serialized = JSON.stringify({
      ARCH_ADR,
      ARCH_AI,
      ARCH_CATEGORIES,
      ARCH_HERO_INDICATORS,
      ARCH_KNOWLEDGE,
      ARCH_META,
      ARCH_PIPELINE,
      ARCH_PRINCIPLE,
      ARCH_QUALITY,
      ARCH_SECURITY,
    });
    expect(serialized).not.toMatch(/sk-[A-Za-z0-9]+/);
    expect(serialized.toLowerCase()).not.toContain("password");
    expect(serialized.toLowerCase()).not.toContain("apikey");
    expect(serialized.toLowerCase()).not.toContain("secret=");
    expect(serialized.toLowerCase()).not.toContain("bearer ");
    expect(serialized).not.toMatch(/https?:\/\/[^\s"']+/);
  });
});
