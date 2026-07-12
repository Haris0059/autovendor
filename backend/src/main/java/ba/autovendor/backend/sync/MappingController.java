package ba.autovendor.backend.sync;

import ba.autovendor.backend.sync.dto.CategoryMappingResponse;
import ba.autovendor.backend.sync.dto.CreateCategoryMappingRequest;
import ba.autovendor.backend.sync.dto.UpdateCategoryMappingRequest;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sync/mappings")
public class MappingController {

    private final MappingService mappingService;

    public MappingController(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public List<CategoryMappingResponse> list(@AuthenticationPrincipal User user) {
        return mappingService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryMappingResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCategoryMappingRequest request
    ) {
        return mappingService.create(user, request);
    }

    @PutMapping("/{id}")
    public CategoryMappingResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryMappingRequest request
    ) {
        return mappingService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        mappingService.delete(user, id);
    }
}
