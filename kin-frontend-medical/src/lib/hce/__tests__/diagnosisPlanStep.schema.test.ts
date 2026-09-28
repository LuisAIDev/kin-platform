import { describe, it, expect } from 'vitest';
import {
  diagnosisSchema,
  treatmentPlanSchema,
  medicalOrderSchema,
  diagnosisPlanSchema,
  cie10CodeSchema,
  getDiagnosisTypeLabel,
  getCertaintyLabel,
  getConductLabel,
  getPrognosisLabel,
  getOrderTypeLabel,
  getOrderPriorityLabel,
} from '../schemas/diagnosisPlanStep.schema';

describe('diagnosisPlanStepSchema', () => {
  describe('cie10CodeSchema', () => {
    it('should accept valid CIE-10 codes', () => {
      const validCodes = ['A00', 'A00.0', 'K59.0', 'J18.9', 'Z00.00', 'E11.9', 'I10'];
      for (const code of validCodes) {
        const result = cie10CodeSchema.safeParse(code);
        expect(result.success).toBe(true);
      }
    });

    it('should reject invalid CIE-10 codes', () => {
      const invalidCodes = ['invalid', '123', 'a00', 'K59.0.1', 'K59.', 'K5'];
      for (const code of invalidCodes) {
        const result = cie10CodeSchema.safeParse(code);
        expect(result.success).toBe(false);
      }
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
        isPrincipal: true,
      };
      const result = diagnosisSchema.safeParse(validDiagnosis);
      expect(result.success).toBe(true);
    });

    it('should default type to SECUNDARIO', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'J18.9',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(true);
      expect(result.data!.type).toBe('SECUNDARIO');
    });

    it('should default certainty to WORKING', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'J18.9',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(true);
      expect(result.data!.certainty).toBe('WORKING');
    });

    it('should default isPrincipal to false', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'J18.9',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(true);
      expect(result.data!.isPrincipal).toBe(false);
    });

    it('should reject invalid CIE-10 code', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'INVALID',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(false);
    });

    it('should reject invalid type', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'K59.0',
        type: 'INVALID_TYPE',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(false);
    });

    it('should reject invalid certainty', () => {
      const diagnosis = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        cie10Code: 'K59.0',
        certainty: 'INVALID',
      };
      const result = diagnosisSchema.safeParse(diagnosis);
      expect(result.success).toBe(false);
    });
  });

  describe('treatmentPlanSchema', () => {
    it('should validate a valid treatment plan', () => {
      const validPlan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        conduct: 'OUTPATIENT_TREATMENT',
        therapeuticGoals: ['Controlar síntomas', 'Prevenir complicaciones'],
        followupPlan: 'Control en 7 días',
        reevaluationCriteria: 'Persistencia de síntomas',
        prognosis: 'GOOD',
      };
      const result = treatmentPlanSchema.safeParse(validPlan);
      expect(result.success).toBe(true);
    });

    it('should default conduct to OUTPATIENT_TREATMENT', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(true);
      expect(result.data!.conduct).toBe('OUTPATIENT_TREATMENT');
    });

    it('should default prognosis to GOOD', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(true);
      expect(result.data!.prognosis).toBe('GOOD');
    });

    it('should default therapeuticGoals to empty array', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(true);
      expect(result.data!.therapeuticGoals).toEqual([]);
    });

    it('should reject therapeuticGoals > 10', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        therapeuticGoals: Array(11).fill('Goal'),
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(false);
    });

    it('should reject invalid conduct', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        conduct: 'INVALID',
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(false);
    });

    it('should reject invalid prognosis', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        prognosis: 'INVALID',
      };
      const result = treatmentPlanSchema.safeParse(plan);
      expect(result.success).toBe(false);
    });
  });

  describe('medicalOrderSchema', () => {
    it('should validate a valid medication order', () => {
      const validOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
        drugName: 'Paracetamol',
        dose: '500',
        doseUnit: 'mg',
        route: 'Oral',
        frequency: 'Cada 8 horas',
        durationDays: 3,
        priority: 'ROUTINE',
      };
      const result = medicalOrderSchema.safeParse(validOrder);
      expect(result.success).toBe(true);
    });

    it('should validate a valid procedure order with CUPS', () => {
      const validOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'PROCEDURE',
        cupsCode: '890201',
        instructions: 'Realizar en ayunas',
      };
      const result = medicalOrderSchema.safeParse(validOrder);
      expect(result.success).toBe(true);
    });

    it('should validate a valid lab exam order with CUPS', () => {
      const validOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'LAB_EXAM',
        cupsCode: '900301',
      };
      const result = medicalOrderSchema.safeParse(validOrder);
      expect(result.success).toBe(true);
    });

    it('should validate a valid imaging order with CUPS', () => {
      const validOrder = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'IMAGING',
        cupsCode: '870101',
      };
      const result = medicalOrderSchema.safeParse(validOrder);
      expect(result.success).toBe(true);
    });

    it('should reject PROCEDURE without CUPS code', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'PROCEDURE',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(false);
    });

    it('should reject LAB_EXAM without CUPS code', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'LAB_EXAM',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(false);
    });

    it('should reject IMAGING without CUPS code', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'IMAGING',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(false);
    });

    it('should accept MEDICATION without CUPS code', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
        drugName: 'Paracetamol',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(true);
    });

    it('should default priority to ROUTINE', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(true);
      expect(result.data!.priority).toBe('ROUTINE');
    });

    it('should reject invalid orderType', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'INVALID',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(false);
    });

    it('should reject invalid priority', () => {
      const order = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        orderType: 'MEDICATION',
        priority: 'INVALID',
      };
      const result = medicalOrderSchema.safeParse(order);
      expect(result.success).toBe(false);
    });
  });

  describe('diagnosisPlanSchema', () => {
    it('should validate a complete diagnosis plan', () => {
      const validPlan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
        diagnoses: [
          {
            encounterId: '123e4567-e89b-12d3-a456-426614174000',
            cie10Code: 'K59.0',
            type: 'PRINCIPAL',
            certainty: 'CONFIRMED',
            isPrincipal: true,
          },
          {
            encounterId: '123e4567-e89b-12d3-a456-426614174000',
            cie10Code: 'K59.1',
            type: 'SECUNDARIO',
            certainty: 'PRESUMPTIVE',
            isPrincipal: false,
          },
        ],
        treatmentPlan: {
          encounterId: '123e4567-e89b-12d3-a456-426614174000',
          conduct: 'OUTPATIENT_TREATMENT',
          therapeuticGoals: ['Aliviar estreñimiento'],
          followupPlan: 'Control en 1 semana',
          prognosis: 'GOOD',
        },
        medicalOrders: [
          {
            encounterId: '123e4567-e89b-12d3-a456-426614174000',
            orderType: 'MEDICATION',
            drugName: 'Lactulosa',
            dose: '15',
            doseUnit: 'ml',
            frequency: 'Cada 12 horas',
            durationDays: 7,
          },
        ],
      };
      const result = diagnosisPlanSchema.safeParse(validPlan);
      expect(result.success).toBe(true);
    });

    it('should default empty arrays', () => {
      const plan = {
        encounterId: '123e4567-e89b-12d3-a456-426614174000',
      };
      const result = diagnosisPlanSchema.safeParse(plan);
      expect(result.success).toBe(true);
      expect(result.data!.diagnoses).toEqual([]);
      expect(result.data!.medicalOrders).toEqual([]);
    });
  });

  describe('getLabel functions', () => {
    it('should return correct diagnosis type labels', () => {
      expect(getDiagnosisTypeLabel('PRINCIPAL')).toBe('Principal');
      expect(getDiagnosisTypeLabel('SECUNDARIO')).toBe('Secundario');
      expect(getDiagnosisTypeLabel('COMORBILIDAD')).toBe('Comorbilidad');
      expect(getDiagnosisTypeLabel('COMPLICACION')).toBe('Complicación');
      expect(getDiagnosisTypeLabel('INGRESO')).toBe('Al Ingreso');
      expect(getDiagnosisTypeLabel('EGRESO')).toBe('Al Egreso');
    });

    it('should return correct certainty labels', () => {
      expect(getCertaintyLabel('CONFIRMED')).toBe('Confirmado');
      expect(getCertaintyLabel('PRESUMPTIVE')).toBe('Presuntivo');
      expect(getCertaintyLabel('RULED_OUT')).toBe('Descartado');
      expect(getCertaintyLabel('WORKING')).toBe('En Estudio');
    });

    it('should return correct conduct labels', () => {
      expect(getConductLabel('OBSERVATION')).toBe('Observación');
      expect(getConductLabel('OUTPATIENT_TREATMENT')).toBe('Tratamiento Ambulatorio');
      expect(getConductLabel('REFERRAL')).toBe('Referencia');
      expect(getConductLabel('HOSPITALIZATION')).toBe('Hospitalización');
      expect(getConductLabel('SURGERY')).toBe('Cirugía');
      expect(getConductLabel('PALLIATIVE')).toBe('Paliativo');
      expect(getConductLabel('REHABILITATION')).toBe('Rehabilitación');
    });

    it('should return correct prognosis labels', () => {
      expect(getPrognosisLabel('EXCELLENT')).toBe('Excelente');
      expect(getPrognosisLabel('GOOD')).toBe('Bueno');
      expect(getPrognosisLabel('FAIR')).toBe('Regular');
      expect(getPrognosisLabel('POOR')).toBe('Malo');
      expect(getPrognosisLabel('GUARDED')).toBe('Reservado');
      expect(getPrognosisLabel('UNKNOWN')).toBe('Desconocido');
    });

    it('should return correct order type labels', () => {
      expect(getOrderTypeLabel('MEDICATION')).toBe('Medicamento');
      expect(getOrderTypeLabel('PROCEDURE')).toBe('Procedimiento');
      expect(getOrderTypeLabel('LAB_EXAM')).toBe('Examen de Laboratorio');
      expect(getOrderTypeLabel('IMAGING')).toBe('Imagenología');
      expect(getOrderTypeLabel('DIET')).toBe('Dieta');
      expect(getOrderTypeLabel('NURSING_CARE')).toBe('Cuidados de Enfermería');
      expect(getOrderTypeLabel('OTHER')).toBe('Otro');
    });

    it('should return correct order priority labels', () => {
      expect(getOrderPriorityLabel('STAT')).toBe('STAT (Inmediato)');
      expect(getOrderPriorityLabel('URGENT')).toBe('Urgente');
      expect(getOrderPriorityLabel('ROUTINE')).toBe('Rutina');
      expect(getOrderPriorityLabel('SCHEDULED')).toBe('Programado');
    });
  });
});