package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryMappingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        // Attribute validation goes through the cached CategoryService path.
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    void createReturnsMappingShape() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.woo_category_id").value(10))
                .andExpect(jsonPath("$.woo_category_name").value("Auto dijelovi"))
                .andExpect(jsonPath("$.olx_category_id").value(6))
                .andExpect(jsonPath("$.olx_category_name").value("Auto dijelovi OLX"));
    }

    @Test
    void createRejectsDuplicateWooCategoryPerUser() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        createMapping(jwtA, 10);

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 7, "Druga")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category already mapped"));

        // The same woo category is fine for another user.
        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated());
    }

    @Test
    void createValidatesInput() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"woo_category_id\": null, \"woo_category_name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    void mappingsAreScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long mappingId = createMapping(jwtA, 10);

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mappingId));
    }

    @Test
    void deleteRemovesMapping() throws Exception {
        String jwt = registerUser("a@test.ba");
        long mappingId = createMapping(jwt, 10);

        mockMvc.perform(delete("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createWithDefaultsPersistsAndEchoesThem() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubAttributes(1179L, requiredVrsta(), optionalNamjena());

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"woo_category_id": 308, "woo_category_name": "Polir Papiri",
                                 "olx_category_id": 1179, "olx_category_name": "Za obradu keramike i stakla",
                                 "attribute_defaults": {"vrsta": "Za keramiku"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attribute_defaults.vrsta").value("Za keramiku"));

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attribute_defaults.vrsta").value("Za keramiku"));
    }

    @Test
    void createMissingRequiredDefaultIsRejected() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubAttributes(1179L, requiredVrsta());

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(308, "Polir Papiri", 1179, "Za obradu keramike i stakla")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required OLX attribute 'Vrsta' needs a default value"));
    }

    @Test
    void createInvalidOptionValueIsRejected() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubAttributes(1179L, requiredVrsta());

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"woo_category_id": 308, "woo_category_name": "Polir Papiri",
                                 "olx_category_id": 1179, "olx_category_name": "Za obradu keramike i stakla",
                                 "attribute_defaults": {"vrsta": "Nepostojeca"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Value 'Nepostojeca' is not a valid option for OLX attribute 'Vrsta'"));
    }

    @Test
    void createUnknownDefaultKeyIsRejected() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubAttributes(1180L);

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"woo_category_id": 308, "woo_category_name": "Polir Papiri",
                                 "olx_category_id": 1180, "olx_category_name": "Neka kategorija",
                                 "attribute_defaults": {"boja": "Crvena"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown OLX attribute 'boja' for this category"));
    }

    @Test
    void updateChangesTargetAndDefaults() throws Exception {
        String jwt = registerUser("a@test.ba");
        long mappingId = createMapping(jwt, 10);
        stubAttributes(1179L, requiredVrsta());

        mockMvc.perform(put("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"olx_category_id": 1179, "olx_category_name": "Za obradu keramike i stakla",
                                 "attribute_defaults": {"vrsta": "Za staklo"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.olx_category_id").value(1179))
                .andExpect(jsonPath("$.woo_category_id").value(10))
                .andExpect(jsonPath("$.attribute_defaults.vrsta").value("Za staklo"));
    }

    @Test
    void updateForeignMappingIs404() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long mappingId = createMapping(jwtA, 10);

        mockMvc.perform(put("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"olx_category_id\": 6, \"olx_category_name\": \"Druga\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/sync/mappings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Not authenticated"));
    }

    private String registerUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123", "name": "Test User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    private long createMapping(String jwt, long wooCategoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(wooCategoryId, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String mappingJson(long wooId, String wooName, long olxId, String olxName) {
        return """
                {"woo_category_id": %d, "woo_category_name": "%s",
                 "olx_category_id": %d, "olx_category_name": "%s"}
                """.formatted(wooId, wooName, olxId, olxName);
    }

    private void stubAttributes(long categoryId, OlxAttributeDto... attributes) {
        when(olxApiClient.getCategoryAttributes(eq(categoryId))).thenReturn(List.of(attributes));
    }

    private static OlxAttributeDto requiredVrsta() {
        return new OlxAttributeDto(3753L, "string", "vrsta", "select", "Vrsta",
                List.of("Za staklo", "Za keramiku", "Ostalo"), true);
    }

    private static OlxAttributeDto optionalNamjena() {
        return new OlxAttributeDto(7651L, "string", "namjena", "select", "Namjena",
                List.of("Profesionalna upotreba", "Hobi ili Kućna radionica"), false);
    }
}
