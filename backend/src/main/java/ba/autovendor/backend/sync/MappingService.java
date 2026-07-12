package ba.autovendor.backend.sync;

import ba.autovendor.backend.sync.dto.CategoryMappingResponse;
import ba.autovendor.backend.sync.dto.CreateCategoryMappingRequest;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MappingService {

    private final CategoryMappingRepository mappingRepository;

    public MappingService(CategoryMappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
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

        CategoryMapping mapping = new CategoryMapping(user.getId(), request.wooCategoryId(),
                request.wooCategoryName(), request.olxCategoryId(), request.olxCategoryName());
        return SyncMapper.toResponse(mappingRepository.save(mapping));
    }

    @Transactional
    public void delete(User user, Long id) {
        CategoryMapping mapping = mappingRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category mapping not found"));
        mappingRepository.delete(mapping);
    }
}
