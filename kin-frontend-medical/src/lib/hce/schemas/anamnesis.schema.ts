import { z } from 'zod';

export const systemsReviewSchema = z.object({
  general: z.string().max(500).optional(),
  skin: z.string().max(500).optional(),
  headNeck: z.string().max(500).optional(),
  eyes: z.string().max(500).optional(),
  ears: z.string().max(500).optional(),
  noseThroat: z.string().max(500).optional(),
  cardiovascular: z.string().max(500).optional(),
  respiratory: z.string().max(500).optional(),
  gastrointestinal: z.string().max(500).optional(),
  genitourinary: z.string().max(500).optional(),
  musculoskeletal: z.string().max(500).optional(),
  neurological: z.string().max(500).optional(),
  psychiatric: z.string().max(500).optional(),
  endocrine: z.string().max(500).optional(),
});

export type SystemsReviewData = z.infer<typeof systemsReviewSchema>;

export const anamnesisSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  onsetDatetime: z.string().datetime('Fecha de inicio inválida').optional(),
  evolutionDescription: z.string().max(2000).optional(),
  aggravatingFactors: z.string().max(1000).optional(),
  alleviatingFactors: z.string().max(1000).optional(),
  associatedSymptoms: z.string().max(1000).optional(),
  severitySelfReported: z.number().int().min(1).max(10).optional(),
  systemsReview: systemsReviewSchema.optional(),
  previousEpisodes: z.string().max(1000).optional(),
  previousTreatments: z.string().max(1000).optional(),
  functionalImpact: z.string().max(1000).optional(),
});

export type AnamnesisFormData = z.infer<typeof anamnesisSchema>;