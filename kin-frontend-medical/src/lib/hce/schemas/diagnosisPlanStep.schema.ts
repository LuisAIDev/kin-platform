import { z } from 'zod';

export const diagnosisTypeSchema = z.enum([
  'PRINCIPAL',
  'SECUNDARIO',
  'COMORBILIDAD',
  'COMPLICACION',
  'INGRESO',
  'EGRESO',
]);

export const certaintySchema = z.enum([
  'CONFIRMED',
  'PRESUMPTIVE',
  'RULED_OUT',
  'WORKING',
]);

export const cie10CodeSchema = z.string().regex(
  /^[A-Z]\d{2}(\.\d{1,2})?$/,
  'Código CIE-10 inválido (ej: K59.0, J18.9)'
);

export const diagnosisSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  cie10Code: cie10CodeSchema,
  cie10Description: z.string().max(200).optional(),
  type: diagnosisTypeSchema.default('SECUNDARIO'),
  certainty: certaintySchema.default('WORKING'),
  isPrincipal: z.boolean().default(false),
});

export type DiagnosisFormData = z.infer<typeof diagnosisSchema>;

export const conductSchema = z.enum([
  'OBSERVATION',
  'OUTPATIENT_TREATMENT',
  'REFERRAL',
  'HOSPITALIZATION',
  'SURGERY',
  'PALLIATIVE',
  'REHABILITATION',
]);

export const prognosisSchema = z.enum([
  'EXCELLENT',
  'GOOD',
  'FAIR',
  'POOR',
  'GUARDED',
  'UNKNOWN',
]);

export const treatmentPlanSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  conduct: conductSchema.default('OUTPATIENT_TREATMENT'),
  therapeuticGoals: z.array(z.string().max(500)).max(10).default([]),
  followupPlan: z.string().max(2000).optional(),
  reevaluationCriteria: z.string().max(2000).optional(),
  prognosis: prognosisSchema.default('GOOD'),
});

export type TreatmentPlanFormData = z.infer<typeof treatmentPlanSchema>;

export const orderTypeSchema = z.enum([
  'MEDICATION',
  'PROCEDURE',
  'LAB_EXAM',
  'IMAGING',
  'DIET',
  'NURSING_CARE',
  'OTHER',
]);

export const orderPrioritySchema = z.enum([
  'STAT',
  'URGENT',
  'ROUTINE',
  'SCHEDULED',
]);

export const medicalOrderSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  orderType: orderTypeSchema,
  priority: orderPrioritySchema.default('ROUTINE'),
  drugName: z.string().max(200).optional(),
  dose: z.string().max(50).optional(),
  doseUnit: z.string().max(20).optional(),
  route: z.string().max(50).optional(),
  frequency: z.string().max(100).optional(),
  durationDays: z.number().int().min(1).max(365).optional(),
  cupsCode: z.string().max(20).optional(),
  instructions: z.string().max(1000).optional(),
}).refine((data) => {
  if (['PROCEDURE', 'LAB_EXAM', 'IMAGING'].includes(data.orderType) && !data.cupsCode) {
    return false;
  }
  return true;
}, {
  message: 'CUPS code is required for procedures, lab exams, and imaging',
  path: ['cupsCode'],
});

export type MedicalOrderFormData = z.infer<typeof medicalOrderSchema>;

export const diagnosisPlanSchema = z.object({
  encounterId: z.string().uuid(),
  diagnoses: z.array(diagnosisSchema).default([]),
  treatmentPlan: treatmentPlanSchema.optional(),
  medicalOrders: z.array(medicalOrderSchema).default([]),
});

export type DiagnosisPlanFormData = z.infer<typeof diagnosisPlanSchema>;

export const getDiagnosisTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    PRINCIPAL: 'Principal',
    SECUNDARIO: 'Secundario',
    COMORBILIDAD: 'Comorbilidad',
    COMPLICACION: 'Complicación',
    INGRESO: 'Al Ingreso',
    EGRESO: 'Al Egreso',
  };
  return labels[type] || type;
};

export const getCertaintyLabel = (certainty: string): string => {
  const labels: Record<string, string> = {
    CONFIRMED: 'Confirmado',
    PRESUMPTIVE: 'Presuntivo',
    RULED_OUT: 'Descartado',
    WORKING: 'En Estudio',
  };
  return labels[certainty] || certainty;
};

export const getConductLabel = (conduct: string): string => {
  const labels: Record<string, string> = {
    OBSERVATION: 'Observación',
    OUTPATIENT_TREATMENT: 'Tratamiento Ambulatorio',
    REFERRAL: 'Referencia',
    HOSPITALIZATION: 'Hospitalización',
    SURGERY: 'Cirugía',
    PALLIATIVE: 'Paliativo',
    REHABILITATION: 'Rehabilitación',
  };
  return labels[conduct] || conduct;
};

export const getPrognosisLabel = (prognosis: string): string => {
  const labels: Record<string, string> = {
    EXCELLENT: 'Excelente',
    GOOD: 'Bueno',
    FAIR: 'Regular',
    POOR: 'Malo',
    GUARDED: 'Reservado',
    UNKNOWN: 'Desconocido',
  };
  return labels[prognosis] || prognosis;
};

export const getOrderTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    MEDICATION: 'Medicamento',
    PROCEDURE: 'Procedimiento',
    LAB_EXAM: 'Examen de Laboratorio',
    IMAGING: 'Imagenología',
    DIET: 'Dieta',
    NURSING_CARE: 'Cuidados de Enfermería',
    OTHER: 'Otro',
  };
  return labels[type] || type;
};

export const getOrderPriorityLabel = (priority: string): string => {
  const labels: Record<string, string> = {
    STAT: 'STAT (Inmediato)',
    URGENT: 'Urgente',
    ROUTINE: 'Rutina',
    SCHEDULED: 'Programado',
  };
  return labels[priority] || priority;
};