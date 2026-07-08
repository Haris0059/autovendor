package ba.autovendor.backend.olx.category;

import ba.autovendor.backend.olx.category.dto.AttributeResponse;
import ba.autovendor.backend.olx.category.dto.CategoryResponse;
import ba.autovendor.backend.olx.category.dto.NamedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/olx/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> topCategories() {
        return categoryService.getTopCategories();
    }

    @GetMapping("/{parentId}")
    public List<CategoryResponse> children(@PathVariable long parentId) {
        return categoryService.getChildren(parentId);
    }

    @GetMapping("/{categoryId}/attributes")
    public List<AttributeResponse> attributes(@PathVariable long categoryId) {
        return categoryService.getAttributes(categoryId);
    }

    @GetMapping("/{categoryId}/brands")
    public List<NamedResponse> brands(@PathVariable long categoryId) {
        return categoryService.getBrands(categoryId);
    }

    @GetMapping("/{categoryId}/brands/{brandId}/models")
    public List<NamedResponse> models(@PathVariable long categoryId, @PathVariable long brandId) {
        return categoryService.getModels(categoryId, brandId);
    }
}
