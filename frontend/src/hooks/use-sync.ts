import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay, paginate } from "@/lib/mocks";
import {
  mockProductLinks,
  mockCategoryMappings,
  mockSyncLogs,
} from "@/lib/mocks/sync";
import type {
  ProductLink,
  CategoryMapping,
  SyncLog,
  SyncDirection,
} from "@/types/sync";
import type { PaginatedResponse } from "@/types/api";

let mockLinks: ProductLink[] = [...mockProductLinks];
let mockMappings: CategoryMapping[] = [...mockCategoryMappings];
const mockLogs: SyncLog[] = [...mockSyncLogs];
let nextLinkId = 2000;
let nextMappingId = 500;
let nextLogId = 10000;

export function useProductLinks() {
  return useQuery({
    queryKey: ["sync", "links"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...mockLinks]);
      return api.get<ProductLink[]>("/sync/links");
    },
  });
}

export function useCategoryMappings() {
  return useQuery({
    queryKey: ["sync", "mappings"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...mockMappings]);
      return api.get<CategoryMapping[]>("/sync/mappings");
    },
  });
}

interface SyncHistoryFilters {
  page?: number;
  per_page?: number;
  status?: string;
  account_id?: number;
  store_id?: number;
}

export function useSyncHistory(filters: SyncHistoryFilters = {}) {
  return useQuery({
    queryKey: ["sync", "history", filters],
    queryFn: () => {
      if (USE_MOCKS) {
        let list = [...mockLogs].sort((a, b) =>
          b.created_at.localeCompare(a.created_at)
        );
        if (filters.status) list = list.filter((l) => l.status === filters.status);
        return mockDelay(paginate(list, filters.page ?? 1, filters.per_page ?? 20));
      }
      const params = Object.fromEntries(
        Object.entries(filters)
          .filter(([, v]) => v !== undefined)
          .map(([k, v]) => [k, String(v)])
      );
      return api.get<PaginatedResponse<SyncLog>>("/sync/history", { params });
    },
  });
}

export function useTriggerSync() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (productLinkId: number) => {
      if (USE_MOCKS) {
        const link = mockLinks.find((l) => l.id === productLinkId);
        if (link) {
          link.last_synced_at = new Date().toISOString();
          mockLogs.unshift({
            id: nextLogId++,
            product_link_id: productLinkId,
            action: "manual",
            status: "success",
            message: "Ručno pokrenuta sinhronizacija.",
            created_at: new Date().toISOString(),
          });
        }
        return mockDelay({ ok: true });
      }
      return api.post("/sync", { product_link_id: productLinkId });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync"] });
    },
  });
}

export function useCreateProductLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      olx_account_id: number;
      woo_store_id: number;
      olx_listing_id: number;
      woo_product_id: number;
      sync_direction: SyncDirection;
    }) => {
      if (USE_MOCKS) {
        const created: ProductLink = {
          id: nextLinkId++,
          ...data,
          last_synced_at: null,
        };
        mockLinks = [created, ...mockLinks];
        return mockDelay(created);
      }
      return api.post<ProductLink>("/sync/links", data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync"] });
    },
  });
}

export function useDeleteProductLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        mockLinks = mockLinks.filter((l) => l.id !== id);
        return mockDelay(undefined);
      }
      return api.delete(`/sync/links/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync"] });
    },
  });
}

export function useCreateCategoryMapping() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      woo_category_id: number;
      woo_category_name: string;
      olx_category_id: number;
      olx_category_name: string;
    }) => {
      if (USE_MOCKS) {
        const created: CategoryMapping = { id: nextMappingId++, ...data };
        mockMappings = [created, ...mockMappings];
        return mockDelay(created);
      }
      return api.post<CategoryMapping>("/sync/mappings", data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync", "mappings"] });
    },
  });
}

export function useDeleteCategoryMapping() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        mockMappings = mockMappings.filter((m) => m.id !== id);
        return mockDelay(undefined);
      }
      return api.delete(`/sync/mappings/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sync", "mappings"] });
    },
  });
}
