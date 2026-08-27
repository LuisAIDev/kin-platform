import { describe, expect, it } from "vitest";
import { ABOUT_CONTENT, ABOUT_LINKS } from "./aboutContent";

/**
 * Integridad del contenido público de "/sobre-kin": las afirmaciones deben
 * estar respaldadas por el código real y no exponer el repositorio privado.
 */
describe("aboutContent — narrativa de /sobre-kin", () => {
  it("la evolución de KIN tiene 9 etapas conceptuales", () => {
    expect(ABOUT_CONTENT.evolution.stages).toHaveLength(9);
    expect(ABOUT_CONTENT.evolution.stages[0].label).toBe("Idea");
    expect(ABOUT_CONTENT.evolution.stages[8].label).toBe("Evolución continua");
  });

  it("la sección de IA introduce el principio del proyecto", () => {
    expect(ABOUT_CONTENT.aiNarrative.principle).toBe("Java decide. El LLM únicamente comunica.");
  });

  it("la audiencia incluye 4 grupos", () => {
    expect(ABOUT_CONTENT.audience.items).toHaveLength(4);
    expect(ABOUT_CONTENT.audience.items[3].title).toBe("Personas que están aprendiendo");
  });

  it("la vertical de Salud usa lenguaje orientativo con disclaimers", () => {
    expect(ABOUT_CONTENT.health.disclaimers.length).toBeGreaterThanOrEqual(3);
    const joined = ABOUT_CONTENT.health.disclaimers.join(" ").toLowerCase();
    expect(joined).toContain("no sustituye");
    expect(joined).toContain("orientativa");
    expect(joined).not.toContain("diagnóstico médico real garantizado");
  });

  it("GitHub enlaza el perfil del creador, no el repositorio privado", () => {
    expect(ABOUT_LINKS.github.href).toBe("https://github.com/LuisAIDev");
    expect(ABOUT_LINKS.github.href).not.toContain("/kin-platform");
  });

  it("ningún texto describe GitHub como repositorio público", () => {
    const serialized = JSON.stringify(ABOUT_CONTENT).toLowerCase();
    expect(serialized).not.toContain("repositorio público");
    expect(serialized).not.toContain("repositorio publico");
    expect(serialized).not.toContain("código fuente está públicamente disponible");
  });

  it("no expone secretos ni credenciales en el contenido público", () => {
    const serialized = JSON.stringify(ABOUT_CONTENT);
    expect(serialized).not.toMatch(/sk-[A-Za-z0-9]+/);
    expect(serialized).not.toMatch(/password[: =]/i);
    expect(serialized).not.toMatch(/https?:\/\/[^\s"']+/);
  });
});
