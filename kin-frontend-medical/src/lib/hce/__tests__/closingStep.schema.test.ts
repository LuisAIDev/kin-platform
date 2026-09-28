import { describe, it, expect } from 'vitest';
import {
  closingStepSchema,
  validateClosingRequirements,
  getMissingRequirements,
} from '../schemas/closingStep.schema';

describe('closingStepSchema', () => {
  it('should validate a valid closing step', () => {
    const validData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      hasPrincipalDiagnosis: true,
      hasTreatmentPlan: true,
      isEncounterOpen: true,
      confirmation: true,
    };
    const result = closingStepSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('should reject missing confirmation', () => {
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      hasPrincipalDiagnosis: true,
      hasTreatmentPlan: true,
      isEncounterOpen: true,
      confirmation: false,
    };
    const result = closingStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    expect(result.error!.issues[0].message).toBe('Debe confirmar el cierre de la consulta');
  });

  it('should reject invalid encounterId', () => {
    const invalidData = {
      encounterId: 'invalid-uuid',
      hasPrincipalDiagnosis: true,
      hasTreatmentPlan: true,
      isEncounterOpen: true,
      confirmation: true,
    };
    const result = closingStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });
});

describe('validateClosingRequirements', () => {
  it('should return valid when all requirements met', () => {
    const result = validateClosingRequirements(true, true, true);
    expect(result.valid).toBe(true);
    expect(result.errors).toHaveLength(0);
  });

  it('should return invalid when missing principal diagnosis', () => {
    const result = validateClosingRequirements(false, true, true);
    expect(result.valid).toBe(false);
    expect(result.errors).toContain('Falta diagnóstico PRINCIPAL (obligatorio según Res 839/1995)');
  });

  it('should return invalid when missing treatment plan', () => {
    const result = validateClosingRequirements(true, false, true);
    expect(result.valid).toBe(false);
    expect(result.errors).toContain('Falta plan de manejo (obligatorio)');
  });

  it('should return invalid when encounter already closed', () => {
    const result = validateClosingRequirements(true, true, false);
    expect(result.valid).toBe(false);
    expect(result.errors).toContain('El encuentro ya está cerrado');
  });

  it('should return multiple errors for multiple missing requirements', () => {
    const result = validateClosingRequirements(false, false, false);
    expect(result.valid).toBe(false);
    expect(result.errors).toHaveLength(3);
    expect(result.errors).toContain('Falta diagnóstico PRINCIPAL (obligatorio según Res 839/1995)');
    expect(result.errors).toContain('Falta plan de manejo (obligatorio)');
    expect(result.errors).toContain('El encuentro ya está cerrado');
  });
});

describe('getMissingRequirements', () => {
  it('should return empty array when all present', () => {
    const result = getMissingRequirements(true, true, true);
    expect(result).toHaveLength(0);
  });

  it('should return missing items', () => {
    const result = getMissingRequirements(false, true, true);
    expect(result).toContain('diagnóstico PRINCIPAL');
  });

  it('should return multiple missing items', () => {
    const result = getMissingRequirements(false, false, true);
    expect(result).toContain('diagnóstico PRINCIPAL');
    expect(result).toContain('plan de manejo');
  });

  it('should return all three when all missing', () => {
    const result = getMissingRequirements(false, false, false);
    expect(result).toHaveLength(3);
    expect(result).toContain('diagnóstico PRINCIPAL');
    expect(result).toContain('plan de manejo');
    expect(result).toContain('encuentro abierto');
  });
});