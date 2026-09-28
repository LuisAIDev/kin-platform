import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type TreatmentPlanResponse, type TreatmentPlanFormData } from '@/lib/hce/api/hce.api';

export const treatmentPlanKeys = {
  all: ['hce', 'treatmentPlan'] as const,
  detail: (encounterId: string) => [...treatmentPlanKeys.all, 'detail', encounterId] as const,
};

export function useTreatmentPlan(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? treatmentPlanKeys.detail(encounterId) : ['hce', 'treatmentPlan', 'empty'],
    queryFn: () => hceApi.getTreatmentPlan(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useCreateTreatmentPlan(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TreatmentPlanFormData) => hceApi.createTreatmentPlan(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: treatmentPlanKeys.detail(encounterId) });
    },
  });
}

export function useUpdateTreatmentPlan(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TreatmentPlanFormData) => hceApi.updateTreatmentPlan(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: treatmentPlanKeys.detail(encounterId) });
    },
  });
}