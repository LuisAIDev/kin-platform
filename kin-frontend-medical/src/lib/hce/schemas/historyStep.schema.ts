import { z } from 'zod';

export const allergySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  allergen: z.string().min(1, 'El alérgeno es obligatorio').max(200),
  reaction: z.string().max(500).optional(),
  severity: z.enum(['MILD', 'MODERATE', 'SEVERE', 'ANAPHYLAXIS']).default('MILD'),
  onsetDate: z.string().date('Fecha inválida').optional(),
  status: z.enum(['ACTIVE', 'RESOLVED', 'UNKNOWN']).default('ACTIVE'),
  notes: z.string().max(500).optional(),
});

export type AllergyFormData = z.infer<typeof allergySchema>;

export const surgerySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  procedure: z.string().min(1, 'El procedimiento es obligatorio').max(200),
  date: z.string().date('Fecha inválida'),
  hospital: z.string().max(200).optional(),
  complications: z.string().max(500).optional(),
  notes: z.string().max(500).optional(),
});

export type SurgeryFormData = z.infer<typeof surgerySchema>;

export const medicationSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  name: z.string().min(1, 'El nombre es obligatorio').max(200),
  dosage: z.string().max(100).optional(),
  frequency: z.string().max(100).optional(),
  route: z.string().max(50).optional(),
  startDate: z.string().date('Fecha inválida').optional(),
  endDate: z.string().date('Fecha inválida').optional(),
  isActive: z.boolean().default(true),
  indication: z.string().max(200).optional(),
  notes: z.string().max(500).optional(),
});

export type MedicationFormData = z.infer<typeof medicationSchema>;

export const vaccineSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  name: z.string().min(1, 'El nombre es obligatorio').max(200),
  dose: z.string().max(50).optional(),
  date: z.string().date('Fecha inválida'),
  batch: z.string().max(50).optional(),
  nextDoseDate: z.string().date('Fecha inválida').optional(),
  notes: z.string().max(500).optional(),
});

export type VaccineFormData = z.infer<typeof vaccineSchema>;

export const familyHistorySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  relationship: z.enum([
    'FATHER', 'MOTHER', 'BROTHER', 'SISTER',
    'SON', 'DAUGHTER', 'GRANDFATHER_PATERNAL', 'GRANDMOTHER_PATERNAL',
    'GRANDFATHER_MATERNAL', 'GRANDMOTHER_MATERNAL', 'UNCLE', 'AUNT',
    'COUSIN', 'OTHER',
  ]),
  condition: z.string().min(1, 'La condición es obligatoria').max(200),
  ageOfOnset: z.number().int().min(0).max(120).optional(),
  isDeceased: z.boolean().optional(),
  ageAtDeath: z.number().int().min(0).max(120).optional(),
  notes: z.string().max(500).optional(),
});

export type FamilyHistoryFormData = z.infer<typeof familyHistorySchema>;

export const toxicHabitSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  substance: z.enum(['TOBACCO', 'ALCOHOL', 'CANNABIS', 'COCAINE', 'OPIOIDS', 'OTHER']),
  frequency: z.string().max(100).optional(),
  quantity: z.string().max(100).optional(),
  startDate: z.string().date('Fecha inválida').optional(),
  endDate: z.string().date('Fecha inválida').optional(),
  isActive: z.boolean().default(true),
  notes: z.string().max(500).optional(),
});

export type ToxicHabitFormData = z.infer<typeof toxicHabitSchema>;

export const gynecoObstetricSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  gravida: z.number().int().min(0).default(0),
  para: z.number().int().min(0).default(0),
  abortions: z.number().int().min(0).default(0),
  cesareans: z.number().int().min(0).default(0),
  livingChildren: z.number().int().min(0).default(0),
  lastMenstrualPeriod: z.string().date('Fecha inválida').optional(),
  menarcheAge: z.number().int().min(8).max(30).optional(),
  menopauseAge: z.number().int().min(35).max(65).optional(),
  contraceptiveMethod: z.string().max(100).optional(),
  notes: z.string().max(500).optional(),
});

export type GynecoObstetricFormData = z.infer<typeof gynecoObstetricSchema>;

export const historyTypeSchema = z.enum([
  'ALLERGY', 'SURGERY', 'MEDICATION', 'VACCINE',
  'FAMILY_HISTORY', 'TOXICOLOGICAL', 'GYNECO_OBSTETRIC',
]);

export type HistoryType = z.infer<typeof historyTypeSchema>;

export const historyItemSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  type: historyTypeSchema,
  data: z.unknown(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional(),
});

export type HistoryItemFormData = z.infer<typeof historyItemSchema>;

export const createHistoryItemSchema = (type: HistoryType) => {
  switch (type) {
    case 'ALLERGY': return allergySchema;
    case 'SURGERY': return surgerySchema;
    case 'MEDICATION': return medicationSchema;
    case 'VACCINE': return vaccineSchema;
    case 'FAMILY_HISTORY': return familyHistorySchema;
    case 'TOXICOLOGICAL': return toxicHabitSchema;
    case 'GYNECO_OBSTETRIC': return gynecoObstetricSchema;
  }
};

export const getHistoryTypeLabel = (type: HistoryType): string => {
  const labels: Record<HistoryType, string> = {
    ALLERGY: 'Alergias',
    SURGERY: 'Cirugías',
    MEDICATION: 'Medicamentos',
    VACCINE: 'Vacunas',
    FAMILY_HISTORY: 'Familiares',
    TOXICOLOGICAL: 'Tóxicos',
    GYNECO_OBSTETRIC: 'Gineco-Obstétricos',
  };
  return labels[type];
};

export const getHistoryTypeIcon = (type: HistoryType): string => {
  const icons: Record<HistoryType, string> = {
    ALLERGY: 'AlertTriangle',
    SURGERY: 'Scissors',
    MEDICATION: 'Pill',
    VACCINE: 'Syringe',
    FAMILY_HISTORY: 'Users',
    TOXICOLOGICAL: 'Skull',
    GYNECO_OBSTETRIC: 'Baby',
  };
  return icons[type];
};