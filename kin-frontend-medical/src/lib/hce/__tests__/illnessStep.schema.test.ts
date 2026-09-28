import { describe, it, expect } from 'vitest';
import {
  illnessStepSchema,
  systemsReviewSchema,
  validateOnsetDatetime,
} from '../schemas/illnessStep.schema';

describe('illnessStepSchema', () => {
  it('should validate a valid illness step', () => {
    const validData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      onsetDatetime: '2024-01-15T10:00:00Z',
      evolutionDescription: 'Dolor progresivo en abdomen bajo',
      aggravatingFactors: 'Movimiento, comida picante',
      alleviatingFactors: 'Reposo, antiácidos',
      associatedSymptoms: 'Náuseas, vómitos',
      severitySelfReported: 7,
      systemsReview: {
        general: { checked: true, notes: 'Paciente alerta' },
        cardiovascular: { checked: false },
        respiratory: { checked: false },
        digestive: { checked: true, notes: 'Dolor epigástrico' },
        genitourinary: { checked: false },
        musculoskeletal: { checked: false },
        neurological: { checked: false },
        endocrine: { checked: false },
        hematological: { checked: false },
        dermatological: { checked: false },
        psychiatric: { checked: false },
        ophthalmological: { checked: false },
        otorhinolaryngological: { checked: false },
        other: { checked: false },
      },
      previousEpisodes: 2,
      previousTreatments: 'Omeprazol 20mg',
      functionalImpact: 'Dificulta actividades laborales',
    };
    const result = illnessStepSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('should reject onsetDatetime in the future', () => {
    const futureDate = new Date(Date.now() + 86400000).toISOString();
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      onsetDatetime: futureDate,
    };
    const result = illnessStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });

  it('should reject severitySelfReported out of range (too high)', () => {
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      severitySelfReported: 15,
    };
    const result = illnessStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });

  it('should reject severitySelfReported out of range (too low)', () => {
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      severitySelfReported: 0,
    };
    const result = illnessStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });

  it('should reject evolutionDescription too long', () => {
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      evolutionDescription: 'a'.repeat(2001),
    };
    const result = illnessStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });

  it('should reject previousEpisodes negative', () => {
    const invalidData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      previousEpisodes: -1,
    };
    const result = illnessStepSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
  });

  it('should accept empty optional fields', () => {
    const minimalData = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
    };
    const result = illnessStepSchema.safeParse(minimalData);
    expect(result.success).toBe(true);
  });

  it('should accept partial systemsReview', () => {
    const data = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      systemsReview: {
        cardiovascular: { checked: true, notes: 'Taquicardia' },
      },
    };
    const result = illnessStepSchema.safeParse(data);
    expect(result.success).toBe(true);
  });
});

describe('systemsReviewSchema', () => {
  it('should validate complete systems review', () => {
    const validSystems = {
      general: { checked: true, notes: 'Bien' },
      cardiovascular: { checked: false },
      respiratory: { checked: false },
      digestive: { checked: false },
      genitourinary: { checked: false },
      musculoskeletal: { checked: false },
      neurological: { checked: false },
      endocrine: { checked: false },
      hematological: { checked: false },
      dermatological: { checked: false },
      psychiatric: { checked: false },
      ophthalmological: { checked: false },
      otorhinolaryngological: { checked: false },
      other: { checked: false },
    };
    const result = systemsReviewSchema.safeParse(validSystems);
    expect(result.success).toBe(true);
  });

  it('should default unchecked systems to false', () => {
    const partialSystems = {
      general: { checked: true, notes: 'Bien' },
    };
    const result = systemsReviewSchema.safeParse(partialSystems);
    expect(result.success).toBe(true);
    expect(result.data!.cardiovascular.checked).toBe(false);
  });

  it('should reject notes too long', () => {
    const invalidSystems = {
      general: { checked: true, notes: 'a'.repeat(1001) },
    };
    const result = systemsReviewSchema.safeParse(invalidSystems);
    expect(result.success).toBe(false);
  });
});

describe('validateOnsetDatetime', () => {
  it('should return valid for past datetime', () => {
    const pastDate = new Date(Date.now() - 86400000).toISOString();
    const result = validateOnsetDatetime(pastDate);
    expect(result.valid).toBe(true);
  });

  it('should return valid for current datetime', () => {
    const now = new Date().toISOString();
    const result = validateOnsetDatetime(now);
    expect(result.valid).toBe(true);
  });

  it('should return invalid for future datetime', () => {
    const futureDate = new Date(Date.now() + 86400000).toISOString();
    const result = validateOnsetDatetime(futureDate);
    expect(result.valid).toBe(false);
    expect(result.message).toBe('La fecha de inicio no puede ser futura');
  });

  it('should return valid for empty string', () => {
    const result = validateOnsetDatetime('');
    expect(result.valid).toBe(true);
  });
});