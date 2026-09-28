import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type PhysicalExamResponse, type PhysicalExamFormData } from '@/lib/hce/api/hce.api';

export const physicalExamKeys = {
  all: ['hce', 'physicalExam'] as const,
  detail: (encounterId: string) => [...physicalExamKeys.all, 'detail', encounterId] as const,
};

export function usePhysicalExam(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? physicalExamKeys.detail(encounterId) : ['hce', 'physicalExam', 'empty'],
    queryFn: () => hceApi.getPhysicalExam(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useUpdatePhysicalExam(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PhysicalExamFormData) => hceApi.updatePhysicalExam(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: physicalExamKeys.detail(encounterId) });
    },
  });
}