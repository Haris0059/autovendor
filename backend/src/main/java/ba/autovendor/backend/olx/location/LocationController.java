package ba.autovendor.backend.olx.location;

import ba.autovendor.backend.olx.location.dto.CantonResponse;
import ba.autovendor.backend.olx.location.dto.CityResponse;
import ba.autovendor.backend.olx.location.dto.CountryResponse;
import ba.autovendor.backend.olx.location.dto.StateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/countries")
    public List<CountryResponse> countries() {
        return locationService.getCountries();
    }

    @GetMapping("/states")
    public List<StateResponse> states() {
        return locationService.getStates();
    }

    @GetMapping("/cantons")
    public List<CantonResponse> cantons() {
        return locationService.getCantons();
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return locationService.getCities();
    }
}
