import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type PatientIdentificationResponse, type PatientIdentificationFormData } from '@/lib/hce/api/hce.api';

export const patientIdentificationKeys = {
  all: ['hce', 'patientIdentification'] as const,
  detail: (patientId: string) => [...patientIdentificationKeys.all, 'detail', patientId] as const,
};

export function usePatientIdentification(patientId: string | null) {
  return useQuery({
    queryKey: patientId ? patientIdentificationKeys.detail(patientId) : ['hce', 'patientIdentification', 'empty'],
    queryFn: () => hceApi.getPatientIdentification(patientId!),
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