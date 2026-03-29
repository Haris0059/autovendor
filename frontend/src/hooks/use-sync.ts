import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { ProductLink, CategoryMapping, SyncLog } from "@/types/sync";
import type { PaginatedResponse } from "@/types/api";

export function useProductLinks() {
  return useQuery({
    queryKey: ["sync", "links"],
    queryFn: () => api.get<ProductLink[]>("/sync/links"),
  });
}

export function useCategoryMappings() {
  return useQuery({
    queryKey: ["sync", "mappings"],
    queryFn: () => api.get<CategoryMapping[]>("/sync/mappings"),
  });
}

export function useSyncHistory(page = 1) {
  return useQuery({
    queryKey: ["sync", "history", page],
    queryFn: () =>
      api.get<PaginatedResponse<SyncLog>>("/sync/history", {
        params: { page: String(page) },
      }),
  });
}

export function useTriggerSync() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productLinkId: number) =>
      api.post("/sync", { product_link_id: productLinkId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync"] });
    },
  });
}
