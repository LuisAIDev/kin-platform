import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type AnamnesisResponse, type AnamnesisFormData } from '@/lib/hce/api/hce.api';

export const anamnesisKeys = {
  all: ['hce', 'anamnesis'] as const,
  detail: (encounterId: string) => [...anamnesisKeys.all, 'detail', encounterId] as const,
};

export function useAnamnesis(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? anamnesisKeys.detail(encounterId) : ['hce', 'anamnesis', 'empty'],
    queryFn: () => hceApi.getAnamnesis(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useUpdateAnamnesis(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AnamnesisFormData) => hceApi.updateAnamnesis(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: anamnesisKeys.detail(encounterId) });
    },
  });
}