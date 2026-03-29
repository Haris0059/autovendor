export type SyncDirection = "woo_to_olx" | "olx_to_woo" | "bidirectional";
export type SyncStatus = "success" | "failed" | "skipped" | "pending";

export interface ProductLink {
  id: number;
  olx_account_id: number;
  woo_store_id: number;
  olx_listing_id: number;
  woo_product_id: number;
  sync_direction: SyncDirection;
  last_synced_at: string | null;
}

export interface CategoryMapping {
  id: number;
  woo_category_id: number;
  woo_category_name: string;
  olx_category_id: number;
  olx_category_name: string;
}

export interface SyncLog {
  id: number;
  product_link_id: number;
  action: string;
  status: SyncStatus;
  message: string | null;
  created_at: string;
}
