import type { ProductLink, CategoryMapping, SyncLog } from "@/types/sync";

function daysAgo(days: number, hour = 10): string {
  const d = new Date(Date.now() - days * 24 * 3600 * 1000);
  d.setHours(hour, 0, 0, 0);
  return d.toISOString();
}

export const mockProductLinks: ProductLink[] = [
  {
    id: 1,
    olx_account_id: 1,
    woo_store_id: 1,
    olx_listing_id: 5008,
    woo_product_id: 1001,
    sync_direction: "woo_to_olx",
    last_synced_at: daysAgo(1),
  },
  {
    id: 2,
    olx_account_id: 1,
    woo_store_id: 1,
    olx_listing_id: 5009,
    woo_product_id: 1002,
    sync_direction: "bidirectional",
    last_synced_at: daysAgo(0, 8),
  },
  {
    id: 3,
    olx_account_id: 2,
    woo_store_id: 2,
    olx_listing_id: 5020,
    woo_product_id: 2001,
    sync_direction: "woo_to_olx",
    last_synced_at: daysAgo(2),
  },
  {
    id: 4,
    olx_account_id: 2,
    woo_store_id: 2,
    olx_listing_id: 5021,
    woo_product_id: 2002,
    sync_direction: "woo_to_olx",
    last_synced_at: daysAgo(4),
  },
  {
    id: 5,
    olx_account_id: 2,
    woo_store_id: 2,
    olx_listing_id: 5024,
    woo_product_id: 2003,
    sync_direction: "bidirectional",
    last_synced_at: null,
  },
  {
    id: 6,
    olx_account_id: 3,
    woo_store_id: 1,
    olx_listing_id: 5042,
    woo_product_id: 1003,
    sync_direction: "olx_to_woo",
    last_synced_at: daysAgo(6),
  },
  {
    id: 7,
    olx_account_id: 3,
    woo_store_id: 1,
    olx_listing_id: 5046,
    woo_product_id: 1004,
    sync_direction: "woo_to_olx",
    last_synced_at: daysAgo(0, 14),
  },
];

export const mockCategoryMappings: CategoryMapping[] = [
  {
    id: 1,
    woo_category_id: 10,
    woo_category_name: "Auto dijelovi",
    olx_category_id: 6,
    olx_category_name: "Auto dijelovi",
  },
  {
    id: 2,
    woo_category_id: 11,
    woo_category_name: "Motor",
    olx_category_id: 61,
    olx_category_name: "Motor i mjenjač",
  },
  {
    id: 3,
    woo_category_id: 12,
    woo_category_name: "Karoserija",
    olx_category_id: 62,
    olx_category_name: "Karoserija",
  },
  {
    id: 4,
    woo_category_id: 20,
    woo_category_name: "Mobiteli",
    olx_category_id: 3,
    olx_category_name: "Mobiteli i tableti",
  },
  {
    id: 5,
    woo_category_id: 30,
    woo_category_name: "Bijela tehnika",
    olx_category_id: 41,
    olx_category_name: "Bijela tehnika",
  },
];

const statuses = ["success", "failed", "skipped", "pending"] as const;
const actions = ["create", "update", "price", "stock", "image", "publish"];
const messages: Record<string, string[]> = {
  success: [
    "Ažurirano uspješno.",
    "Cijena sinhronizovana.",
    "Stanje ažurirano.",
    "Listing objavljen na OLX.",
    "Novi proizvod kreiran.",
  ],
  failed: [
    "API timeout na OLX.",
    "Nedostaje kategorija mapping.",
    "Slika previše velika.",
    "Nevažeći token za profil.",
    "Rate limit premašen.",
  ],
  skipped: [
    "Nema promjene od zadnje sinhronizacije.",
    "Ručno isključeno za ovaj artikal.",
  ],
  pending: ["U redu čekanja."],
};

export const mockSyncLogs: SyncLog[] = Array.from({ length: 50 }).map((_, i) => {
  const status = statuses[i % statuses.length];
  const action = actions[i % actions.length];
  const msgs = messages[status];
  return {
    id: 9000 + i,
    product_link_id: (i % 7) + 1,
    action,
    status,
    message: msgs[i % msgs.length],
    created_at: daysAgo(Math.floor(i / 2), (i * 3) % 24),
  };
});
