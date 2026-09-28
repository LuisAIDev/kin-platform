import { describe, it, expect } from 'vitest';
import {
  physicalExamSchema,
  calculateBMI,
  validateVitals,
} from '../schemas/physicalExamStep.schema';

describe('physicalExamStepSchema', () => {
  it('should validate a valid physical exam', () => {
    const validExam = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      bpSystolic: 120,
      bpDiastolic: 80,
      heartRate: 72,
      respiratoryRate: 16,
      temperature: 36.5,
      spo2: 98,
      weightKg: 70,
      heightCm: 175,
      glasgowScore: 15,
      painScale: 3,
      generalAppearance: 'Paciente consciente, orientado',
      cardiovascular: 'Ritmo regular, sin soplos',
      respiratory: 'Mur vesicular conservado bilateral',
    };
    const result = physicalExamSchema.safeParse(validExam);
    expect(result.success).toBe(true);
  });

  it('should reject bpSystolic out of range (too low)', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      bpSystolic: 30,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject bpSystolic out of range (too high)', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      bpSystolic: 350,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject bpDiastolic out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      bpDiastolic: 20,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject heartRate out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      heartRate: 20,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject respiratoryRate out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      respiratoryRate: 4,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject temperature out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      temperature: 25,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject spo2 out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      spo2: 40,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject weightKg out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      weightKg: 0.05,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject heightCm out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      heightCm: 15,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject glasgowScore out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      glasgowScore: 2,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should reject painScale out of range', () => {
    const invalid = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      painScale: 15,
    };
    const result = physicalExamSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it('should accept empty optional fields', () => {
    const minimal = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
    };
    const result = physicalExamSchema.safeParse(minimal);
    expect(result.success).toBe(true);
  });

  it('should accept all system fields', () => {
    const fullExam = {
      encounterId: '123e4567-e89b-12d3-a456-426614174000',
      generalAppearance: 'Bien',
      headNeck: 'Normal',
      cardiovascular: 'Normal',
      respiratory: 'Normal',
      abdominal: 'Normal',
      neurological: 'Normal',
      musculoskeletal: 'Normal',
      skin: 'Normal',
      genitourinary: 'Normal',
      psychiatric: 'Normal',
    };
    const result = physicalExamSchema.safeParse(fullExam);
    expect(result.success).toBe(true);
  });
});

describe('calculateBMI', () => {
  it('should calculate BMI correctly', () => {
    expect(calculateBMI(70, 175)).toBe(22.9);
    expect(calculateBMI(80, 180)).toBe(24.7);
    expect(calculateBMI(60, 160)).toBe(23.4);
    expect(calculateBMI(100, 170)).toBe(34.6);
  });

  it('should return undefined for missing weight or height', () => {
    expect(calculateBMI(undefined, 175)).toBeUndefined();
    expect(calculateBMI(70, undefined)).toBeUndefined();
    expect(calculateBMI(0, 175)).toBeUndefined();
    expect(calculateBMI(70, 0)).toBeUndefined();
  });

  it('should handle edge cases', () => {
    expect(calculateBMI(0.1, 20)).toBe(2.5);
    expect(calculateBMI(500, 250)).toBe(80.0);
  });
});

describe('validateVitals', () => {
  it('should return valid for correct vitals', () => {
    const data = {
      bpSystolic: 120,
      bpDiastolic: 80,
      heartRate: 72,
      respiratoryRate: 16,
      temperature: 36.5,
      spo2: 98,
      weightKg: 70,
      heightCm: 175,
      glasgowScore: 15,
      painScale: 3,
    };
    const result = validateVitals(data);
    expect(result.valid).toBe(true);
    expect(Object.keys(result.errors).length).toBe(0);
  });

  it('should return errors for invalid bpSystolic', () => {
    const data = { bpSystolic: 30 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.bpSystolic).toBeDefined();
  });

  it('should return errors for invalid bpDiastolic', () => {
    const data = { bpDiastolic: 20 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.bpDiastolic).toBeDefined();
  });

  it('should return errors for invalid heartRate', () => {
    const data = { heartRate: 20 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.heartRate).toBeDefined();
  });

  it('should return errors for invalid respiratoryRate', () => {
    const data = { respiratoryRate: 4 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.respiratoryRate).toBeDefined();
  });

  it('should return errors for invalid temperature', () => {
    const data = { temperature: 25 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.temperature).toBeDefined();
  });

  it('should return errors for invalid spo2', () => {
    const data = { spo2: 40 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.spo2).toBeDefined();
  });

  it('should return errors for invalid weightKg', () => {
    const data = { weightKg: 0.05 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.weightKg).toBeDefined();
  });

  it('should return errors for invalid heightCm', () => {
    const data = { heightCm: 15 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.heightCm).toBeDefined();
  });

  it('should return errors for invalid glasgowScore', () => {
    const data = { glasgowScore: 2 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.glasgowScore).toBeDefined();
  });

  it('should return errors for invalid painScale', () => {
    const data = { painScale: 15 };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.painScale).toBeDefined();
  });

  it('should return multiple errors for multiple invalid fields', () => {
    const data = {
      bpSystolic: 30,
      heartRate: 20,
      temperature: 25,
      spo2: 40,
    };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(Object.keys(result.errors).length).toBe(4);
  });

  it('should not add error for valid fields when others are invalid', () => {
    const data = {
      bpSystolic: 30,
      bpDiastolic: 80,
      heartRate: 72,
    };
    const result = validateVitals(data);
    expect(result.valid).toBe(false);
    expect(result.errors.bpSystolic).toBeDefined();
    expect(result.errors.bpDiastolic).toBeUndefined();
    expect(result.errors.heartRate).toBeUndefined();
  });
});