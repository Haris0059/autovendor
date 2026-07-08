import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay, paginate } from "@/lib/mocks";
import { mockListings } from "@/lib/mocks/listings";
import { useActiveAccount } from "@/hooks/use-active-account";
import { useOlxAccounts } from "@/hooks/use-olx-accounts";
import type { OlxListing, OlxImage } from "@/types/olx";
import type { PaginatedResponse } from "@/types/api";

interface ListingFilters {
  account_id: number;
  status?: string;
  page?: number;
  per_page?: number;
}

const mockListingsStore: (OlxListing & { account_id: number })[] = [...mockListings];

/**
 * Single-listing operations need the owning OLX account for the path-scoped
 * backend routes. Falls back to the first account when none is active yet
 * (e.g. hard refresh on a detail page).
 */
function useResolvedAccountId(): number | null {
  const { account } = useActiveAccount();
  const accountsQuery = useOlxAccounts();
  return account?.id ?? accountsQuery.data?.[0]?.id ?? null;
}

export function useListings(filters: ListingFilters) {
  return useQuery({
    queryKey: ["listings", filters],
    queryFn: () => {
      if (USE_MOCKS) {
        const filtered = mockListingsStore.filter(
          (l) =>
            l.account_id === filters.account_id &&
            (!filters.status || l.status === filters.status)
        );
        return mockDelay(paginate(filtered, filters.page ?? 1, filters.per_page ?? 10));
      }
      const params: Record<string, string> = {};
      if (filters.status) params.status = filters.status;
      if (filters.page) params.page = String(filters.page);
      if (filters.per_page) params.per_page = String(filters.per_page);
      return api.get<PaginatedResponse<OlxListing>>(
        `/olx/accounts/${filters.account_id}/listings`,
        { params }
      );
    },
    enabled: !!filters.account_id,
  });
}

export function useListing(id: number) {
  const accountId = useResolvedAccountId();
  return useQuery({
    queryKey: ["listings", "single", accountId, id],
    queryFn: () => {
      if (USE_MOCKS) {
        const found = mockListingsStore.find((l) => l.id === id);
        if (!found) throw new Error("Artikal nije pronađen.");
        return mockDelay(found);
      }
      return api.get<OlxListing>(`/olx/accounts/${accountId}/listings/${id}`);
    },
    enabled: !!id && (USE_MOCKS || !!accountId),
  });
}

export function useAllListings() {
  return useQuery({
    queryKey: ["listings", "all"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...mockListingsStore]);
      return api.get<OlxListing[]>("/olx/listings/all");
    },
  });
}

export function useCreateListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      account_id: number;
      title: string;
      [key: string]: unknown;
    }) => {
      if (USE_MOCKS) {
        const created: OlxListing = {
          id: Math.floor(50000 + Math.random() * 10000),
          account_id: data.account_id,
          title: data.title,
          description: (data.description as string) ?? null,
          price: (data.price as number) ?? null,
          city_id: (data.city_id as number) ?? null,
          category_id: (data.category_id as number) ?? null,
          listing_type: (data.listing_type as string) ?? "sell",
          state: (data.state as string) ?? "used",
          status: "draft",
          images: [],
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        };
        mockListingsStore.unshift(created);
        return mockDelay(created);
      }
      const { account_id, ...body } = data;
      return api.post<OlxListing>(`/olx/accounts/${account_id}/listings`, body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useUpdateListing() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (data: { id: number; [key: string]: unknown }) => {
      if (USE_MOCKS) {
        const idx = mockListingsStore.findIndex((l) => l.id === data.id);
        if (idx === -1) throw new Error("Artikal nije pronađen.");
        const current = mockListingsStore[idx];
        const merged = {
          ...current,
          ...data,
          id: current.id,
          account_id: current.account_id,
          images: current.images,
          updated_at: new Date().toISOString(),
        } as OlxListing;
        mockListingsStore[idx] = merged;
        return mockDelay(merged);
      }
      const { id, ...body } = data;
      return api.put<OlxListing>(`/olx/accounts/${accountId}/listings/${id}`, body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useDeleteListing() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        const idx = mockListingsStore.findIndex((l) => l.id === id);
        if (idx !== -1) mockListingsStore.splice(idx, 1);
        return mockDelay(undefined);
      }
      return api.delete(`/olx/accounts/${accountId}/listings/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export type ListingAction =
  | "publish"
  | "finish"
  | "hide"
  | "unhide"
  | "refresh";

/** Which actions make sense for a listing in a given status. */
export const LISTING_ACTIONS_BY_STATUS: Record<string, ListingAction[]> = {
  active: ["refresh", "hide", "finish"],
  hidden: ["unhide", "finish"],
  draft: ["publish"],
  inactive: ["publish"],
  expired: ["publish"],
  finished: ["publish"],
};

function applyStatus(action: ListingAction): string | null {
  switch (action) {
    case "publish":
      return "active";
    case "finish":
      return "finished";
    case "hide":
      return "hidden";
    case "unhide":
      return "active";
    case "refresh":
      return null;
  }
}

export function useListingAction() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (vars: { id: number; action: ListingAction }) => {
      if (USE_MOCKS) {
        const idx = mockListingsStore.findIndex((l) => l.id === vars.id);
        if (idx !== -1) {
          const newStatus = applyStatus(vars.action);
          mockListingsStore[idx] = {
            ...mockListingsStore[idx],
            status: newStatus ?? mockListingsStore[idx].status,
            updated_at: new Date().toISOString(),
          };
        }
        return mockDelay({ ok: true });
      }
      const endpoint = `/olx/accounts/${accountId}/listings/${vars.id}/${vars.action}`;
      if (vars.action === "refresh") return api.put(endpoint);
      return api.post(endpoint);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useUploadListingImages() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (vars: { listingId: number; files: File[] }) => {
      if (USE_MOCKS) return mockDelay([] as OlxImage[]);
      const formData = new FormData();
      for (const file of vars.files) formData.append("images", file);
      return api.postForm<OlxImage[]>(
        `/olx/accounts/${accountId}/listings/${vars.listingId}/images`,
        formData
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useDeleteListingImage() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (vars: { listingId: number; imageId: number }) => {
      if (USE_MOCKS) return mockDelay(undefined);
      return api.delete(
        `/olx/accounts/${accountId}/listings/${vars.listingId}/images/${vars.imageId}`
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useSetMainListingImage() {
  const queryClient = useQueryClient();
  const accountId = useResolvedAccountId();

  return useMutation({
    mutationFn: async (vars: { listingId: number; imageId: number }) => {
      if (USE_MOCKS) return mockDelay(undefined);
      return api.post(
        `/olx/accounts/${accountId}/listings/${vars.listingId}/images/${vars.imageId}/main`
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}
