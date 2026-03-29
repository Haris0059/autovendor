import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { OlxAccount } from "@/types/olx";

export function useOlxAccounts() {
  return useQuery({
    queryKey: ["olx-accounts"],
    queryFn: () => api.get<OlxAccount[]>("/olx/accounts"),
  });
}

export function useOlxAccount(id: number) {
  return useQuery({
    queryKey: ["olx-accounts", id],
    queryFn: () => api.get<OlxAccount>(`/olx/accounts/${id}`),
    enabled: !!id,
  });
}

export function useCreateOlxAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { username: string; password: string }) =>
      api.post<OlxAccount>("/olx/accounts", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["olx-accounts"] });
    },
  });
}

export function useDeleteOlxAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => api.delete(`/olx/accounts/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["olx-accounts"] });
    },
  });
}
