package ba.autovendor.backend.olx;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.olx.client.dto.OlxCantonDto;
import ba.autovendor.backend.olx.client.dto.OlxCategoryDto;
import ba.autovendor.backend.olx.client.dto.OlxCityDto;
import ba.autovendor.backend.olx.client.dto.OlxCountryDto;
import ba.autovendor.backend.olx.client.dto.OlxLocationDto;
import ba.autovendor.backend.olx.client.dto.OlxNamedDto;
import ba.autovendor.backend.olx.client.dto.OlxStateDto;
import ba.autovendor.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OlxCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private String jwt;

    @BeforeEach
    void setUp() throws Exception {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
        userRepository.deleteAll();
        jwt = registerUser();
    }

    @Test
    void topCategoriesAreMappedToFrontendShape() throws Exception {
        when(olxApiClient.getCategories())
                .thenReturn(List.of(new OlxCategoryDto(1L, "Vozila", "vozila", null)));

        mockMvc.perform(get("/olx/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Vozila"))
                .andExpect(jsonPath("$[0].slug").value("vozila"))
                .andExpect(jsonPath("$[0].parent_id").isEmpty());
    }

    @Test
    void childCategoriesAreReturnedForParent() throws Exception {
        when(olxApiClient.getCategoryChildren(1L))
                .thenReturn(List.of(new OlxCategoryDto(18L, "Automobili", "automobili", 1L)));

        mockMvc.perform(get("/olx/categories/1").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(18))
                .andExpect(jsonPath("$[0].parent_id").value(1));
    }

    @Test
    void attributesAreMappedWithSnakeCaseKeys() throws Exception {
        when(olxApiClient.getCategoryAttributes(18L)).thenReturn(List.of(
                new OlxAttributeDto(7L, "string", "gorivo", "select", "Gorivo",
                        List.of("Dizel", "Benzin"), true)));

        mockMvc.perform(get("/olx/categories/18/attributes").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("string"))
                .andExpect(jsonPath("$[0].name").value("gorivo"))
                .andExpect(jsonPath("$[0].input_type").value("select"))
                .andExpect(jsonPath("$[0].display_name").value("Gorivo"))
                .andExpect(jsonPath("$[0].options", hasSize(2)))
                .andExpect(jsonPath("$[0].required").value(true));
    }

    @Test
    void brandsAndModelsAreProxied() throws Exception {
        when(olxApiClient.getCategoryBrands(18L))
                .thenReturn(List.of(new OlxNamedDto(1L, "BMW", "bmw")));
        when(olxApiClient.getBrandModels(18L, 1L))
                .thenReturn(List.of(new OlxNamedDto(10L, "Series 3", "series-3")));

        mockMvc.perform(get("/olx/categories/18/brands").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("BMW"));

        mockMvc.perform(get("/olx/categories/18/brands/1/models").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("series-3"));
    }

    @Test
    void countriesArePassedThrough() throws Exception {
        when(olxApiClient.getCountries())
                .thenReturn(List.of(new OlxCountryDto(49L, "Bosna i Hercegovina", "BA")));

        mockMvc.perform(get("/locations/countries").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(49))
                .andExpect(jsonPath("$[0].code").value("BA"));
    }

    @Test
    void statesAndCantonsComeFromCountryStates() throws Exception {
        when(olxApiClient.getCountryStates()).thenReturn(statesFixture());

        mockMvc.perform(get("/locations/states").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Federacija BiH"))
                .andExpect(jsonPath("$[0].code").value("FBiH"));

        mockMvc.perform(get("/locations/cantons").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].state_id").value(1))
                .andExpect(jsonPath("$[1].id").value(16))
                .andExpect(jsonPath("$[1].state_id").value(2));
    }

    @Test
    void citiesAreFlattenedFromTree() throws Exception {
        when(olxApiClient.getCitiesTree()).thenReturn(List.of(
                new OlxStateDto(1L, "Federacija BiH", "FBiH", List.of(
                        new OlxCantonDto(1L, "Unsko-sanski kanton", List.of(
                                new OlxCityDto(3L, "Bihać", new OlxLocationDto("44.8146563", "15.86896"), 1L))))),
                new OlxStateDto(2L, "Republika Srpska", "RS", List.of(
                        new OlxCantonDto(16L, "Banjalučka regija", List.of(
                                new OlxCityDto(66L, "Kostajnica", new OlxLocationDto("45.21", "16.54"), null))),
                        new OlxCantonDto(17L, "Prazna regija", null)))));

        mockMvc.perform(get("/locations/cities").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Bihać"))
                .andExpect(jsonPath("$[0].zip_code").isEmpty())
                .andExpect(jsonPath("$[0].latitude").value(44.8146563))
                .andExpect(jsonPath("$[0].longitude").value(15.86896))
                .andExpect(jsonPath("$[0].canton_id").value(1))
                .andExpect(jsonPath("$[0].state_id").value(1))
                .andExpect(jsonPath("$[1].canton_id").value(16))
                .andExpect(jsonPath("$[1].state_id").value(2));
    }

    @Test
    void categoriesAreCachedInRedis() throws Exception {
        when(olxApiClient.getCategories())
                .thenReturn(List.of(new OlxCategoryDto(1L, "Vozila", "vozila", null)));

        mockMvc.perform(get("/olx/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/olx/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vozila"));

        verify(olxApiClient, times(1)).getCategories();
    }

    @Test
    void locationsAreCachedInRedis() throws Exception {
        when(olxApiClient.getCountryStates()).thenReturn(statesFixture());

        mockMvc.perform(get("/locations/cantons").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/locations/cantons").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        verify(olxApiClient, times(1)).getCountryStates();
    }

    @Test
    void upstreamFailureReturns502() throws Exception {
        when(olxApiClient.getCategories())
                .thenThrow(new OlxApiException("OLX request /categories failed with status 500"));

        mockMvc.perform(get("/olx/categories").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("OLX request /categories failed with status 500"));
    }

    @Test
    void catalogEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/olx/categories"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/locations/cities"))
                .andExpect(status().isUnauthorized());
    }

    private static List<OlxStateDto> statesFixture() {
        return List.of(
                new OlxStateDto(1L, "Federacija BiH", "FBiH", List.of(
                        new OlxCantonDto(1L, "Unsko-sanski kanton", null))),
                new OlxStateDto(2L, "Republika Srpska", "RS", List.of(
                        new OlxCantonDto(16L, "Banjalučka regija", null))));
    }

    private String registerUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "catalog@test.ba", "password": "password123", "name": "Test User"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }
}
