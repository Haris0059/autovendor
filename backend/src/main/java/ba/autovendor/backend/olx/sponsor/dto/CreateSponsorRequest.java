package ba.autovendor.backend.olx.sponsor.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSponsorRequest(
        @NotNull Integer type,
        @NotNull Integer days,
        @NotNull Integer refreshEvery,
        List<String> locations
) {
}
