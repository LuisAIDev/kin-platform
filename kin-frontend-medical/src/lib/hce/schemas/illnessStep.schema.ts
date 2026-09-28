import { z } from 'zod';

export const systemsReviewItemSchema = z.object({
  checked: z.boolean().default(false),
  notes: z.string().max(1000).optional(),
});

export type SystemsReviewItem = z.infer<typeof systemsReviewItemSchema>;

export const systemsReviewSchema = z.object({
  general: systemsReviewItemSchema.default({ checked: false }),
  cardiovascular: systemsReviewItemSchema.default({ checked: false }),
  respiratory: systemsReviewItemSchema.default({ checked: false }),
  digestive: systemsReviewItemSchema.default({ checked: false }),
  genitourinary: systemsReviewItemSchema.default({ checked: false }),
  musculoskeletal: systemsReviewItemSchema.default({ checked: false }),
  neurological: systemsReviewItemSchema.default({ checked: false }),
  endocrine: systemsReviewItemSchema.default({ checked: false }),
  hematological: systemsReviewItemSchema.default({ checked: false }),
  dermatological: systemsReviewItemSchema.default({ checked: false }),
  psychiatric: systemsReviewItemSchema.default({ checked: false }),
  ophthalmological: systemsReviewItemSchema.default({ checked: false }),
  otorhinolaryngological: systemsReviewItemSchema.default({ checked: false }),
  other: systemsReviewItemSchema.default({ checked: false }),
});

export type SystemsReviewData = z.infer<typeof systemsReviewSchema>;

export const illnessStepSchema = z.object({
  encounterId: z.string().uuid(),
  onsetDatetime: z.string().datetime('Fecha y hora de inicio inválida').optional()
    .refine((val) => !val || new Date(val) <= new Date(), 'La fecha de inicio no puede ser futura'),
  evolutionDescription: z.string().max(2000, 'Máximo 2000 caracteres').optional(),
  aggravatingFactors: z.string().max(1000, 'Máximo 1000 caracteres').optional(),
  alleviatingFactors: z.string().max(1000, 'Máximo 1000 caracteres').optional(),
  associatedSymptoms: z.string().max(1000, 'Máximo 1000 caracteres').optional(),
  severitySelfReported: z.number().int().min(1, 'Mínimo 1').max(10, 'Máximo 10').optional(),
  systemsReview: systemsReviewSchema.optional(),
  previousEpisodes: z.number().int().min(0, 'No puede ser negativo').optional(),
  previousTreatments: z.string().max(1000, 'Máximo 1000 caracteres').optional(),
  functionalImpact: z.string().max(1000, 'Máximo 1000 caracteres').optional(),
});

export type IllnessStepFormData = z.infer<typeof illnessStepSchema>;

export const validateOnsetDatetime = (value: string): { valid: boolean; message?: string } => {
  if (!value) return { valid: true };
  const onset = new Date(value);
  const now = new Date();
  if (onset > now) {
    return { valid: false, message: 'La fecha de inicio no puede ser futura' };
  }
  return { valid: true };
};