import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type PatientIdentificationResponse, type PatientIdentificationFormData } from '@/lib/hce/api/hce.api';

export const patientIdentificationKeys = {
  all: ['hce', 'patientIdentification'] as const,
  detail: (patientId: string) => [...patientIdentificationKeys.all, 'detail', patientId] as const,
};

export function usePatientIdentification(patientId: string | null) {
  return useQuery({
    queryKey: patientId ? patientIdentificationKeys.detail(patientId) : ['hce', 'patientIdentification', 'empty'],
    queryFn: async () => {
      try {
        return await hceApi.getPatientIdentification(patientId!);
      } catch (err) {
        // 404 = el paciente aún no tiene identificación (recurso opcional): se
        // trata como "sin datos" (null) y no como error.
        if ((err as { status?: number })?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!patientId,
    staleTime: 30_000,
  });
}

export function useUpdatePatientIdentification(patientId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PatientIdentificationFormData) => hceApi.updatePatientIdentification(patientId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: patientIdentificationKeys.detail(patientId) });
    },
  });
}