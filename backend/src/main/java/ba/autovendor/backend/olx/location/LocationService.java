package ba.autovendor.backend.olx.location;

import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.dto.OlxCantonDto;
import ba.autovendor.backend.olx.client.dto.OlxCityDto;
import ba.autovendor.backend.olx.client.dto.OlxStateDto;
import ba.autovendor.backend.olx.location.dto.CantonResponse;
import ba.autovendor.backend.olx.location.dto.CityResponse;
import ba.autovendor.backend.olx.location.dto.CountryResponse;
import ba.autovendor.backend.olx.location.dto.StateResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Cached values must be ArrayLists: the Redis JSON serializer cannot reconstruct
// JDK immutable lists (List.of / Stream.toList) from their embedded type hints.
public class LocationService {

    private final OlxApiClient olxApiClient;

    public LocationService(OlxApiClient olxApiClient) {
        this.olxApiClient = olxApiClient;
    }

    @Cacheable(cacheNames = "olx-locations", key = "'countries'")
    public List<CountryResponse> getCountries() {
        return olxApiClient.getCountries().stream()
                .map(c -> new CountryResponse(c.id(), c.name(), c.code()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = "olx-locations", key = "'states'")
    public List<StateResponse> getStates() {
        return olxApiClient.getCountryStates().stream()
                .map(s -> new StateResponse(s.id(), s.name(), s.code()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = "olx-locations", key = "'cantons'")
    public List<CantonResponse> getCantons() {
        List<CantonResponse> cantons = new ArrayList<>();
        for (OlxStateDto state : olxApiClient.getCountryStates()) {
            if (state.cantons() == null) {
                continue;
            }
            for (OlxCantonDto canton : state.cantons()) {
                cantons.add(new CantonResponse(canton.id(), canton.name(), state.id()));
            }
        }
        return cantons;
    }

    @Cacheable(cacheNames = "olx-locations", key = "'cities'")
    public List<CityResponse> getCities() {
        List<CityResponse> cities = new ArrayList<>();
        for (OlxStateDto state : olxApiClient.getCitiesTree()) {
            if (state.cantons() == null) {
                continue;
            }
            for (OlxCantonDto canton : state.cantons()) {
                if (canton.cities() == null) {
                    continue;
                }
                for (OlxCityDto city : canton.cities()) {
                    cities.add(new CityResponse(
                            city.id(),
                            city.name(),
                            null,
                            parseCoordinate(city.location() != null ? city.location().lat() : null),
                            parseCoordinate(city.location() != null ? city.location().lon() : null),
                            city.cantonId() != null ? city.cantonId() : canton.id(),
                            state.id()
                    ));
                }
            }
        }
        return cities;
    }

    private static Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
