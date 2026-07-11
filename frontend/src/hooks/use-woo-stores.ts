import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import {
  mockWooStores,
  mockWooProducts,
  mockWooCategories,
  mockWooAttributes,
} from "@/lib/mocks/woo";
import type { 
  WooStore, 
  WooProduct, 
  WooCategory, 
  WooAttribute 
} from "@/types/woocommerce";

let mockStoresList: WooStore[] = [...mockWooStores];
let nextStoreId = 1000;

export function useWooStores() {
  return useQuery({
    queryKey: ["woo-stores"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...mockStoresList]);
      return api.get<WooStore[]>("/woo/stores");
    },
  });
}

export function useWooStore(id: number) {
  return useQuery({
    queryKey: ["woo-stores", id],
    queryFn: () => {
      if (USE_MOCKS) {
        const s = mockStoresList.find((s) => s.id === id);
        if (!s) throw new Error("Prodavnica nije pronađena.");
        return mockDelay(s);
      }
      return api.get<WooStore>(`/woo/stores/${id}`);
    },
    enabled: !!id,
  });
}

export function useWooStoreProducts(storeId: number) {
  return useQuery({
    queryKey: ["woo-stores", storeId, "products"],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(
          mockWooProducts.filter((p) => p.store_id === storeId) as WooProduct[]
        );
      }
      return api.get<WooProduct[]>(`/woo/stores/${storeId}/products`);
    },
    enabled: !!storeId,
  });
}

export function useWooStoreCategories(storeId: number) {
  return useQuery({
    queryKey: ["woo-stores", storeId, "categories"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay(mockWooCategories as WooCategory[]);
      return api.get<WooCategory[]>(`/woo/stores/${storeId}/categories`);
    },
    enabled: !!storeId,
  });
}

export function useWooStoreAttributes(storeId: number) {
  return useQuery({
    queryKey: ["woo-stores", storeId, "attributes"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay(mockWooAttributes);
      return api.get<WooAttribute[]>(`/woo/stores/${storeId}/attributes`);
    },
    enabled: !!storeId,
  });
}

export function useCreateWooStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      name: string;
      store_url: string;
      api_key: string;
    }) => {
      if (USE_MOCKS) {
        const created: WooStore = {
          id: nextStoreId++,
          name: data.name,
          store_url: data.store_url,
          created_at: new Date().toISOString(),
        };
        mockStoresList = [created, ...mockStoresList];
        return mockDelay(created);
      }
      return api.post<WooStore>("/woo/stores", data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["woo-stores"] });
    },
  });
}

export function useUpdateWooStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      id: number;
      name?: string;
      store_url?: string;
      api_key?: string;
    }) => {
      if (USE_MOCKS) {
        mockStoresList = mockStoresList.map((s) =>
          s.id === data.id
            ? {
                ...s,
                name: data.name ?? s.name,
                store_url: data.store_url ?? s.store_url,
              }
            : s
        );
        const updated = mockStoresList.find((s) => s.id === data.id)!;
        return mockDelay(updated);
      }
      return api.put<WooStore>(`/woo/stores/${data.id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["woo-stores"] });
    },
  });
}

export function useDeleteWooStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        mockStoresList = mockStoresList.filter((s) => s.id !== id);
        return mockDelay(undefined);
      }
      return api.delete(`/woo/stores/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["woo-stores"] });
    },
  });
}

export function useTestWooConnection() {
  return useMutation({
    mutationFn: async (data: { store_url: string; api_key: string }) => {
      if (USE_MOCKS) {
        await mockDelay(null, 600);
        if (!data.store_url || !data.api_key)
          throw new Error("Nedostaju podaci za test.");
        if (data.api_key.length < 10)
          throw new Error("API ključ je prekratak.");
        return { ok: true, products_count: 24 };
      }
      return api.post<{ ok: boolean; products_count: number }>(
        "/woo/stores/test",
        data
      );
    },
  });
}

export function useTestWooStoreConnection() {
  return useMutation({
    mutationFn: async (storeId: number) => {
      if (USE_MOCKS) {
        await mockDelay(null, 600);
        return { ok: true, products_count: 24 };
      }
      return api.post<{ ok: boolean; products_count: number }>(
        `/woo/stores/${storeId}/test`
      );
    },
  });
}
