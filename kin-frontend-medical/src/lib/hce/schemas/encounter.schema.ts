import { z } from 'zod';

export const encounterTypeSchema = z.enum([
  'OUTPATIENT',
  'INPATIENT',
  'EMERGENCY',
  'TELEMEDICINE',
  'HOME_CARE',
  'DAY_SURGERY',
]);

export type EncounterType = z.infer<typeof encounterTypeSchema>;

export const encounterSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  encounterType: encounterTypeSchema,
  chiefComplaint: z.string().min(1, 'El motivo de consulta es obligatorio').max(500),
  status: z.enum(['OPEN', 'CLOSED', 'CANCELLED']).optional(),
  startedAt: z.string().datetime().optional(),
  closedAt: z.string().datetime().nullable().optional(),
});

export type EncounterFormData = z.infer<typeof encounterSchema>;

export const encounterUpdateSchema = encounterSchema.partial();
export type EncounterUpdateData = z.infer<typeof encounterUpdateSchema>;

export const patientIdentificationSchema = z.object({
  patientId: z.string().uuid(),
  // Alineado con PatientIdentification.DocumentType (PP, no PA).
  documentType: z.enum(['CC', 'TI', 'CE', 'PP', 'RC']),
  documentNumber: z.string().min(1, 'El número de documento es obligatorio').max(20),
  firstName: z.string().min(1, 'El nombre es obligatorio').max(100),
  lastName: z.string().min(1, 'El apellido es obligatorio').max(100),
  birthDate: z.string().date('Fecha de nacimiento inválida'),
  // Alineado con PatientIdentification.RhFactor (grupo + Rh combinado). El backend no tiene sex/bloodType.
  rhFactor: z.enum(['A_POS', 'A_NEG', 'B_POS', 'B_NEG', 'AB_POS', 'AB_NEG', 'O_POS', 'O_NEG']).optional(),
  eps: z.string().max(100).optional(),
  // Alineado con PatientIdentification.Regimen.
  regime: z.enum(['CONTRIBUTIVO', 'SUBSIDIADO', 'ESPECIAL', 'EXCEPCION']).optional(),
  phone: z.string().max(20).optional(),
  email: z.string().email('Email inválido').max(100).optional().or(z.literal('')),
  address: z.string().max(200).optional(),
  city: z.string().max(100).optional(),
  emergencyContactName: z.string().max(100).optional(),
  emergencyContactPhone: z.string().max(20).optional(),
  emergencyContactRelation: z.string().max(50).optional(),
  guardianDocumentType: z.enum(['CC', 'TI', 'CE', 'PP', 'RC']).optional(),
  guardianDocumentNumber: z.string().max(20).optional(),
  guardianName: z.string().max(100).optional(),
});

export type PatientIdentificationFormData = z.infer<typeof patientIdentificationSchema>;

export const diagnosisTypeSchema = z.enum(['PRINCIPAL', 'SECUNDARIO', 'COMPLICACION', 'COMORBILIDAD']);
export const certaintySchema = z.enum(['CONFIRMED', 'PROBABLE', 'SOSPECHA', 'RULED_OUT']);

export const diagnosisSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  cie10Code: z.string().regex(/^[A-Z]\d{2}(\.\d{1,2})?$/, 'Código CIE-10 inválido'),
  cie10Description: z.string().max(200).optional(),
  type: diagnosisTypeSchema,
  certainty: certaintySchema,
  isPrincipal: z.boolean().optional(),
});

export type DiagnosisFormData = z.infer<typeof diagnosisSchema>;

export const treatmentPlanSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  conduct: z.string().max(5000).optional(),
  objectives: z.string().max(5000).optional(),
  followUpInstructions: z.string().max(5000).optional(),
  prognosis: z.string().max(5000).optional(),
  referredTo: z.string().max(200).optional(),
  referralReason: z.string().max(500).optional(),
});

export type TreatmentPlanFormData = z.infer<typeof treatmentPlanSchema>;

export const medicalOrderSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  orderType: z.enum(['MEDICATION', 'EXAM', 'PROCEDURE', 'REFERRAL', 'OTHER']),
  description: z.string().min(1, 'La descripción es obligatoria').max(1000),
  frequency: z.string().max(100).optional(),
  duration: z.string().max(100).optional(),
  priority: z.enum(['ROUTINE', 'URGENT', 'STAT']).optional(),
  notes: z.string().max(500).optional(),
});

export type MedicalOrderFormData = z.infer<typeof medicalOrderSchema>;