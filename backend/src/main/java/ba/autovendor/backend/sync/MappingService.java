package ba.autovendor.backend.sync;

import ba.autovendor.backend.olx.category.CategoryService;
import ba.autovendor.backend.olx.category.dto.AttributeResponse;
import ba.autovendor.backend.sync.dto.CategoryMappingResponse;
import ba.autovendor.backend.sync.dto.CreateCategoryMappingRequest;
import ba.autovendor.backend.sync.dto.UpdateCategoryMappingRequest;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MappingService {

    private final CategoryMappingRepository mappingRepository;
    private final CategoryService categoryService;

    public MappingService(CategoryMappingRepository mappingRepository, CategoryService categoryService) {
        this.mappingRepository = mappingRepository;
        this.categoryService = categoryService;
    }

    public List<CategoryMappingResponse> list(User user) {
        return mappingRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SyncMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryMappingResponse create(User user, CreateCategoryMappingRequest request) {
        if (mappingRepository.existsByUserIdAndWooCategoryId(user.getId(), request.wooCategoryId())) {
            throw new IllegalArgumentException("Category already mapped");
        }
        validateDefaults(request.olxCategoryId(), request.attributeDefaults());

        CategoryMapping mapping = new CategoryMapping(user.getId(), request.wooCategoryId(),
                request.wooCategoryName(), request.olxCategoryId(), request.olxCategoryName());
        mapping.setAttributeDefaults(request.attributeDefaults());
        return SyncMapper.toResponse(mappingRepository.save(mapping));
    }

    @Transactional
    public CategoryMappingResponse update(User user, Long id, UpdateCategoryMappingRequest request) {
        CategoryMapping mapping = mappingRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category mapping not found"));
        validateDefaults(request.olxCategoryId(), request.attributeDefaults());

        mapping.updateOlxTarget(request.olxCategoryId(), request.olxCategoryName());
        mapping.setAttributeDefaults(request.attributeDefaults());
        return SyncMapper.toResponse(mappingRepository.save(mapping));
    }

    @Transactional
    public void delete(User user, Long id) {
        CategoryMapping mapping = mappingRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category mapping not found"));
        mappingRepository.delete(mapping);
    }

    /**
     * Every required OLX attribute must have a default (guarantees syncs into the
     * category pass OLX validation — decided with the user), and every provided
     * default must name a real attribute with a value from its options.
     */
    private void validateDefaults(Long olxCategoryId, Map<String, String> defaults) {
        List<AttributeResponse> attributes = categoryService.getAttributes(olxCategoryId);

        for (AttributeResponse attribute : attributes) {
            if (Boolean.TRUE.equals(attribute.required())) {
                String value = defaults != null ? defaults.get(attribute.name()) : null;
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException(
                            "Required OLX attribute '" + attribute.displayName() + "' needs a default value");
                }
            }
        }
        if (defaults == null) {
            return;
        }
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            AttributeResponse attribute = attributes.stream()
                    .filter(a -> a.name().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown OLX attribute '" + entry.getKey() + "' for this category"));
            if (attribute.options() != null && !attribute.options().isEmpty()
                    && !attribute.options().contains(entry.getValue())) {
                throw new IllegalArgumentException("Value '" + entry.getValue()
                        + "' is not a valid option for OLX attribute '" + attribute.displayName() + "'");
            }
        }
    }
}
