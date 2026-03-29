import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { WooStore, WooProduct } from "@/types/woocommerce";

export function useWooStores() {
  return useQuery({
    queryKey: ["woo-stores"],
    queryFn: () => api.get<WooStore[]>("/woo/stores"),
  });
}

export function useWooStoreProducts(storeId: number) {
  return useQuery({
    queryKey: ["woo-stores", storeId, "products"],
    queryFn: () => api.get<WooProduct[]>(`/woo/stores/${storeId}/products`),
    enabled: !!storeId,
  });
}

export function useCreateWooStore() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: {
      name: string;
      store_url: string;
      consumer_key: string;
      consumer_secret: string;
    }) => api.post<WooStore>("/woo/stores", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["woo-stores"] });
    },
  });
}
