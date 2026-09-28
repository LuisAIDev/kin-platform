import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type EncounterResponse, type EncounterUpdateData } from '@/lib/hce/api/hce.api';

export const encounterKeys = {
  all: ['hce', 'encounter'] as const,
  detail: (encounterId: string) => [...encounterKeys.all, 'detail', encounterId] as const,
};

export function useEncounter(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? encounterKeys.detail(encounterId) : ['hce', 'encounter', 'empty'],
    queryFn: () => hceApi.getEncounter(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useUpdateEncounter(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EncounterUpdateData) => hceApi.updateEncounter(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: encounterKeys.detail(encounterId) });
    },
  });
}

export function useCloseEncounter(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => hceApi.closeEncounter(encounterId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: encounterKeys.detail(encounterId) });
    },
  });
}