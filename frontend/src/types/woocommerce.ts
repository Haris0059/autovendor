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
  status: string;
  price: string;
  regular_price: string;
  sale_price: string;
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
}

export interface WooImage {
  id: number;
  src: string;
  name: string;
  alt: string;
}
