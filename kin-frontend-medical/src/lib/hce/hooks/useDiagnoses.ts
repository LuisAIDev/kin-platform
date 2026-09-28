import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type DiagnosisResponse, type DiagnosisFormData } from '@/lib/hce/api/hce.api';

export const diagnosesKeys = {
  all: ['hce', 'diagnoses'] as const,
  list: (encounterId: string) => [...diagnosesKeys.all, 'list', encounterId] as const,
  detail: (diagnosisId: string) => [...diagnosesKeys.all, 'detail', diagnosisId] as const,
};

export function useDiagnoses(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? diagnosesKeys.list(encounterId) : ['hce', 'diagnoses', 'empty'],
    queryFn: () => hceApi.getDiagnoses(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useCreateDiagnosis(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: DiagnosisFormData) => hceApi.createDiagnosis(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: diagnosesKeys.list(encounterId) });
    },
  });
}

export function useUpdateDiagnosis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ diagnosisId, data }: { diagnosisId: string; data: Partial<DiagnosisFormData> }) =>
      hceApi.updateDiagnosis(diagnosisId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: diagnosesKeys.detail(variables.diagnosisId) });
    },
  });
}

export function useDeleteDiagnosis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (diagnosisId: string) => hceApi.deleteDiagnosis(diagnosisId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: diagnosesKeys.all });
    },
  });
}

export function useSetPrincipalDiagnosis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (diagnosisId: string) => hceApi.setPrincipalDiagnosis(diagnosisId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: diagnosesKeys.all });
    },
  });
}