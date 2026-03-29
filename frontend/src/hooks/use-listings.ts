import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { OlxListing } from "@/types/olx";
import type { PaginatedResponse } from "@/types/api";

interface ListingFilters {
  account_id: number;
  status?: string;
  page?: number;
}

export function useListings(filters: ListingFilters) {
  return useQuery({
    queryKey: ["listings", filters],
    queryFn: () =>
      api.get<PaginatedResponse<OlxListing>>("/olx/listings", {
        params: Object.fromEntries(
          Object.entries(filters)
            .filter(([, v]) => v !== undefined)
            .map(([k, v]) => [k, String(v)])
        ),
      }),
    enabled: !!filters.account_id,
  });
}

export function useCreateListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { account_id: number; title: string; [key: string]: unknown }) =>
      api.post<OlxListing>("/olx/listings", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listings"] });
    },
  });
}
