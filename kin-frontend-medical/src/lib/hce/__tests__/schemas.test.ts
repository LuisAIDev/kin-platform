import { describe, it, expect } from 'vitest';
import {
  encounterSchema,
  encounterTypeSchema,
  patientIdentificationSchema,
  diagnosisSchema,
  treatmentPlanSchema,
  medicalOrderSchema,
} from '../schemas/encounter.schema';
import { anamnesisSchema } from '../schemas/anamnesis.schema';
import { physicalExamSchema, calculateBMI } from '../schemas/physicalExam.schema';
import { historySchema } from '../schemas/history.schema';

describe('HCE Schemas', () => {
  describe('encounterSchema', () => {
    it('should validate a valid encounter', () => {
      const validEncounter = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        encounterType: 'OUTPATIENT',
        chiefComplaint: 'Dolor abdominal',
      };
      const result = encounterSchema.safeParse(validEncounter);
      expect(result.success).toBe(true);
    });

    it('should reject invalid encounter type', () => {
      const invalidEncounter = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        encounterType: 'INVALID_TYPE',
        chiefComplaint: 'Dolor abdominal',
      };
      const result = encounterSchema.safeParse(invalidEncounter);
      expect(result.success).toBe(false);
    });

    it('should reject empty chief complaint', () => {
      const invalidEncounter = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        encounterType: 'OUTPATIENT',
        chiefComplaint: '',
      };
      const result = encounterSchema.safeParse(invalidEncounter);
      expect(result.success).toBe(false);
    });

    it('should reject chief complaint longer than 500 chars', () => {
      const invalidEncounter = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        encounterType: 'OUTPATIENT',
        chiefComplaint: 'a'.repeat(501),
      };
      const result = encounterSchema.safeParse(invalidEncounter);
      expect(result.success).toBe(false);
    });
  });

  describe('patientIdentificationSchema', () => {
    it('should validate a valid patient identification', () => {
      const validIdentification = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
        bloodType: 'A+',
        rhFactor: 'POSITIVE',
        eps: 'Sanitas',
        regime: 'CONTRIBUTIVO',
        phone: '3001234567',
        email: 'juan@test.com',
      };
      const result = patientIdentificationSchema.safeParse(validIdentification);
      expect(result.success).toBe(true);
    });

    it('should reject invalid document type', () => {
      const invalidIdentification = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        documentType: 'INVALID',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
      };
      const result = patientIdentificationSchema.safeParse(invalidIdentification);
      expect(result.success).toBe(false);
    });

    it('should reject invalid email format', () => {
      const invalidIdentification = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
        email: 'invalid-email',
      };
      const result = patientIdentificationSchema.safeParse(invalidIdentification);
      expect(result.success).toBe(false);
    });

    it('should accept empty email', () => {
      const validIdentification = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
        email: '',
      };
      const result = patientIdentificationSchema.safeParse(validIdentification);
      expect(result.success).toBe(true);
    });
  });

  describe('diagnosisSchema', () => {
    it('should validate a valid diagnosis', () => {
      const validDiagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'K59.0',
        cie10Description: 'Constipación',
        type: 'PRINCIPAL',
        certainty: 'CONFIRMED',
      };
      const result = diagnosisSchema.safeParse(validDiagnosis);
      expect(result.success).toBe(true);
    });

    it('should reject invalid CIE-10 code format', () => {
      const invalidDiagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'INVALID',
        type: 'PRINCIPAL',
        certainty: 'CONFIRMED',
      };
      const result = diagnosisSchema.safeParse(invalidDiagnosis);
      expect(result.success).toBe(false);
    });

    it('should accept valid CIE-10 codes', () => {
      const validCodes = ['A00', 'A00.0', 'K59.0', 'Z00.00', 'J18.9'];
      for (const code of validCodes) {
        const diagnosis = {
          encounterId: '123e4567-e89b-12d3-a456-426614174000',
          cie10Code: code,
          type: 'PRINCIPAL',
          certainty: 'CONFIRMED',
        };
        const result = diagnosisSchema.safeParse(diagnosis);
        expect(result.success).toBe(true);
      }
    });
  });

  describe('treatmentPlanSchema', () => {
    it('should validate a valid treatment plan', () => {
      const validPlan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        conduct: 'Tratamiento conservador',
        objectives: 'Mejorar síntomas',
        followUpInstructions: 'Control en 7 días',
        prognosis: 'Favorable',
        referredTo: '',
        referralReason: '',
      };
      const result = treatmentPlanSchema.safeParse(validPlan);
      expect(result.success).toBe(true);
    });
  });

  describe('medicalOrderSchema', () => {
    it('should validate a valid medical order', () => {
      const validOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
        description: 'Paracetamol 500mg c/8h x 3 días',
        frequency: 'Cada 8 horas',
        duration: '3 días',
        priority: 'ROUTINE',
      };
      const result = medicalOrderSchema.safeParse(validOrder);
      expect(result.success).toBe(true);
    });

    it('should reject empty description', () => {
      const invalidOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
        description: '',
      };
      const result = medicalOrderSchema.safeParse(invalidOrder);
      expect(result.success).toBe(false);
    });
  });

  describe('anamnesisSchema', () => {
    it('should validate a valid anamnesis', () => {
      const validAnamnesis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        onsetDatetime: '2024-01-15T10:00:00Z',
        evolutionDescription: 'Dolor progresivo',
        aggravatingFactors: 'Movimiento',
        alleviatingFactors: 'Reposo',
        associatedSymptoms: 'Náuseas',
        severitySelfReported: 7,
        systemsReview: {
          cardiovascular: 'Normal',
          respiratory: 'Normal',
        },
        previousEpisodes: 'Ninguno',
        previousTreatments: 'Ninguno',
        functionalImpact: 'Leve',
      };
      const result = anamnesisSchema.safeParse(validAnamnesis);
      expect(result.success).toBe(true);
    });

    it('should reject severity out of range', () => {
      const invalidAnamnesis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        severitySelfReported: 15,
      };
      const result = anamnesisSchema.safeParse(invalidAnamnesis);
      expect(result.success).toBe(false);
    });
  });

  describe('physicalExamSchema', () => {
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
        cardiovascular: 'Normal',
        respiratory: 'Normal',
      };
      const result = physicalExamSchema.safeParse(validExam);
      expect(result.success).toBe(true);
    });

    it('should reject invalid blood pressure values (too low)', () => {
      const invalidExam = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        bpSystolic: 30,
        bpDiastolic: 20,
      };
      const result = physicalExamSchema.safeParse(invalidExam);
      expect(result.success).toBe(false);
    });

    it('should reject invalid blood pressure values (too high)', () => {
      const invalidExam = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        bpSystolic: 350,
        bpDiastolic: 250,
      };
      const result = physicalExamSchema.safeParse(invalidExam);
      expect(result.success).toBe(false);
    });

    it('should reject invalid heart rate', () => {
      const invalidExam = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        heartRate: 20,
      };
      const result = physicalExamSchema.safeParse(invalidExam);
      expect(result.success).toBe(false);
    });

    it('should reject invalid temperature', () => {
      const invalidExam = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        temperature: 25,
      };
      const result = physicalExamSchema.safeParse(invalidExam);
      expect(result.success).toBe(false);
    });

    it('should reject invalid SpO2', () => {
      const invalidExam = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        spo2: 40,
      };
      const result = physicalExamSchema.safeParse(invalidExam);
      expect(result.success).toBe(false);
    });
  });

  describe('calculateBMI', () => {
    it('should calculate BMI correctly', () => {
      expect(calculateBMI(70, 175)).toBe(22.9);
      expect(calculateBMI(80, 180)).toBe(24.7);
      expect(calculateBMI(60, 160)).toBe(23.4);
    });

    it('should return undefined for missing weight or height', () => {
      expect(calculateBMI(undefined, 175)).toBeUndefined();
      expect(calculateBMI(70, undefined)).toBeUndefined();
      expect(calculateBMI(0, 175)).toBeUndefined();
      expect(calculateBMI(70, 0)).toBeUndefined();
    });
  });

  describe('historySchema', () => {
    it('should validate a valid history', () => {
      const validHistory = {
        allergies: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            allergen: 'Penicilina',
            reaction: 'Erupción cutánea',
            severity: 'MODERADA',
          },
        ],
        surgeries: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            procedure: 'Apendicectomía',
            date: '2020-05-15',
          },
        ],
        medications: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            name: 'Losartán',
            dosage: '50mg',
            frequency: '1 vez al día',
            isActive: true,
          },
        ],
        vaccines: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            vaccine: 'COVID-19',
            date: '2021-03-15',
          },
        ],
        familyHistory: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            relative: 'PADRE',
            condition: 'Diabetes tipo 2',
          },
        ],
        toxicHabits: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            substance: 'TABACO',
            isActive: false,
          },
        ],
        gynecoObstetric: {
          patientId: '123e4567-e89b-12d3-a456-426614174000',
          gravida: 2,
          para: 1,
        },
      };
      const result = historySchema.safeParse(validHistory);
      expect(result.success).toBe(true);
    });

    it('should reject invalid allergy severity', () => {
      const invalidHistory = {
        allergies: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            allergen: 'Penicilina',
            severity: 'INVALID',
          },
        ],
      };
      const result = historySchema.safeParse(invalidHistory);
      expect(result.success).toBe(false);
    });

    it('should reject invalid relative in family history', () => {
      const invalidHistory = {
        familyHistory: [
          {
            patientId: '123e4567-e89b-12d3-a456-426614174000',
            relative: 'PRIMO',
            condition: 'Cáncer',
          },
        ],
      };
      const result = historySchema.safeParse(invalidHistory);
      expect(result.success).toBe(false);
    });
  });
});