import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { OlxCategory, OlxCategoryAttribute } from "@/types/olx";

export function useOlxCategories(parentId?: number) {
  return useQuery({
    queryKey: ["olx-categories", parentId],
    queryFn: () =>
      api.get<OlxCategory[]>(
        parentId ? `/olx/categories/${parentId}` : "/olx/categories"
      ),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCategoryAttributes(categoryId: number) {
  return useQuery({
    queryKey: ["olx-categories", categoryId, "attributes"],
    queryFn: () =>
      api.get<OlxCategoryAttribute[]>(
        `/olx/categories/${categoryId}/attributes`
      ),
    enabled: !!categoryId,
    staleTime: 5 * 60 * 1000,
  });
}
