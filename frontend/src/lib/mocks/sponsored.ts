import type { OlxSponsorship, OlxDiscount } from "@/types/olx";

function daysAgo(d: number): string {
  return new Date(Date.now() - d * 24 * 3600 * 1000).toISOString();
}
function daysAhead(d: number): string {
  return new Date(Date.now() + d * 24 * 3600 * 1000).toISOString();
}

export const mockSponsoredListings: OlxSponsorship[] = [
  {
    id: 1,
    listing_id: 5001,
    account_id: 1,
    type: 2,
    days: 14,
    refresh_every: 6,
    locations: ["homepage"],
    started_at: daysAgo(3),
    ends_at: daysAhead(11),
    price_total: 380,
  },
  {
    id: 2,
    listing_id: 5002,
    account_id: 1,
    type: 1,
    days: 7,
    refresh_every: 8,
    locations: ["homepage"],
    started_at: daysAgo(2),
    ends_at: daysAhead(5),
    price_total: 190,
  },
  {
    id: 3,
    listing_id: 5008,
    account_id: 1,
    type: 1,
    days: 3,
    refresh_every: 3,
    locations: ["homepage"],
    started_at: daysAgo(1),
    ends_at: daysAhead(2),
    price_total: 60,
  },
  {
    id: 4,
    listing_id: 5040,
    account_id: 3,
    type: 2,
    days: 30,
    refresh_every: 6,
    locations: ["homepage"],
    started_at: daysAgo(10),
    ends_at: daysAhead(20),
    price_total: 720,
  },
  {
    id: 5,
    listing_id: 5046,
    account_id: 3,
    type: 1,
    days: 7,
    refresh_every: 24,
    locations: ["homepage"],
    started_at: daysAgo(4),
    ends_at: daysAhead(3),
    price_total: 140,
  },
];

export const mockDiscounts: OlxDiscount[] = [
  {
    id: 1,
    listing_id: 5009,
    account_id: 1,
    original_price: 2400,
    discount_price: 2199,
    days: 7,
    started_at: daysAgo(2),
    ends_at: daysAhead(5),
  },
  {
    id: 2,
    listing_id: 5006,
    account_id: 1,
    original_price: 195000,
    discount_price: 185000,
    days: 30,
    started_at: daysAgo(8),
    ends_at: daysAhead(22),
  },
  {
    id: 3,
    listing_id: 5042,
    account_id: 3,
    original_price: 1350,
    discount_price: 1250,
    days: 7,
    started_at: daysAgo(1),
    ends_at: daysAhead(6),
  },
];
