import { z } from 'zod';

export const physicalExamSchema = z.object({
  id: z.string().uuid().optional(),
  encounterId: z.string().uuid(),
  bpSystolic: z.number().int().min(50, 'Mínimo 50').max(300, 'Máximo 300').optional(),
  bpDiastolic: z.number().int().min(30, 'Mínimo 30').max(200, 'Máximo 200').optional(),
  heartRate: z.number().int().min(30, 'Mínimo 30').max(250, 'Máximo 250').optional(),
  respiratoryRate: z.number().int().min(5, 'Mínimo 5').max(60, 'Máximo 60').optional(),
  temperature: z.number().min(30, 'Mínimo 30°C').max(43, 'Máximo 43°C').optional(),
  spo2: z.number().int().min(50, 'Mínimo 50%').max(100, 'Máximo 100%').optional(),
  weightKg: z.number().min(0.5, 'Mínimo 0.5 kg').max(300, 'Máximo 300 kg').optional(),
  heightCm: z.number().int().min(30, 'Mínimo 30 cm').max(250, 'Máximo 250 cm').optional(),
  bmi: z.number().min(10, 'IMC mínimo 10').max(70, 'IMC máximo 70').optional(),
  glasgowScore: z.number().int().min(3, 'Mínimo 3').max(15, 'Máximo 15').optional(),
  painScale: z.number().int().min(0, 'Mínimo 0').max(10, 'Máximo 10').optional(),
  headNeck: z.string().max(1000).optional(),
  cardiovascular: z.string().max(1000).optional(),
  respiratory: z.string().max(1000).optional(),
  abdominal: z.string().max(1000).optional(),
  neurological: z.string().max(1000).optional(),
  musculoskeletal: z.string().max(1000).optional(),
  skin: z.string().max(1000).optional(),
  genitourinary: z.string().max(1000).optional(),
  psychiatric: z.string().max(1000).optional(),
});

export type PhysicalExamFormData = z.infer<typeof physicalExamSchema>;

export const calculateBMI = (weightKg: number | undefined, heightCm: number | undefined): number | undefined => {
  if (!weightKg || !heightCm || heightCm === 0) return undefined;
  const heightM = heightCm / 100;
  return Math.round((weightKg / (heightM * heightM)) * 10) / 10;
};