import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { hceApi, type MedicalOrderResponse, type MedicalOrderFormData } from '@/lib/hce/api/hce.api';

export const medicalOrderKeys = {
  all: ['hce', 'medicalOrders'] as const,
  list: (encounterId: string) => [...medicalOrderKeys.all, 'list', encounterId] as const,
  detail: (orderId: string) => [...medicalOrderKeys.all, 'detail', orderId] as const,
};

export function useMedicalOrders(encounterId: string | null) {
  return useQuery({
    queryKey: encounterId ? medicalOrderKeys.list(encounterId) : ['hce', 'medicalOrders', 'empty'],
    queryFn: () => hceApi.getMedicalOrders(encounterId!),
    enabled: !!encounterId,
    staleTime: 30_000,
  });
}

export function useCreateMedicalOrder(encounterId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: MedicalOrderFormData) => hceApi.createMedicalOrder(encounterId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: medicalOrderKeys.list(encounterId) });
    },
  });
}

export function useUpdateMedicalOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, data }: { orderId: string; data: Partial<MedicalOrderFormData> }) =>
      hceApi.updateMedicalOrder(orderId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: medicalOrderKeys.detail(variables.orderId) });
    },
  });
}

export function useDeleteMedicalOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: string) => hceApi.deleteMedicalOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: medicalOrderKeys.all });
    },
  });
}