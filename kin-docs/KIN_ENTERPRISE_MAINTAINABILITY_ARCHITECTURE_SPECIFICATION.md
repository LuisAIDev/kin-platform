# KIN_ENTERPRISE_MAINTAINABILITY_ARCHITECTURE_SPECIFICATION.md — FASE 30

**Especificación Oficial de Arquitectura de Mantenibilidad Empresarial (Enterprise Maintainability Architecture) de KIN.**
*Consolidación documental exclusiva de F12–F29 sin modificar ninguno de esos documentos. Compatible al 100 % con Hexagonal, DDD, Clean, CQRS, Event Driven, SOLID, SemVer, Contract Versioning, Evolutionary Architecture, GitOps, DevSecOps, Zero Trust y Defense in Depth. Filosofía inalterable: **Java decide, la IA comunica**.*

---

## 1. Resumen Ejecutivo

La **FASE 30** define la **Arquitectura Oficial de Mantenibilidad Empresarial de KIN** para los próximos 10–15 años: cómo el sistema se **mantiene, evoluciona y modifica de forma segura y predecible** durante su vida útil — como **política, guideline, estrategia, estándar, governance, contrato documental, mecanismo documental o práctica**, jamás como implementación. Consolida exclusivamente F12–F29 (código, arquitectura, deuda, documentación, revisión, evolución), sin duplicar la Gobernanza (F25) ni la Calidad (F26).

---

## 2. Objetivo

Definir el **contrato documental de mantenibilidad de KIN**: qué hace al sistema fácil de entender, modificar, probar y extender durante 15 años — sin modificar el Core, el DAG, el AI Boundary ni ninguna decisión aprobada, preservando **"Java decide, la IA comunica"**.

---

## 3. Alcance

**Incluye**: principios de mantenibilidad, visión, arquitectura de mantenibilidad, modelo de mantenibilidad, código, arquitectura, dependencias, modularización, refactoring, deuda técnica, documentación, estándares de código, revisión, cambio, evolución, métricas, KPIs, checklists, roadmap y riesgos.

**Excluye** cualquier modificación de: Core, DAG, AI Boundary, BIL, Pipeline, Conversation, EngineRegistry, ProviderRouter, PromptAssembler, Enterprise, Flyway, ConnectorCatalog, EventCatalog, PolicyEngine, FeatureRegistry, DecisionRecord, BusinessDataset, SmartRouter, QueryService, AiContextBuilder, ResponseGuard, ResponseFallback, SecurityGateway, Vault, CostEngine, PlanAccessPort, APIs, contratos, eventos, ports, adapters, Aggregates, entidades, VOs, Domain/Application Services, casos de uso, Bounded Contexts, reglas de negocio o ADR.

---

## 4. Principios de Mantenibilidad Empresarial

| # | Principio |
|---|---|
| M1 | **Simplicidad estructural** — módulos con una responsabilidad (SRP) y sin "clase Dios" (F14 §19). |
| M2 | **Cohesión alta, acoplamiento bajo** — dependencias por el DAG (F12A §2). |
| M3 | **Cambio localizado** — una capacidad = un módulo/puerto (OCP). |
| M4 | **Deuda controlada** — deuda registrada y priorizada (F12C §7). |
| M5 | **Documentación viva** — ADR, blueprint y runbooks actualizados (F25 §9). |
| M6 | **Evolución aditiva** — evolución sin romper compatibilidad (MIP §24). |
| M7 | **Determinismo mantenible** — motores deterministas con golden master (F16). |
| M8 | **Java decide, IA comunica** — la IA no participa en la mantenibilidad. |

---

## 5. Enterprise Maintainability Vision

La mantenibilidad empresarial de KIN es la **capa documental que hace al sistema modificable y extensible durante 15 años**: cada módulo es comprensible, cada dependencia está gobernada, cada cambio es localizado y reversible, y cada decisión de evolución es documentada y auditada — sin tocar el Core ni la filosofía **"Java decide, la IA comunica"**.

---

## 6. Arquitectura General de Mantenibilidad

| Pilar | Contenido |
|---|---|
| **Código** | Estándares (F14 §5), naming, VOs, factories |
| **Arquitectura** | Hexagonal, DAG, ArchUnit (F14 §6) |
| **Dependencias** | Reglas de importación y DAG (F12A §2, F14 §6) |
| **Modularización** | Paquetes y BCs (F14 §4, F12A §5) |
| **Evolución** | Refactoring, deuda, deprecación, evolución aditiva |

---

## 7. Maintainability Model

Modelo documental: **legibilidad → localidad de cambio → extensibilidad → testabilidad → evolucionabilidad**. Cada servicio/módulo evalúa su mantenibilidad con métricas (cap. 19); la evolución es aditiva y gobernada.

---

## 8. Code Maintainability

- Estándares de código (F14 §5): records, factories `fromXxx/defaultXxx`, eventos en pasado, sin `if/switch` por país.
- Anti-patterns prohibidos: God Class, Controller inteligente, Service de 3000 líneas, anémia de dominio (F14 §19).
- Código con JavaDoc en clases públicas del dominio (gobernanza §4.3).

---

## 9. Architecture Maintainability

- Hexagonal: dominio puro, puertos, adapters (F14 §7).
- DAG sin ciclos; ArchUnit 0 violaciones (F16 §10).
- Heat map y scorecard trimestral (F12B §10).

---

## 10. Dependency Management

- Reglas de importación por módulo (F14 §6): dominio puro, adapters en `ai.*.adapter`, DAG unidireccional.
- `mvn dependency:analyze` limpio; dependencias versionadas (SemVer).
- Revisión de dependencias (OWASP) y sin librerías no aprobadas (F25 §7).

---

## 11. Modularization Strategy

- Módulos por BC (F12A §5): `kin.bil.*` (dominio), `ai.*.adapter` (infra), web del BC.
- Folder structure estándar (F14 §4): model, services, policies, specs, events, ports.
- Service Catalog con estado por módulo (F24 §11); BCs cohesivos (gobernanza §2.3).

---

## 12. Refactoring Governance

- Refactor = PR con tests y 2 reviewers (F12C §15).
- Refactor sin cambio de comportamiento; verificación con suite completa y ArchUnit.
- Refactoring de determinismo protegido por golden master (F16).

---

## 13. Technical Debt Governance

Consolidación de F12C §7 y F25 §15: clasificación **Critical/High/Medium/Low** × (Arquitectura/Seguridad/Performance/Documentación); tiempos máximos; deuda vencida **bloquea el Increment Gate** (MIP §8); revisión trimestral en el scorecard.

---

## 14. Documentation Governance

- ADR (append-only, estados), Blueprint/Execution/MIP vivos, runbooks actualizados (F25 §9).
- Documentación versionada; un documento aprobado se enmienda, no se reescribe.
- Traceability incremento → ADR → PR → tests → release (MIP §9).

---

## 15. Coding Standards Governance

- Estándares de código (F14 §5) y de seguridad (F18) con owner y revisión (F25 §12).
- Cambio de estándar requiere RFC y aprobación del Board.

---

## 16. Review Governance

- Code Review Constitution (F12C §5): qué revisar, qué bloquear, cuándo ADR/RFC, cuándo rechazar.
- 2 reviewers por PR (1 del BC, 1 de arquitectura).
- Revisión de arquitectura en cada PR (ArchUnit, DAG).

---

## 17. Change Maintainability

- Tipos de cambio con flujo de aprobación (F12C §15): cosmético, refactor, feature, arquitectura, infraestructura, seguridad, breaking.
- Todo cambio mantiene compatibilidad (SemVer + deprecación, F12B §4).

---

## 18. Evolution Governance

- Evolución aditiva: puertos nuevos, overloads, flags (MIP §24).
- Deprecación 2 fases MINOR; retiro solo MAJOR (gobernanza §9.3).
- Invariantes: DAG sin ciclos, dominio puro, Java decide.

---

## 19. Maintainability Metrics

| Métrica | Definición |
|---|---|
| Cohesión / Acoplamiento | por módulo (F12B §9) |
| Complejidad | módulos sin "clase Dios" |
| ArchUnit violations | 0 |
| Dependencias prohibidas | 0 |
| Cobertura de pruebas | dominio ≥ 90 % (F16 §5) |
| Deuda técnica | registrada y priorizada (F12C §7) |
| Duplicación | minimizada (F14 §19) |

---

## 20. Enterprise Maintainability KPIs

| KPI | Objetivo |
|---|---|
| ArchUnit / dependency violations | 0 |
| Deuda Critical/High vencida | 0 |
| Coverage dominio / engines | ≥ 90 % / ≥ 95 % |
| Lead time de cambio | ≤ 3 días (F17 §22) |
| Change Failure Rate | ≤ 15 % |
| ADR/RFC documentados | 100 % de decisiones |
| Scorecard de mantenibilidad | ≥ 8.0 trimestral |

> **Nota editorial (E1, conforme a la Pre-Auditoría FASE 30)**: el objetivo **"ADR/RFC documentados — 100 % de decisiones"** constituye un **objetivo documental de mantenibilidad consolidado por la propia FASE 30**. **No corresponde a un KPI numérico previamente definido en F12–F29**, **no reemplaza ningún KPI existente**, **no modifica ninguna decisión previamente aprobada** y únicamente formaliza documentalmente la práctica derivada de **F25 (Decisions as Documentation)**. Ningún otro KPI fue modificado y ninguna cifra fue alterada.

---

## 21. Enterprise Checklists

| Área | Checklist |
|---|---|
| **Código** | ¿estándares F14 §5? · ¿sin anti-patterns? |
| **Arquitectura** | ¿ArchUnit 0? · ¿DAG? · ¿dominio puro? |
| **Dependencias** | ¿`dependency:analyze` limpio? · ¿semver? |
| **Refactoring** | ¿tests? · ¿sin cambio de comportamiento? |
| **Deuda** | ¿registrada? · ¿sin Critical/High vencidos? |
| **Documentación** | ¿ADR/RFC? · ¿runbooks? |
| **Filosofía** | ¿Java decide, IA comunica verificado? |

---

## 22. Enterprise Maintainability Roadmap

```
F12 ── F12A ── MIP ── F12B ── F12C ── F13 ── F14 ── F15 ── F16 ── F17 ── F18 ── F19 ── F20 ── F21 ── F22 ── F23 ── F24 ── F25 ── F26 ── F27 ── F28 ── F29 ── F30
DAG/BIL · Execution · Plan · Constitución · Governance · Roadmap · Playbook · Producto · QA · DevOps · Seguridad · Enterprise · Escala · Operación · Datos · Integración · Plataforma · Gobernanza · Calidad · Resiliencia · Operaciones · Observabilidad · **Mantenibilidad**
```

---

## 23. Riesgos

| Riesgo | Naturaleza | Mitigación |
|---|---|---|
| Acoplamiento creciente | Arquitectura | ArchUnit + DAG + scorecard |
| Deuda no controlada | Técnica | Clasificación + gates (F12C §7) |
| Refactoring sin tests | Técnica | Gates + golden master |
| Documentación desactualizada | Documental | DoD + traceability (MIP) |
| Dependencias no aprobadas | Seguridad | Revisión OWASP + approval (F25) |

---

## 24. Compatibilidad

F12/F12A (DAG, BIL) · MIP (gates, DoR/DoD) · F12B (Constitución, KPIs) · F12C (gobernanza, review, deuda) · F13 (roadmap) · F14 (playbook, estándares, anti-patterns) · F15 (producto) · F16 (QA) · F17 (DevOps) · F18 (seguridad) · F19 (Enterprise) · F20 (escala) · F21 (excelencia) · F22 (datos) · F23 (integración) · F24 (plataforma) · F25 (gobernanza) · F26 (calidad) · F27 (resiliencia) · F28 (operaciones) · F29 (observabilidad).

---

## 25. Referencias Arquitectónicas

F12B (Constitución, scorecard) · MIP (gates, DoR/DoD) · F12C (gobernanza, §5 review, §7 deuda, §15 cambio) · F14 (playbook, §5 estándares, §19 anti-patterns) · F16 (QA, §10 architecture compliance) · F25 (gobernanza empresarial) · F26 (calidad).

---

## 26. Conclusiones

La **FASE 30** consolida la **Arquitectura Oficial de Mantenibilidad Empresarial de KIN** como una **capa documental**: modelo de mantenibilidad, código, arquitectura, dependencias, modularización, refactoring, deuda, documentación, estándares, revisión, cambio y evolución — sin modificar el Core, el DAG, el AI Boundary ni ninguna decisión aprobada, y preservando íntegramente la filosofía **"Java decide, la IA comunica"**. Queda lista para incorporarse como `kin-docs/KIN_ENTERPRISE_MAINTAINABILITY_ARCHITECTURE_SPECIFICATION.md` y para continuar el flujo oficial Pre-Auditoría → Formalización → Auditoría Final → Aprobación.

---

*Especificación oficial de Arquitectura de Mantenibilidad Empresarial de KIN (FASE 30). Documento formalizado conforme a la pre-auditoría (E1 aplicado). Compatible al 100 % con F12–F29.*
