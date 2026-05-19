import type { OlxAccount } from "@/types/olx";

export const mockOlxAccounts: OlxAccount[] = [
  {
    id: 1,
    username: "shop_sarajevo",
    olx_user_id: 101234,
    default_city_id: 1,
    token_expires_at: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString(),
    created_at: "2025-11-02T09:14:00.000Z",
  },
  {
    id: 2,
    username: "autodijelovi_tz",
    olx_user_id: 102456,
    default_city_id: 3,
    token_expires_at: new Date(Date.now() + 2 * 24 * 3600 * 1000).toISOString(),
    created_at: "2025-12-18T14:31:00.000Z",
  },
  {
    id: 3,
    username: "mobilni_mostar",
    olx_user_id: 103789,
    default_city_id: 2,
    token_expires_at: new Date(Date.now() - 3 * 24 * 3600 * 1000).toISOString(),
    created_at: "2026-01-22T08:47:00.000Z",
  },
];
