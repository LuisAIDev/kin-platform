import { z } from 'zod';

export const allergySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  allergen: z.string().min(1, 'El alérgeno es obligatorio').max(200),
  reaction: z.string().max(500).optional(),
  severity: z.enum(['LEVE', 'MODERADA', 'GRAVE', 'ANAFILAXIA']).optional(),
  onsetDate: z.string().date('Fecha inválida').optional(),
  notes: z.string().max(500).optional(),
});

export type AllergyFormData = z.infer<typeof allergySchema>;

export const surgerySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  procedure: z.string().min(1, 'El procedimiento es obligatorio').max(200),
  date: z.string().date('Fecha inválida'),
  complications: z.string().max(500).optional(),
  notes: z.string().max(500).optional(),
});

export type SurgeryFormData = z.infer<typeof surgerySchema>;

export const medicationSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  name: z.string().min(1, 'El nombre del medicamento es obligatorio').max(200),
  dosage: z.string().max(100).optional(),
  frequency: z.string().max(100).optional(),
  route: z.string().max(50).optional(),
  startDate: z.string().date('Fecha inválida').optional(),
  endDate: z.string().date('Fecha inválida').optional(),
  isActive: z.boolean().optional(),
  indication: z.string().max(200).optional(),
  notes: z.string().max(500).optional(),
});

export type MedicationFormData = z.infer<typeof medicationSchema>;

export const vaccineSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  vaccine: z.string().min(1, 'La vacuna es obligatoria').max(200),
  date: z.string().date('Fecha inválida'),
  dose: z.string().max(50).optional(),
  batch: z.string().max(50).optional(),
  nextDoseDate: z.string().date('Fecha inválida').optional(),
  notes: z.string().max(500).optional(),
});

export type VaccineFormData = z.infer<typeof vaccineSchema>;

export const familyHistorySchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  relative: z.enum(['MADRE', 'PADRE', 'HERMANO', 'HIJO', 'ABUELO_MATERNO', 'ABUELA_MATERNA', 'ABUELO_PATERNO', 'ABUELA_PATERNA', 'TIO', 'TIA', 'OTRO']),
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
  substance: z.enum(['TABACO', 'ALCOHOL', 'CANNABIS', 'COCAINA', 'OTRAS_DROGAS']),
  frequency: z.string().max(100).optional(),
  quantity: z.string().max(100).optional(),
  startDate: z.string().date('Fecha inválida').optional(),
  endDate: z.string().date('Fecha inválida').optional(),
  isActive: z.boolean().optional(),
  notes: z.string().max(500).optional(),
});

export type ToxicHabitFormData = z.infer<typeof toxicHabitSchema>;

export const gynecoObstetricSchema = z.object({
  id: z.string().uuid().optional(),
  patientId: z.string().uuid(),
  gravida: z.number().int().min(0).optional(),
  para: z.number().int().min(0).optional(),
  abortions: z.number().int().min(0).optional(),
  cesareans: z.number().int().min(0).optional(),
  livingChildren: z.number().int().min(0).optional(),
  lastMenstrualPeriod: z.string().date('Fecha inválida').optional(),
  menarcheAge: z.number().int().min(8).max(30).optional(),
  menopauseAge: z.number().int().min(35).max(65).optional(),
  contraceptiveMethod: z.string().max(100).optional(),
  notes: z.string().max(500).optional(),
});

export type GynecoObstetricFormData = z.infer<typeof gynecoObstetricSchema>;

export const historySchema = z.object({
  allergies: z.array(allergySchema).default([]),
  surgeries: z.array(surgerySchema).default([]),
  medications: z.array(medicationSchema).default([]),
  vaccines: z.array(vaccineSchema).default([]),
  familyHistory: z.array(familyHistorySchema).default([]),
  toxicHabits: z.array(toxicHabitSchema).default([]),
  gynecoObstetric: gynecoObstetricSchema.optional(),
});

export type HistoryFormData = z.infer<typeof historySchema>;