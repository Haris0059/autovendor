package ba.autovendor.backend.woo.product;

import ba.autovendor.backend.woo.client.dto.WooPluginAttributeDto;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.product.dto.WooAttributeResponse;
import ba.autovendor.backend.woo.product.dto.WooCategoryResponse;
import ba.autovendor.backend.woo.product.dto.WooImageResponse;
import ba.autovendor.backend.woo.product.dto.WooProductResponse;

import java.util.ArrayList;
import java.util.List;

// All lists (including nested ones) are ArrayLists: mapped values end up in the Redis
// cache, whose JSON serializer cannot reconstruct JDK immutable lists.
public final class WooCatalogMapper {

    /** WooCommerce stores don't expose a currency via the plugin; the shops we serve price in KM. */
    private static final String CURRENCY = "KM";

    private WooCatalogMapper() {
    }

    public static WooProductResponse toResponse(WooPluginProductDto product) {
        return new WooProductResponse(
                product.id(),
                product.name(),
                product.slug(),
                product.sku(),
                product.status(),
                product.price(),
                product.regularPrice(),
                product.salePrice(),
                CURRENCY,
                product.stockStatus(),
                product.stockQty(),
                product.description(),
                product.shortDescription(),
                toProductCategories(product.categories()),
                toImages(product.images())
        );
    }

    /** Flattens the plugin's nested category tree; parent = enclosing node id, 0 for roots. */
    public static List<WooCategoryResponse> flattenTree(List<WooPluginCategoryDto> roots) {
        List<WooCategoryResponse> flat = new ArrayList<>();
        addSubtree(roots, 0, flat);
        return flat;
    }

    public static WooAttributeResponse toResponse(WooPluginAttributeDto attribute) {
        List<WooAttributeResponse.Term> terms = new ArrayList<>();
        if (attribute.terms() != null) {
            for (WooPluginAttributeDto.Term term : attribute.terms()) {
                terms.add(new WooAttributeResponse.Term(term.id(), term.name(), term.slug()));
            }
        }
        return new WooAttributeResponse(
                attribute.id(),
                attribute.name(),
                attribute.slug(),
                attribute.type(),
                attribute.orderBy(),
                false,
                false,
                null,
                terms
        );
    }

    private static void addSubtree(List<WooPluginCategoryDto> nodes, long parentId, List<WooCategoryResponse> flat) {
        if (nodes == null) {
            return;
        }
        for (WooPluginCategoryDto node : nodes) {
            flat.add(new WooCategoryResponse(node.id(), node.name(), node.slug(), parentId, null));
            addSubtree(node.children(), node.id(), flat);
        }
    }

    /** Categories embedded in a product carry no hierarchy — parent defaults to 0. */
    private static List<WooCategoryResponse> toProductCategories(List<WooPluginCategoryDto> categories) {
        List<WooCategoryResponse> result = new ArrayList<>();
        if (categories != null) {
            for (WooPluginCategoryDto category : categories) {
                result.add(new WooCategoryResponse(category.id(), category.name(), category.slug(), 0, null));
            }
        }
        return result;
    }

    private static List<WooImageResponse> toImages(List<WooPluginImageDto> images) {
        List<WooImageResponse> result = new ArrayList<>();
        if (images != null) {
            for (WooPluginImageDto image : images) {
                result.add(new WooImageResponse(image.id(), image.src(), image.name(), image.alt()));
            }
        }
        return result;
    }
}
