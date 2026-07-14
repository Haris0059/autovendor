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

export interface OlxCategorySuggestion {
  id: number;
  name: string;
  count: number;
  path: string;
}

export interface OlxCategoryAttribute {
  id: number;
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

export interface OlxSponsorship {
  id: number;
  listing_id: number;
  account_id: number;
  type: 1 | 2;
  days: number;
  refresh_every: number;
  locations: string[];
  started_at: string;
  ends_at: string;
  price_total: number;
}

export interface OlxDiscount {
  id: number;
  listing_id: number;
  account_id: number;
  original_price: number;
  discount_price: number;
  days: 3 | 7 | 30;
  started_at: string;
  ends_at: string;
}

export interface OlxSponsorPrice {
  search: number;
  refresh: number;
  locations: number;
  extras: number;
  total: number;
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
