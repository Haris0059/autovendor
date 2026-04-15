import type { WooStore, WooProduct, WooCategory } from "@/types/woocommerce";

export const mockWooStores: WooStore[] = [
  {
    id: 1,
    name: "Moj Shop BA",
    store_url: "https://mojshop.ba",
    created_at: "2025-09-12T10:30:00.000Z",
  },
  {
    id: 2,
    name: "Autodijelovi Online",
    store_url: "https://autodijelovi-online.com",
    created_at: "2025-11-04T14:12:00.000Z",
  },
];

export const mockWooCategories: WooCategory[] = [
  { id: 10, name: "Auto dijelovi", slug: "auto-dijelovi", parent: 0 },
  { id: 11, name: "Motor", slug: "motor", parent: 10 },
  { id: 12, name: "Karoserija", slug: "karoserija", parent: 10 },
  { id: 20, name: "Mobiteli", slug: "mobiteli", parent: 0 },
  { id: 21, name: "Android", slug: "android", parent: 20 },
  { id: 22, name: "iPhone", slug: "iphone", parent: 20 },
  { id: 30, name: "Bijela tehnika", slug: "bijela-tehnika", parent: 0 },
];

interface MockWooProduct extends WooProduct {
  store_id: number;
  sku: string;
  stock_status: "instock" | "outofstock";
  stock_qty: number | null;
}

export const mockWooProducts: MockWooProduct[] = [
  {
    id: 1001,
    store_id: 1,
    name: "iPhone 13 Pro 256GB - refurbished",
    slug: "iphone-13-pro-256",
    status: "publish",
    price: "1499",
    regular_price: "1599",
    sale_price: "1499",
    description: "Refurbished, pun garancijski period.",
    short_description: "Refurbished iPhone 13 Pro 256GB",
    categories: [mockWooCategories[3], mockWooCategories[5]],
    images: [
      { id: 90001, src: "https://picsum.photos/seed/woo-iphone13/400/300", name: "iphone13", alt: "iphone" },
    ],
    sku: "IPH13P-256-REF",
    stock_status: "instock",
    stock_qty: 3,
  },
  {
    id: 1002,
    store_id: 1,
    name: "Samsung Galaxy S24 Ultra",
    slug: "galaxy-s24-ultra",
    status: "publish",
    price: "2399",
    regular_price: "2399",
    sale_price: "",
    description: "Potpuno nov, zapakovan.",
    short_description: "Samsung flagship 2024",
    categories: [mockWooCategories[3], mockWooCategories[4]],
    images: [
      { id: 90002, src: "https://picsum.photos/seed/woo-s24/400/300", name: "s24", alt: "s24" },
    ],
    sku: "SAMS-S24U",
    stock_status: "instock",
    stock_qty: 8,
  },
  {
    id: 1003,
    store_id: 1,
    name: "Apple iPad Air 5 256GB",
    slug: "ipad-air-5",
    status: "publish",
    price: "1299",
    regular_price: "1299",
    sale_price: "",
    description: "Apple iPad Air 5. generacija.",
    short_description: "iPad Air 5",
    categories: [mockWooCategories[3]],
    images: [
      { id: 90003, src: "https://picsum.photos/seed/woo-ipad/400/300", name: "ipad", alt: "ipad" },
    ],
    sku: "APL-IPAIR5-256",
    stock_status: "outofstock",
    stock_qty: 0,
  },
  {
    id: 1004,
    store_id: 1,
    name: "Samsung QLED TV 55\"",
    slug: "samsung-qled-55",
    status: "publish",
    price: "1450",
    regular_price: "1550",
    sale_price: "1450",
    description: "Samsung QLED 4K 2024.",
    short_description: "55-inčni QLED TV",
    categories: [mockWooCategories[6]],
    images: [
      { id: 90004, src: "https://picsum.photos/seed/woo-tv/400/300", name: "tv", alt: "tv" },
    ],
    sku: "SAMS-TV55Q",
    stock_status: "instock",
    stock_qty: 5,
  },
  {
    id: 2001,
    store_id: 2,
    name: "Alternator Bosch 0124525055",
    slug: "alternator-bosch",
    status: "publish",
    price: "195",
    regular_price: "195",
    sale_price: "",
    description: "Alternator Bosch za VAG grupu 2.0 TDI.",
    short_description: "Alternator 2.0 TDI",
    categories: [mockWooCategories[0], mockWooCategories[1]],
    images: [
      { id: 90010, src: "https://picsum.photos/seed/woo-alt/400/300", name: "alt", alt: "alt" },
    ],
    sku: "ALT-BOSCH-525",
    stock_status: "instock",
    stock_qty: 12,
  },
  {
    id: 2002,
    store_id: 2,
    name: "Retrovizor BMW E60 desni",
    slug: "retrovizor-e60",
    status: "publish",
    price: "135",
    regular_price: "135",
    sale_price: "",
    description: "Desni retrovizor za BMW E60.",
    short_description: "Retrovizor E60",
    categories: [mockWooCategories[0], mockWooCategories[2]],
    images: [
      { id: 90011, src: "https://picsum.photos/seed/woo-retro/400/300", name: "retro", alt: "retro" },
    ],
    sku: "RETR-E60-D",
    stock_status: "instock",
    stock_qty: 2,
  },
  {
    id: 2003,
    store_id: 2,
    name: "Turbina Garrett 1.9 TDI",
    slug: "turbina-1-9-tdi",
    status: "publish",
    price: "340",
    regular_price: "340",
    sale_price: "",
    description: "Turbina za 1.9 TDI, regenerisana.",
    short_description: "Turbina 1.9 TDI",
    categories: [mockWooCategories[0], mockWooCategories[1]],
    images: [
      { id: 90012, src: "https://picsum.photos/seed/woo-turbo/400/300", name: "turbo", alt: "turbo" },
    ],
    sku: "TURB-19-TDI",
    stock_status: "outofstock",
    stock_qty: 0,
  },
];

export interface MockWooAttribute {
  id: number;
  name: string;
  slug: string;
  terms: { id: number; name: string; slug: string }[];
}

export const mockWooAttributes: MockWooAttribute[] = [
  {
    id: 1,
    name: "Boja",
    slug: "boja",
    terms: [
      { id: 11, name: "Crna", slug: "crna" },
      { id: 12, name: "Bijela", slug: "bijela" },
      { id: 13, name: "Plava", slug: "plava" },
    ],
  },
  {
    id: 2,
    name: "Veličina",
    slug: "velicina",
    terms: [
      { id: 21, name: "S", slug: "s" },
      { id: 22, name: "M", slug: "m" },
      { id: 23, name: "L", slug: "l" },
      { id: 24, name: "XL", slug: "xl" },
    ],
  },
];
