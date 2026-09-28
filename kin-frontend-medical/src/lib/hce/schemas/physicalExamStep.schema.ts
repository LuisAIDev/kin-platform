import { z } from 'zod';

export const physicalExamSchema = z.object({
  encounterId: z.string().uuid(),
  bpSystolic: z.number().int().min(50, 'Mínimo 50 mmHg').max(300, 'Máximo 300 mmHg').optional(),
  bpDiastolic: z.number().int().min(30, 'Mínimo 30 mmHg').max(200, 'Máximo 200 mmHg').optional(),
  heartRate: z.number().int().min(30, 'Mínimo 30 lpm').max(250, 'Máximo 250 lpm').optional(),
  respiratoryRate: z.number().int().min(5, 'Mínimo 5 rpm').max(80, 'Máximo 80 rpm').optional(),
  temperature: z.number().min(30.0, 'Mínimo 30.0°C').max(45.0, 'Máximo 45.0°C').step(0.1).optional(),
  spo2: z.number().int().min(50, 'Mínimo 50%').max(100, 'Máximo 100%').optional(),
  weightKg: z.number().min(0.1, 'Mínimo 0.1 kg').max(500, 'Máximo 500 kg').step(0.1).optional(),
  heightCm: z.number().int().min(20, 'Mínimo 20 cm').max(250, 'Máximo 250 cm').optional(),
  glasgowScore: z.number().int().min(3, 'Mínimo 3').max(15, 'Máximo 15').optional(),
  painScale: z.number().int().min(0, 'Mínimo 0').max(10, 'Máximo 10').optional(),
  generalAppearance: z.string().max(1000).optional(),
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

export const validateVitals = (data: Partial<PhysicalExamFormData>): { valid: boolean; errors: Record<string, string> } => {
  const errors: Record<string, string> = {};

  if (data.bpSystolic !== undefined && (data.bpSystolic < 50 || data.bpSystolic > 300)) {
    errors.bpSystolic = 'Presión sistólica debe estar entre 50 y 300 mmHg';
  }
  if (data.bpDiastolic !== undefined && (data.bpDiastolic < 30 || data.bpDiastolic > 200)) {
    errors.bpDiastolic = 'Presión diastólica debe estar entre 30 y 200 mmHg';
  }
  if (data.heartRate !== undefined && (data.heartRate < 30 || data.heartRate > 250)) {
    errors.heartRate = 'Frecuencia cardíaca debe estar entre 30 y 250 lpm';
  }
  if (data.respiratoryRate !== undefined && (data.respiratoryRate < 5 || data.respiratoryRate > 80)) {
    errors.respiratoryRate = 'Frecuencia respiratoria debe estar entre 5 y 80 rpm';
  }
  if (data.temperature !== undefined && (data.temperature < 30 || data.temperature > 45)) {
    errors.temperature = 'Temperatura debe estar entre 30.0°C y 45.0°C';
  }
  if (data.spo2 !== undefined && (data.spo2 < 50 || data.spo2 > 100)) {
    errors.spo2 = 'SpO2 debe estar entre 50% y 100%';
  }
  if (data.weightKg !== undefined && (data.weightKg < 0.1 || data.weightKg > 500)) {
    errors.weightKg = 'Peso debe estar entre 0.1 y 500 kg';
  }
  if (data.heightCm !== undefined && (data.heightCm < 20 || data.heightCm > 250)) {
    errors.heightCm = 'Talla debe estar entre 20 y 250 cm';
  }
  if (data.glasgowScore !== undefined && (data.glasgowScore < 3 || data.glasgowScore > 15)) {
    errors.glasgowScore = 'Escala de Glasgow debe estar entre 3 y 15';
  }
  if (data.painScale !== undefined && (data.painScale < 0 || data.painScale > 10)) {
    errors.painScale = 'Escala de dolor debe estar entre 0 y 10';
  }

  return { valid: Object.keys(errors).length === 0, errors };
};