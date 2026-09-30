import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type HistoryResponse, type HistoryFormData } from '@/lib/hce/api/hce.api';

export const historyKeys = {
  all: ['hce', 'history'] as const,
  detail: (patientId: string) => [...historyKeys.all, 'detail', patientId] as const,
};

export function usePatientHistory(patientId: string | null) {
  return useQuery({
    queryKey: patientId ? historyKeys.detail(patientId) : ['hce', 'history', 'empty'],
    queryFn: async () => {
      try {
        return await hceApi.getPatientHistory(patientId!);
      } catch (err) {
        // 404 = paciente sin antecedentes registrados (recurso opcional) -> vacío, no error.
        if ((err as { status?: number })?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!patientId,
    staleTime: 30_000,
  });
}

export function useCreateHistoryItem(patientId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: HistoryFormData) => hceApi.createHistoryItem(patientId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: historyKeys.detail(patientId) });
    },
  });
}

export function useUpdateHistoryItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ historyId, data }: { historyId: string; data: Partial<HistoryFormData> }) =>
      hceApi.updateHistoryItem(historyId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: historyKeys.all });
    },
  });
}

export function useDeleteHistoryItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (historyId: string) => hceApi.deleteHistoryItem(historyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: historyKeys.all });
    },
  });
}