export const OLX_LISTING_STATUS = {
  ACTIVE: "active",
  INACTIVE: "inactive",
  FINISHED: "finished",
  EXPIRED: "expired",
  HIDDEN: "hidden",
  DRAFT: "draft",
} as const;

export const SYNC_DIRECTION = {
  WOO_TO_OLX: "woo_to_olx",
  OLX_TO_WOO: "olx_to_woo",
  BIDIRECTIONAL: "bidirectional",
} as const;

export const SPONSOR_TYPE = {
  NONE: 0,
  NORMAL: 1,
  PREMIUM: 2,
} as const;
