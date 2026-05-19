export interface OlxAccount {
  id: number;
  username: string;
  olx_user_id: number | null;
  default_city_id: number | null;
  token_expires_at: string | null;
  created_at: string;
}

export interface OlxListing {
  id: number;
  account_id: number;
  title: string;
  description: string | null;
  price: number | null;
  city_id: number | null;
  category_id: number | null;
  listing_type: string;
  state: string | null;
  status: string;
  images: OlxImage[];
  created_at: string;
  updated_at: string;
}

export interface OlxImage {
  id: number;
  url: string;
  is_main: boolean;
}

export interface OlxCategory {
  id: number;
  name: string;
  slug: string;
  parent_id: number | null;
  children?: OlxCategory[];
}

export interface OlxCategoryAttribute {
  type: string;
  name: string;
  input_type: string;
  display_name: string;
  options: string[] | null;
  required: boolean;
}

export interface OlxCity {
  id: number;
  name: string;
  zip_code: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface OlxListingLimits {
  cars: { used: number; limit: number };
  real_estate: { used: number; limit: number };
  other: { used: number; limit: number };
}

export interface OlxRefreshLimits {
  free_limit: number;
  free_count: number;
  paid_count: number;
  listing_count: number;
}
