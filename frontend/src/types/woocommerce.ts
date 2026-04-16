export interface WooStore {
  id: number;
  name: string;
  store_url: string;
  created_at: string;
}

export interface WooProduct {
  id: number;
  name: string;
  slug: string;
  sku: string | null;
  status: string;
  price: string;
  regular_price: string;
  sale_price: string;
  currency: string;
  stock_status: string;
  stock_quantity: number | null;
  description: string;
  short_description: string;
  categories: WooCategory[];
  images: WooImage[];
}

export interface WooCategory {
  id: number;
  name: string;
  slug: string;
  parent: number;
  count?: number;
}

export interface WooImage {
  id: number;
  src: string;
  name: string;
  alt: string;
}

export interface WooAttribute {
  id: number;
  name: string;
  slug: string;
  type: string;
  order_by: string;
  has_archives: boolean;
  variation: boolean;
  options?: string[];
  terms?: { id: number; name: string; slug: string }[];
}
