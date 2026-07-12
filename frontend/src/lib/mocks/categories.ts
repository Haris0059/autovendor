import type { OlxCategory, OlxCategoryAttribute } from "@/types/olx";

export const mockCategories: OlxCategory[] = [
  {
    id: 1,
    name: "Automobili",
    slug: "automobili",
    parent_id: null,
    children: [
      { id: 11, name: "Putnički automobili", slug: "putnicki", parent_id: 1 },
      { id: 12, name: "Kombi i dostavna", slug: "kombi", parent_id: 1 },
      { id: 13, name: "Motocikli", slug: "motocikli", parent_id: 1 },
    ],
  },
  {
    id: 2,
    name: "Nekretnine",
    slug: "nekretnine",
    parent_id: null,
    children: [
      { id: 21, name: "Stanovi", slug: "stanovi", parent_id: 2 },
      { id: 22, name: "Kuće", slug: "kuce", parent_id: 2 },
      { id: 23, name: "Poslovni prostori", slug: "poslovni", parent_id: 2 },
    ],
  },
  {
    id: 3,
    name: "Mobiteli i tableti",
    slug: "mobiteli",
    parent_id: null,
    children: [
      { id: 31, name: "Mobiteli", slug: "mobiteli-mobiteli", parent_id: 3 },
      { id: 32, name: "Tableti", slug: "tableti", parent_id: 3 },
      { id: 33, name: "Dodaci", slug: "mobiteli-dodaci", parent_id: 3 },
    ],
  },
  {
    id: 4,
    name: "Kućni aparati",
    slug: "kucni-aparati",
    parent_id: null,
    children: [
      { id: 41, name: "Bijela tehnika", slug: "bijela-tehnika", parent_id: 4 },
      { id: 42, name: "Mali aparati", slug: "mali-aparati", parent_id: 4 },
    ],
  },
  {
    id: 5,
    name: "Odjeća i obuća",
    slug: "odjeca",
    parent_id: null,
    children: [
      { id: 51, name: "Muška odjeća", slug: "muska", parent_id: 5 },
      { id: 52, name: "Ženska odjeća", slug: "zenska", parent_id: 5 },
    ],
  },
  {
    id: 6,
    name: "Auto dijelovi",
    slug: "auto-dijelovi",
    parent_id: null,
    children: [
      { id: 61, name: "Motor i mjenjač", slug: "motor", parent_id: 6 },
      { id: 62, name: "Karoserija", slug: "karoserija", parent_id: 6 },
      { id: 63, name: "Elektronika", slug: "auto-elektronika", parent_id: 6 },
    ],
  },
];

export const mockCategoryAttributes: Record<number, OlxCategoryAttribute[]> = {
  11: [
    {
      id: 9001,
      type: "year",
      name: "godiste",
      input_type: "number",
      display_name: "Godište",
      options: null,
      required: true,
    },
    {
      id: 9002,
      type: "km",
      name: "kilometraza",
      input_type: "number",
      display_name: "Kilometraža",
      options: null,
      required: true,
    },
    {
      id: 9003,
      type: "fuel",
      name: "gorivo",
      input_type: "select",
      display_name: "Gorivo",
      options: ["Benzin", "Dizel", "Hibrid", "Električni", "Plin"],
      required: true,
    },
    {
      id: 9004,
      type: "transmission",
      name: "mjenjac",
      input_type: "select",
      display_name: "Mjenjač",
      options: ["Manuelni", "Automatski", "Poluautomatski"],
      required: true,
    },
  ],
  21: [
    {
      id: 9005,
      type: "area",
      name: "kvadratura",
      input_type: "number",
      display_name: "Kvadratura (m²)",
      options: null,
      required: true,
    },
    {
      id: 9006,
      type: "rooms",
      name: "broj_soba",
      input_type: "select",
      display_name: "Broj soba",
      options: ["Garsonjera", "1", "1.5", "2", "2.5", "3", "3.5", "4+"],
      required: true,
    },
  ],
  31: [
    {
      id: 9007,
      type: "brand",
      name: "proizvodjac",
      input_type: "select",
      display_name: "Proizvođač",
      options: ["Apple", "Samsung", "Xiaomi", "Huawei", "OnePlus"],
      required: true,
    },
  ],
};

export const mockBrands: Record<number, { id: number; name: string; slug: string }[]> = {
  11: [
    { id: 1001, name: "Audi", slug: "audi" },
    { id: 1002, name: "BMW", slug: "bmw" },
    { id: 1003, name: "Volkswagen", slug: "vw" },
    { id: 1004, name: "Mercedes-Benz", slug: "mercedes" },
    { id: 1005, name: "Škoda", slug: "skoda" },
  ],
};

export const mockModels: Record<number, { id: number; name: string; slug: string }[]> = {
  1001: [
    { id: 20001, name: "A3", slug: "a3" },
    { id: 20002, name: "A4", slug: "a4" },
    { id: 20003, name: "A6", slug: "a6" },
    { id: 20004, name: "Q5", slug: "q5" },
    { id: 20005, name: "Q7", slug: "q7" },
  ],
  1002: [
    { id: 20101, name: "Serija 1", slug: "serija-1" },
    { id: 20102, name: "Serija 3", slug: "serija-3" },
    { id: 20103, name: "Serija 5", slug: "serija-5" },
    { id: 20104, name: "X3", slug: "x3" },
    { id: 20105, name: "X5", slug: "x5" },
  ],
  1003: [
    { id: 20201, name: "Golf", slug: "golf" },
    { id: 20202, name: "Passat", slug: "passat" },
    { id: 20203, name: "Tiguan", slug: "tiguan" },
    { id: 20204, name: "Polo", slug: "polo" },
  ],
  1004: [
    { id: 20301, name: "C-klasa", slug: "c-klasa" },
    { id: 20302, name: "E-klasa", slug: "e-klasa" },
    { id: 20303, name: "GLC", slug: "glc" },
  ],
  1005: [
    { id: 20401, name: "Octavia", slug: "octavia" },
    { id: 20402, name: "Superb", slug: "superb" },
    { id: 20403, name: "Kodiaq", slug: "kodiaq" },
  ],
};
