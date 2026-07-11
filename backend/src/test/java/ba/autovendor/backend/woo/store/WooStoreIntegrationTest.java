package ba.autovendor.backend.woo.store;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.InvalidWooApiKeyException;
import ba.autovendor.backend.common.WooPluginException;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class WooStoreIntegrationTest {

    private static final String STORE_URL = "https://mojshop.ba";
    private static final String API_KEY = "woo-secret-api-key-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WooStoreRepository storeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createReturnsStoreWithoutApiKey() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);

        mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeJson("Moj Shop", STORE_URL, API_KEY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Moj Shop"))
                .andExpect(jsonPath("$.store_url").value(STORE_URL))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andExpect(jsonPath("$.encrypted_api_key").doesNotExist());
    }

    @Test
    void createNormalizesStoreUrl() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);

        mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeJson("Moj Shop", "mojshop.ba/", API_KEY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.store_url").value("https://mojshop.ba"));

        verify(wooPluginClient).getCatalogHashes(eq("https://mojshop.ba"), eq(API_KEY), anyInt(), anyInt());
    }

    @Test
    void createStoresApiKeyEncrypted() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        createStore(jwt, "Moj Shop", STORE_URL);

        byte[] rawKey = jdbcTemplate.queryForObject(
                "SELECT encrypted_api_key FROM woo_stores LIMIT 1", byte[].class);

        assertThat(rawKey).isNotEqualTo(API_KEY.getBytes(StandardCharsets.UTF_8));
        assertThat(new String(rawKey, StandardCharsets.ISO_8859_1)).doesNotContain(API_KEY);

        WooStore decrypted = storeRepository.findAll().getFirst();
        assertThat(decrypted.getApiKey()).isEqualTo(API_KEY);
    }

    @Test
    void createRejectsInvalidApiKey() throws Exception {
        String jwt = registerUser("a@test.ba");
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new InvalidWooApiKeyException("Invalid WooCommerce API key"));

        mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeJson("Moj Shop", STORE_URL, "wrong-key-000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid WooCommerce API key"));

        assertThat(storeRepository.count()).isZero();
    }

    @Test
    void createRejectsDuplicateStoreUrlForSameUser() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        createStore(jwt, "Moj Shop", STORE_URL);

        mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeJson("Isti Shop", STORE_URL, API_KEY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Store already added"));
    }

    @Test
    void createValidatesInput() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"store_url\": \"\", \"api_key\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));

        verify(wooPluginClient, never()).getCatalogHashes(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void storesAreScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        stubHashes(24);
        long storeId = createStore(jwtA, "Moj Shop", STORE_URL);

        mockMvc.perform(get("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/woo/stores")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/woo/stores")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(storeId));
    }

    @Test
    void updateNameOnlyDoesNotCallPlugin() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        long storeId = createStore(jwt, "Moj Shop", STORE_URL);
        clearInvocations(wooPluginClient);

        mockMvc.perform(put("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Novi Naziv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novi Naziv"));

        verify(wooPluginClient, never()).getCatalogHashes(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void updateApiKeyReTestsWithNewKey() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        long storeId = createStore(jwt, "Moj Shop", STORE_URL);
        clearInvocations(wooPluginClient);

        mockMvc.perform(put("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"api_key\": \"new-woo-key-456\"}"))
                .andExpect(status().isOk());

        verify(wooPluginClient).getCatalogHashes(eq(STORE_URL), eq("new-woo-key-456"), anyInt(), anyInt());
        assertThat(storeRepository.findAll().getFirst().getApiKey()).isEqualTo("new-woo-key-456");
    }

    @Test
    void updateStoreUrlReTestsWithStoredKeyAndNormalizes() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        long storeId = createStore(jwt, "Moj Shop", STORE_URL);
        clearInvocations(wooPluginClient);

        mockMvc.perform(put("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_url\": \"novishop.ba/\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store_url").value("https://novishop.ba"));

        verify(wooPluginClient).getCatalogHashes(eq("https://novishop.ba"), eq(API_KEY), anyInt(), anyInt());
    }

    @Test
    void deleteRemovesStore() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(24);
        long storeId = createStore(jwt, "Moj Shop", STORE_URL);

        mockMvc.perform(delete("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testEndpointSumsHashPages() throws Exception {
        String jwt = registerUser("a@test.ba");
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), eq(1), anyInt()))
                .thenReturn(hashPage(200));
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), eq(2), anyInt()))
                .thenReturn(hashPage(37));

        mockMvc.perform(post("/woo/stores/test")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_url\": \"%s\", \"api_key\": \"%s\"}".formatted(STORE_URL, API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.products_count").value(237));
    }

    @Test
    void storedTestUsesDecryptedStoredKey() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubHashes(42);
        long storeId = createStore(jwt, "Moj Shop", STORE_URL);
        clearInvocations(wooPluginClient);

        mockMvc.perform(post("/woo/stores/" + storeId + "/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.products_count").value(42));

        verify(wooPluginClient).getCatalogHashes(eq(STORE_URL), eq(API_KEY), eq(1), anyInt());
    }

    @Test
    void storedTestIsScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        stubHashes(24);
        long storeId = createStore(jwtA, "Moj Shop", STORE_URL);

        mockMvc.perform(post("/woo/stores/" + storeId + "/test")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());
    }

    @Test
    void unreachableStoreReturns502() throws Exception {
        String jwt = registerUser("a@test.ba");
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new WooPluginException("WooCommerce store is unreachable"));

        mockMvc.perform(post("/woo/stores/test")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_url\": \"%s\", \"api_key\": \"%s\"}".formatted(STORE_URL, API_KEY)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("WooCommerce store is unreachable"));
    }

    @Test
    void missingPluginSurfacesAs400() throws Exception {
        String jwt = registerUser("a@test.ba");
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new WooPluginException("AutoVendor plugin not found on this store", 404));

        mockMvc.perform(post("/woo/stores/test")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_url\": \"%s\", \"api_key\": \"%s\"}".formatted(STORE_URL, API_KEY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("AutoVendor plugin not found on this store"));
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/woo/stores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Not authenticated"));

        mockMvc.perform(post("/woo/stores/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_url\": \"%s\", \"api_key\": \"%s\"}".formatted(STORE_URL, API_KEY)))
                .andExpect(status().isUnauthorized());
    }

    private void stubHashes(int count) {
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(hashPage(count));
    }

    private static WooHashPageDto hashPage(int count) {
        return new WooHashPageDto(List.of(), 1, 200, count);
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

    private long createStore(String jwt, String name, String storeUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeJson(name, storeUrl, API_KEY)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String storeJson(String name, String storeUrl, String apiKey) {
        return """
                {"name": "%s", "store_url": "%s", "api_key": "%s"}
                """.formatted(name, storeUrl, apiKey);
    }
}
