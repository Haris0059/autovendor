package ba.autovendor.backend.olx.client.dto;

import java.util.List;

public record OlxListingPageDto(List<OlxListingDto> data, OlxPageMetaDto meta) {

    public record OlxPageMetaDto(Long total, Integer lastPage, Integer currentPage, Integer perPage) {
    }
}
