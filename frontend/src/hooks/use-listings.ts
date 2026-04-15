import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay, paginate } from "@/lib/mocks";
import { mockListings } from "@/lib/mocks/listings";
import type { OlxListing } from "@/types/olx";
import type { PaginatedResponse } from "@/types/api";

interface ListingFilters {
  account_id: number;
  status?: string;
  page?: number;
  per_page?: number;
}

const mockListingsStore: (OlxListing & { account_id: number })[] = [...mockListings];

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
      return api.get<PaginatedResponse<OlxListing>>("/olx/listings", {
        params: Object.fromEntries(
          Object.entries(filters)
            .filter(([, v]) => v !== undefined)
            .map(([k, v]) => [k, String(v)])
        ),
      });
    },
    enabled: !!filters.account_id,
  });
}

export function useListing(id: number) {
  return useQuery({
    queryKey: ["listings", "single", id],
    queryFn: () => {
      if (USE_MOCKS) {
        const found = mockListingsStore.find((l) => l.id === id);
        if (!found) throw new Error("Artikal nije pronađen.");
        return mockDelay(found);
      }
      return api.get<OlxListing>(`/olx/listings/${id}`);
    },
    enabled: !!id,
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
        const created: OlxListing & { account_id: number } = {
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
      return api.post<OlxListing>("/olx/listings", data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useUpdateListing() {
  const queryClient = useQueryClient();

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
        } as OlxListing & { account_id: number };
        mockListingsStore[idx] = merged;
        return mockDelay(merged);
      }
      return api.put<OlxListing>(`/olx/listings/${data.id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}

export function useDeleteListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        const idx = mockListingsStore.findIndex((l) => l.id === id);
        if (idx !== -1) mockListingsStore.splice(idx, 1);
        return mockDelay(undefined);
      }
      return api.delete(`/olx/listings/${id}`);
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
      const endpoint = `/olx/listings/${vars.id}/${vars.action}`;
      if (vars.action === "refresh") return api.put(endpoint);
      return api.post(endpoint);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}
