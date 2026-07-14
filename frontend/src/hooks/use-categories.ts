import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import {
  mockCategories,
  mockCategoryAttributes,
  mockBrands,
  mockModels,
} from "@/lib/mocks/categories";
import type {
  OlxCategory,
  OlxCategoryAttribute,
  OlxCategorySuggestion,
} from "@/types/olx";

const STALE = 10 * 60 * 1000;

function flatten(cats: OlxCategory[]): OlxCategory[] {
  const out: OlxCategory[] = [];
  for (const c of cats) {
    out.push(c);
    if (c.children) out.push(...flatten(c.children));
  }
  return out;
}

export function useOlxCategories(parentId?: number) {
  return useQuery({
    queryKey: ["olx-categories", parentId],
    queryFn: () => {
      if (USE_MOCKS) {
        if (!parentId) return mockDelay(mockCategories);
        const all = flatten(mockCategories);
        const parent = all.find((c) => c.id === parentId);
        return mockDelay(parent?.children ?? []);
      }
      return api.get<OlxCategory[]>(
        parentId ? `/olx/categories/${parentId}` : "/olx/categories"
      );
    },
    staleTime: STALE,
  });
}

/**
 * OLX's own "keyword → category" suggester (public upstream endpoint) —
 * semantic ranking based on where real listings with that keyword live.
 */
export function useCategorySuggestions(keyword: string) {
  const trimmed = keyword.trim();
  return useQuery({
    queryKey: ["olx-categories", "suggest", trimmed],
    queryFn: () => {
      if (USE_MOCKS) {
        const needle = trimmed.toLowerCase();
        const matches = flatten(mockCategories)
          .filter((c) => c.name.toLowerCase().includes(needle))
          .slice(0, 8)
          .map((c) => ({
            id: c.id,
            name: c.name,
            count: 10,
            path: c.parent_id
              ? flatten(mockCategories).find((p) => p.id === c.parent_id)?.name ?? ""
              : "",
          }));
        return mockDelay(matches);
      }
      return api.get<OlxCategorySuggestion[]>("/olx/categories/suggest", {
        params: { keyword: trimmed },
      });
    },
    enabled: trimmed.length >= 2,
    staleTime: STALE,
  });
}

export function useCategoryAttributes(categoryId: number) {
  return useQuery({
    queryKey: ["olx-categories", categoryId, "attributes"],
    queryFn: () => {
      if (USE_MOCKS) {
        return mockDelay(mockCategoryAttributes[categoryId] ?? []);
      }
      return api.get<OlxCategoryAttribute[]>(
        `/olx/categories/${categoryId}/attributes`
      );
    },
    enabled: !!categoryId,
    staleTime: STALE,
  });
}

export function useCategoryBrands(categoryId: number) {
  return useQuery({
    queryKey: ["olx-categories", categoryId, "brands"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay(mockBrands[categoryId] ?? []);
      return api.get<{ id: number; name: string; slug: string }[]>(
        `/olx/categories/${categoryId}/brands`
      );
    },
    enabled: !!categoryId,
    staleTime: STALE,
  });
}

export function useBrandModels(categoryId: number, brandId: number) {
  return useQuery({
    queryKey: ["olx-categories", categoryId, "brands", brandId, "models"],
    queryFn: () => {
      if (USE_MOCKS) return mockDelay(mockModels[brandId] ?? []);
      return api.get<{ id: number; name: string; slug: string }[]>(
        `/olx/categories/${categoryId}/brands/${brandId}/models`
      );
    },
    enabled: !!categoryId && !!brandId,
    staleTime: STALE,
  });
}
