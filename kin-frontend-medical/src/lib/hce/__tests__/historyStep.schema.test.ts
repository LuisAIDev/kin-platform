import { describe, it, expect } from 'vitest';
import {
  allergySchema,
  surgerySchema,
  medicationSchema,
  vaccineSchema,
  familyHistorySchema,
  toxicHabitSchema,
  gynecoObstetricSchema,
  historyTypeSchema,
  getHistoryTypeLabel,
  getHistoryTypeIcon,
  createHistoryItemSchema,
} from '../schemas/historyStep.schema';

describe('historyStepSchema', () => {
  describe('allergySchema', () => {
    it('should validate a valid allergy', () => {
      const validAllergy = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        allergen: 'Penicilina',
        reaction: 'Erupción cutánea',
        severity: 'MODERATE',
        onsetDate: '2020-01-15',
        status: 'ACTIVE',
      };
      const result = allergySchema.safeParse(validAllergy);
      expect(result.success).toBe(true);
    });

    it('should default severity to MILD', () => {
      const allergy = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        allergen: 'Polen',
      };
      const result = allergySchema.safeParse(allergy);
      expect(result.success).toBe(true);
      expect(result.data!.severity).toBe('MILD');
    });

    it('should reject invalid severity', () => {
      const allergy = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        allergen: 'Penicilina',
        severity: 'INVALID',
      };
      const result = allergySchema.safeParse(allergy);
      expect(result.success).toBe(false);
    });

    it('should reject empty allergen', () => {
      const allergy = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        allergen: '',
      };
      const result = allergySchema.safeParse(allergy);
      expect(result.success).toBe(false);
    });
  });

  describe('surgerySchema', () => {
    it('should validate a valid surgery', () => {
      const validSurgery = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        procedure: 'Apendicectomía',
        date: '2020-05-15',
        hospital: 'Hospital Central',
        complications: 'Ninguna',
      };
      const result = surgerySchema.safeParse(validSurgery);
      expect(result.success).toBe(true);
    });

    it('should reject missing procedure', () => {
      const surgery = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        date: '2020-05-15',
      };
      const result = surgerySchema.safeParse(surgery);
      expect(result.success).toBe(false);
    });

    it('should reject invalid date format', () => {
      const surgery = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        procedure: 'Apendicectomía',
        date: 'invalid-date',
      };
      const result = surgerySchema.safeParse(surgery);
      expect(result.success).toBe(false);
    });
  });

  describe('medicationSchema', () => {
    it('should validate a valid medication', () => {
      const validMed = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        name: 'Losartán',
        dosage: '50mg',
        frequency: '1 vez al día',
        route: 'Oral',
        startDate: '2023-01-15',
        isActive: true,
      };
      const result = medicationSchema.safeParse(validMed);
      expect(result.success).toBe(true);
    });

    it('should default isActive to true', () => {
      const med = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        name: 'Aspirina',
      };
      const result = medicationSchema.safeParse(med);
      expect(result.success).toBe(true);
      expect(result.data!.isActive).toBe(true);
    });
  });

  describe('vaccineSchema', () => {
    it('should validate a valid vaccine', () => {
      const validVaccine = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        name: 'COVID-19',
        date: '2021-03-15',
        dose: '1ra dosis',
        batch: 'AB12345',
      };
      const result = vaccineSchema.safeParse(validVaccine);
      expect(result.success).toBe(true);
    });

    it('should reject missing name', () => {
      const vaccine = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        date: '2021-03-15',
      };
      const result = vaccineSchema.safeParse(vaccine);
      expect(result.success).toBe(false);
    });
  });

  describe('familyHistorySchema', () => {
    it('should validate a valid family history', () => {
      const validFamily = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        relationship: 'FATHER',
        condition: 'Diabetes tipo 2',
        ageOfOnset: 55,
        isDeceased: true,
        ageAtDeath: 72,
      };
      const result = familyHistorySchema.safeParse(validFamily);
      expect(result.success).toBe(true);
    });

    it('should reject invalid relationship', () => {
      const family = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        relationship: 'PRIMO',
        condition: 'Cáncer',
      };
      const result = familyHistorySchema.safeParse(family);
      expect(result.success).toBe(false);
    });

    it('should reject ageOfOnset out of range', () => {
      const family = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        relationship: 'FATHER',
        condition: 'Diabetes',
        ageOfOnset: 150,
      };
      const result = familyHistorySchema.safeParse(family);
      expect(result.success).toBe(false);
    });
  });

  describe('toxicHabitSchema', () => {
    it('should validate a valid toxic habit', () => {
      const validToxic = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        substance: 'TOBACCO',
        frequency: 'Diario',
        quantity: '10 cigarrillos',
        startDate: '2010-01-01',
        isActive: false,
      };
      const result = toxicHabitSchema.safeParse(validToxic);
      expect(result.success).toBe(true);
    });

    it('should reject invalid substance', () => {
      const toxic = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        substance: 'CAFEINA',
      };
      const result = toxicHabitSchema.safeParse(toxic);
      expect(result.success).toBe(false);
    });

    it('should default isActive to true', () => {
      const toxic = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        substance: 'ALCOHOL',
      };
      const result = toxicHabitSchema.safeParse(toxic);
      expect(result.success).toBe(true);
      expect(result.data!.isActive).toBe(true);
    });
  });

  describe('gynecoObstetricSchema', () => {
    it('should validate a valid gyneco-obstetric record', () => {
      const validGyneco = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        gravida: 3,
        para: 2,
        abortions: 0,
        cesareans: 1,
        livingChildren: 2,
        lastMenstrualPeriod: '2024-01-15',
        menarcheAge: 12,
      };
      const result = gynecoObstetricSchema.safeParse(validGyneco);
      expect(result.success).toBe(true);
    });

    it('should default numeric fields to 0', () => {
      const gyneco = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
      };
      const result = gynecoObstetricSchema.safeParse(gyneco);
      expect(result.success).toBe(true);
      expect(result.data!.gravida).toBe(0);
      expect(result.data!.para).toBe(0);
    });

    it('should reject menarcheAge out of range', () => {
      const gyneco = {
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        menarcheAge: 5,
      };
      const result = gynecoObstetricSchema.safeParse(gyneco);
      expect(result.success).toBe(false);
    });
  });

  describe('historyTypeSchema', () => {
    it('should validate all valid history types', () => {
      const validTypes = [
        'ALLERGY', 'SURGERY', 'MEDICATION', 'VACCINE',
        'FAMILY_HISTORY', 'TOXICOLOGICAL', 'GYNECO_OBSTETRIC',
      ];
      for (const type of validTypes) {
        const result = historyTypeSchema.safeParse(type);
        expect(result.success).toBe(true);
      }
    });

    it('should reject invalid history type', () => {
      const result = historyTypeSchema.safeParse('INVALID_TYPE');
      expect(result.success).toBe(false);
    });
  });

  describe('getHistoryTypeLabel', () => {
    it('should return correct labels for all types', () => {
      expect(getHistoryTypeLabel('ALLERGY')).toBe('Alergias');
      expect(getHistoryTypeLabel('SURGERY')).toBe('Cirugías');
      expect(getHistoryTypeLabel('MEDICATION')).toBe('Medicamentos');
      expect(getHistoryTypeLabel('VACCINE')).toBe('Vacunas');
      expect(getHistoryTypeLabel('FAMILY_HISTORY')).toBe('Familiares');
      expect(getHistoryTypeLabel('TOXICOLOGICAL')).toBe('Tóxicos');
      expect(getHistoryTypeLabel('GYNECO_OBSTETRIC')).toBe('Gineco-Obstétricos');
    });
  });

  describe('getHistoryTypeIcon', () => {
    it('should return correct icons for all types', () => {
      expect(getHistoryTypeIcon('ALLERGY')).toBe('AlertTriangle');
      expect(getHistoryTypeIcon('SURGERY')).toBe('Scissors');
      expect(getHistoryTypeIcon('MEDICATION')).toBe('Pill');
      expect(getHistoryTypeIcon('VACCINE')).toBe('Syringe');
      expect(getHistoryTypeIcon('FAMILY_HISTORY')).toBe('Users');
      expect(getHistoryTypeIcon('TOXICOLOGICAL')).toBe('Skull');
      expect(getHistoryTypeIcon('GYNECO_OBSTETRIC')).toBe('Baby');
    });
  });

  describe('createHistoryItemSchema', () => {
    it('should return correct schema for each type', () => {
      const allergySchemaResult = createHistoryItemSchema('ALLERGY');
      const result = allergySchemaResult.safeParse({
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        allergen: 'Penicilina',
      });
      expect(result.success).toBe(true);

      const surgerySchemaResult = createHistoryItemSchema('SURGERY');
      const surgeryResult = surgerySchemaResult.safeParse({
        patientId: '123e4567-e89b-12d3-a456-426614174000',
        procedure: 'Apendicectomía',
        date: '2020-01-01',
      });
      expect(surgeryResult.success).toBe(true);
    });
  });
});