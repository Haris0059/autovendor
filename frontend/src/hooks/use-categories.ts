import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import {
  mockCategories,
  mockCategoryAttributes,
  mockBrands,
  mockModels,
} from "@/lib/mocks/categories";
import type { OlxCategory, OlxCategoryAttribute } from "@/types/olx";

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
