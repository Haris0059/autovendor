import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import {
  mockSponsoredListings,
  mockDiscounts,
  type MockSponsoredListing,
  type MockDiscount,
} from "@/lib/mocks/sponsored";

let sponsored: MockSponsoredListing[] = [...mockSponsoredListings];
let discounts: MockDiscount[] = [...mockDiscounts];
let nextSponsorId = 100;
let nextDiscountId = 100;

export function useSponsoredListings() {
  return useQuery({
    queryKey: ["sponsored"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...sponsored]);
      return api.get<MockSponsoredListing[]>("/olx/sponsored");
    },
  });
}

export function useDiscounts() {
  return useQuery({
    queryKey: ["discounts"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay([...discounts]);
      return api.get<MockDiscount[]>("/olx/discounts");
    },
  });
}

interface SponsorPriceInput {
  listing_id: number;
  type: 1 | 2;
  days: number;
  refresh_every: number;
  locations: string[];
}

export function useSponsorPrice(input: SponsorPriceInput | null) {
  return useQuery({
    queryKey: ["sponsor-price", input],
    queryFn: async () => {
      if (!input) return null;
      if (USE_MOCKS) {
        const base = input.type === 2 ? 15 : 5;
        const search = base * input.days;
        const refresh =
          input.refresh_every === 0
            ? 0
            : Math.max(1, Math.round((24 / input.refresh_every) * input.days * 2));
        const locations = input.locations.includes("homepage") ? 40 : 0;
        const total = search + refresh + locations;
        await mockDelay(null, 250);
        return { search, refresh, locations, extras: 0, total };
      }
      return api.get<{
        search: number;
        refresh: number;
        locations: number;
        extras: number;
        total: number;
      }>(`/olx/listings/${input.listing_id}/sponsore/price`, {
        params: {
          type: String(input.type),
          days: String(input.days),
          refresh_every: String(input.refresh_every),
          locations: input.locations.join(","),
        },
      });
    },
    enabled: !!input,
    staleTime: 10 * 1000,
  });
}

export function useCreateSponsor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: SponsorPriceInput) => {
      if (USE_MOCKS) {
        const ends = new Date(Date.now() + input.days * 24 * 3600 * 1000).toISOString();
        const price =
          (input.type === 2 ? 15 : 5) * input.days +
          (input.locations.includes("homepage") ? 40 : 0);
        const created: MockSponsoredListing = {
          id: nextSponsorId++,
          listing_id: input.listing_id,
          account_id: 1,
          type: input.type,
          days: input.days,
          refresh_every: input.refresh_every,
          locations: input.locations,
          started_at: new Date().toISOString(),
          ends_at: ends,
          price_total: price,
        };
        sponsored = [created, ...sponsored];
        return mockDelay(created);
      }
      return api.post(`/olx/listings/${input.listing_id}/sponsore`, input);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sponsored"] }),
  });
}

export function useEndSponsor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        sponsored = sponsored.filter((s) => s.id !== id);
        return mockDelay(undefined);
      }
      return api.delete(`/olx/sponsored/${id}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sponsored"] }),
  });
}

interface DiscountInput {
  listing_id: number;
  original_price: number;
  discount_price: number;
  days: 3 | 7 | 30;
}

export function useCreateDiscount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: DiscountInput) => {
      if (USE_MOCKS) {
        const created: MockDiscount = {
          id: nextDiscountId++,
          listing_id: input.listing_id,
          account_id: 1,
          original_price: input.original_price,
          discount_price: input.discount_price,
          days: input.days,
          started_at: new Date().toISOString(),
          ends_at: new Date(Date.now() + input.days * 24 * 3600 * 1000).toISOString(),
        };
        discounts = [created, ...discounts];
        return mockDelay(created);
      }
      return api.post(`/olx/listings/${input.listing_id}/discount`, input);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["discounts"] }),
  });
}

export function useEndDiscount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      if (USE_MOCKS) {
        discounts = discounts.filter((d) => d.id !== id);
        return mockDelay(undefined);
      }
      return api.post(`/olx/discounts/${id}/finish`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["discounts"] }),
  });
}
