import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import {
  mockCountries,
  mockStates,
  mockCantons,
  mockCities,
  type MockCountry,
  type MockState,
  type MockCanton,
} from "@/lib/mocks/locations";
import type { OlxCity } from "@/types/olx";

const STALE = 30 * 60 * 1000;

export function useCountries() {
  return useQuery({
    queryKey: ["locations", "countries"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay(mockCountries);
      return api.get<MockCountry[]>("/locations/countries");
    },
    staleTime: STALE,
  });
}

export function useStates(countryId?: number) {
  return useQuery({
    queryKey: ["locations", "states", countryId],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(
          countryId ? mockStates.filter((s) => s.country_id === countryId) : mockStates
        );
      }
      return api.get<MockState[]>("/locations/states");
    },
    staleTime: STALE,
  });
}

export function useCantons(stateId?: number) {
  return useQuery({
    queryKey: ["locations", "cantons", stateId],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(
          stateId ? mockCantons.filter((c) => c.state_id === stateId) : mockCantons
        );
      }
      return api.get<MockCanton[]>("/locations/cantons");
    },
    staleTime: STALE,
  });
}

export function useCities(opts?: { cantonId?: number; stateId?: number }) {
  return useQuery({
    queryKey: ["locations", "cities", opts],
    queryFn: () => {
      if (USE_MOCKS) {
        let list = mockCities;
        if (opts?.cantonId) list = list.filter((c) => c.canton_id === opts.cantonId);
        if (opts?.stateId) list = list.filter((c) => c.state_id === opts.stateId);
        return mockDelay(list as OlxCity[]);
      }
      return api.get<OlxCity[]>("/locations/cities");
    },
    staleTime: STALE,
  });
}
