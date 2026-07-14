package ba.autovendor.backend.olx.category;

import ba.autovendor.backend.olx.category.dto.AttributeResponse;
import ba.autovendor.backend.olx.category.dto.CategoryResponse;
import ba.autovendor.backend.olx.category.dto.CategorySuggestionResponse;
import ba.autovendor.backend.olx.category.dto.NamedResponse;
import ba.autovendor.backend.olx.client.OlxApiClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Cached values must be ArrayLists: the Redis JSON serializer cannot reconstruct
// JDK immutable lists (List.of / Stream.toList) from their embedded type hints.
public class CategoryService {

    private final OlxApiClient olxApiClient;

    public CategoryService(OlxApiClient olxApiClient) {
        this.olxApiClient = olxApiClient;
    }

    @Cacheable(cacheNames = "olx-categories", key = "'top'")
    public List<CategoryResponse> getTopCategories() {
        return olxApiClient.getCategories().stream().map(CategoryMapper::toResponse).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = "olx-categories", key = "'children:' + #parentId")
    public List<CategoryResponse> getChildren(long parentId) {
        return olxApiClient.getCategoryChildren(parentId).stream().map(CategoryMapper::toResponse).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = "olx-categories", key = "'attributes:' + #categoryId")
    public List<AttributeResponse> getAttributes(long categoryId) {
        return olxApiClient.getCategoryAttributes(categoryId).stream().map(CategoryMapper::toResponse).collect(Collectors.toCollection(ArrayList::new));
    }

    // Deliberately uncached: the keyword space is unbounded (24h TTL entries
    // would pile up in Redis) and the upstream call is a cheap public GET.
    public List<CategorySuggestionResponse> getSuggestions(String keyword) {
        return olxApiClient.getCategorySuggestions(keyword).stream().map(CategoryMapper::toResponse).toList();
    }

    @Cacheable(cacheNames = "olx-categories", key = "'brands:' + #categoryId")
    public List<NamedResponse> getBrands(long categoryId) {
        return olxApiClient.getCategoryBrands(categoryId).stream().map(CategoryMapper::toResponse).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = "olx-categories", key = "'models:' + #categoryId + ':' + #brandId")
    public List<NamedResponse> getModels(long categoryId, long brandId) {
        return olxApiClient.getBrandModels(categoryId, brandId).stream().map(CategoryMapper::toResponse).collect(Collectors.toCollection(ArrayList::new));
    }
}
