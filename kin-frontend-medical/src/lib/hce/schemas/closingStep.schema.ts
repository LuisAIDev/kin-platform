import { z } from 'zod';

export const closingStepSchema = z.object({
  encounterId: z.string().uuid(),
  hasPrincipalDiagnosis: z.boolean(),
  hasTreatmentPlan: z.boolean(),
  isEncounterOpen: z.boolean(),
  confirmation: z.boolean().refine((val) => val === true, {
    message: 'Debe confirmar el cierre de la consulta',
  }),
});

export type ClosingStepFormData = z.infer<typeof closingStepSchema>;

export const validateClosingRequirements = (
  hasPrincipalDiagnosis: boolean,
  hasTreatmentPlan: boolean,
  isEncounterOpen: boolean
): { valid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (!hasPrincipalDiagnosis) {
    errors.push('Falta diagnóstico PRINCIPAL (obligatorio según Res 839/1995)');
  }
  if (!hasTreatmentPlan) {
    errors.push('Falta plan de manejo (obligatorio)');
  }
  if (!isEncounterOpen) {
    errors.push('El encuentro ya está cerrado');
  }

  return { valid: errors.length === 0, errors };
};

export const getMissingRequirements = (
  hasPrincipalDiagnosis: boolean,
  hasTreatmentPlan: boolean,
  isEncounterOpen: boolean
): string[] => {
  const missing: string[] = [];
  if (!hasPrincipalDiagnosis) missing.push('diagnóstico PRINCIPAL');
  if (!hasTreatmentPlan) missing.push('plan de manejo');
  if (!isEncounterOpen) missing.push('encuentro abierto');
  return missing;
};