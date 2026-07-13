import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import { mockListingLimits, mockRefreshLimits } from "@/lib/mocks/limits";
import type { OlxListingLimits, OlxRefreshLimits } from "@/types/olx";

export function useListingLimits(accountId: number) {
  return useQuery({
    queryKey: ["listing-limits", accountId],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(
          mockListingLimits[accountId] ?? {
            cars: { used: 0, limit: 10 },
            real_estate: { used: 0, limit: 5 },
            other: { used: 0, limit: 100 },
          }
        );
      }
      return api.get<OlxListingLimits>(`/olx/accounts/${accountId}/listing-limits`);
    },
    enabled: !!accountId,
  });
}

export function useRefreshLimits(accountId: number) {
  return useQuery({
    queryKey: ["refresh-limits", accountId],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(
          mockRefreshLimits[accountId] ?? {
            free_limit: 10,
            free_count: 0,
            paid_count: 0,
            listing_count: 0,
          }
        );
      }
      return api.get<OlxRefreshLimits>(`/olx/accounts/${accountId}/listing/refresh/limits`);
    },
    enabled: !!accountId,
  });
}
