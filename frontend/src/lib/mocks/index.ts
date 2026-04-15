export const USE_MOCKS = process.env.NEXT_PUBLIC_USE_MOCKS === "true";

export async function mockDelay<T>(value: T, ms = 250): Promise<T> {
  await new Promise((r) => setTimeout(r, ms));
  return value;
}

export function paginate<T>(
  items: T[],
  page = 1,
  perPage = 10
): {
  data: T[];
  total: number;
  page: number;
  per_page: number;
  last_page: number;
} {
  const total = items.length;
  const last_page = Math.max(1, Math.ceil(total / perPage));
  const safePage = Math.min(Math.max(1, page), last_page);
  const start = (safePage - 1) * perPage;
  const data = items.slice(start, start + perPage);
  return { data, total, page: safePage, per_page: perPage, last_page };
}
