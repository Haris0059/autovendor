import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import { mockOlxAccounts } from "@/lib/mocks/olx-accounts";
import type { OlxAccount } from "@/types/olx";

let mockAccountsStore: OlxAccount[] = [...mockOlxAccounts];
let nextMockId = 1000;

export function useOlxAccounts() {
  return useQuery({
    queryKey: ["olx-accounts"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...mockAccountsStore]);
      return api.get<OlxAccount[]>("/olx/accounts");
    },
  });
}

export function useOlxAccount(id: number) {
  return useQuery({
    queryKey: ["olx-accounts", id],
    queryFn: () => {
      if (USE_MOCKS) {
        const found = mockAccountsStore.find((a) => a.id === id);
        if (!found) throw new Error("Profil nije pronađen.");
        return mockDelay(found);
      }
      return api.get<OlxAccount>(`/olx/accounts/${id}`);
    },
    enabled: !!id,
  });
}

export function useCreateOlxAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      username: string;
      password: string;
      default_city_id?: number | null;
    }) => {
      if (USE_MOCKS) {
        const created: OlxAccount = {
          id: nextMockId++,
          username: data.username,
          olx_user_id: Math.floor(100000 + Math.random() * 900000),
          default_city_id: data.default_city_id ?? null,
          token_expires_at: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString(),
          created_at: new Date().toISOString(),
        };
        mockAccountsStore = [created, ...mockAccountsStore];
        return mockDelay(created);
      }
      return api.post<OlxAccount>("/olx/accounts", data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["olx-accounts"] });
    },
  });
}

export function useUpdateOlxAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      id: number;
      username?: string;
      password?: string;
      default_city_id?: number | null;
    }) => {
      if (USE_MOCKS) {
        const existing = mockAccountsStore.find((a) => a.id === data.id);
        if (!existing) throw new Error("Profil nije pronađen.");
        const updated: OlxAccount = {
          ...existing,
          username: data.username ?? existing.username,
          default_city_id:
            data.default_city_id !== undefined
              ? data.default_city_id
              : existing.default_city_id,
        };
        mockAccountsStore = mockAccountsStore.map((a) =>
          a.id === data.id ? updated : a
        );
        return mockDelay(updated);
      }
      return api.put<OlxAccount>(`/olx/accounts/${data.id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["olx-accounts"] });
    },
  });
}

export function useDeleteOlxAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        mockAccountsStore = mockAccountsStore.filter((a) => a.id !== id);
        return mockDelay(undefined);
      }
      return api.delete(`/olx/accounts/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["olx-accounts"] });
    },
  });
}
