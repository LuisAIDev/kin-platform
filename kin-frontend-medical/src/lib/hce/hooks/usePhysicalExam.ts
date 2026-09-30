import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type PhysicalExamResponse, type PhysicalExamFormData } from '@/lib/hce/api/hce.api';

export const physicalExamKeys = {
  all: ['hce', 'physicalExam'] as const,
  detail: (encounterId: string) => [...physicalExamKeys.all, 'detail', encounterId] as const,
};

export function usePhysicalExam(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? physicalExamKeys.detail(encounterId) : ['hce', 'physicalExam', 'empty'],
    queryFn: async () => {
      try {
        return await hceApi.getPhysicalExam(encounterId!);
      } catch (err) {
        // 404 = examen físico aún no registrado (recurso opcional) -> vacío, no error.
        if ((err as { status?: number })?.status === 404) return null;
        throw err;
      }
    },
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