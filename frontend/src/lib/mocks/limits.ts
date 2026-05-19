import type { OlxListingLimits, OlxRefreshLimits } from "@/types/olx";

export const mockListingLimits: Record<number, OlxListingLimits> = {
  1: {
    cars: { used: 5, limit: 10 },
    real_estate: { used: 2, limit: 5 },
    other: { used: 12, limit: 100 },
  },
  2: {
    cars: { used: 0, limit: 10 },
    real_estate: { used: 0, limit: 5 },
    other: { used: 36, limit: 100 },
  },
  3: {
    cars: { used: 0, limit: 10 },
    real_estate: { used: 2, limit: 5 },
    other: { used: 42, limit: 100 },
  },
};

export const mockRefreshLimits: Record<number, OlxRefreshLimits> = {
  1: { free_limit: 10, free_count: 7, paid_count: 2, listing_count: 19 },
  2: { free_limit: 10, free_count: 3, paid_count: 0, listing_count: 6 },
  3: { free_limit: 10, free_count: 9, paid_count: 5, listing_count: 13 },
};
