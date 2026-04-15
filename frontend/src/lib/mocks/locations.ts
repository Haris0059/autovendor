import type { OlxCity } from "@/types/olx";

export interface MockCountry {
  id: number;
  name: string;
  code: string;
}

export interface MockState {
  id: number;
  country_id: number;
  name: string;
}

export interface MockCanton {
  id: number;
  state_id: number;
  name: string;
}

export const mockCountries: MockCountry[] = [
  { id: 1, name: "Bosna i Hercegovina", code: "BA" },
];

export const mockStates: MockState[] = [
  { id: 1, country_id: 1, name: "Federacija BiH" },
  { id: 2, country_id: 1, name: "Republika Srpska" },
  { id: 3, country_id: 1, name: "Brčko Distrikt" },
];

export const mockCantons: MockCanton[] = [
  { id: 1, state_id: 1, name: "Kanton Sarajevo" },
  { id: 2, state_id: 1, name: "Hercegovačko-neretvanski kanton" },
  { id: 3, state_id: 1, name: "Tuzlanski kanton" },
  { id: 4, state_id: 1, name: "Zeničko-dobojski kanton" },
];

export const mockCities: (OlxCity & {
  canton_id: number | null;
  state_id: number;
  country_id: number;
})[] = [
  {
    id: 1,
    name: "Sarajevo",
    zip_code: "71000",
    latitude: 43.8563,
    longitude: 18.4131,
    canton_id: 1,
    state_id: 1,
    country_id: 1,
  },
  {
    id: 2,
    name: "Mostar",
    zip_code: "88000",
    latitude: 43.3438,
    longitude: 17.8078,
    canton_id: 2,
    state_id: 1,
    country_id: 1,
  },
  {
    id: 3,
    name: "Tuzla",
    zip_code: "75000",
    latitude: 44.5381,
    longitude: 18.6731,
    canton_id: 3,
    state_id: 1,
    country_id: 1,
  },
  {
    id: 4,
    name: "Zenica",
    zip_code: "72000",
    latitude: 44.2039,
    longitude: 17.9078,
    canton_id: 4,
    state_id: 1,
    country_id: 1,
  },
  {
    id: 5,
    name: "Banja Luka",
    zip_code: "78000",
    latitude: 44.7722,
    longitude: 17.191,
    canton_id: null,
    state_id: 2,
    country_id: 1,
  },
  {
    id: 6,
    name: "Bihać",
    zip_code: "77000",
    latitude: 44.8188,
    longitude: 15.87,
    canton_id: null,
    state_id: 1,
    country_id: 1,
  },
];
