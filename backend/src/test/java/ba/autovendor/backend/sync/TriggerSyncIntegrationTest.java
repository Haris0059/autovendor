package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.common.WooPluginException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TriggerSyncIntegrationTest {

    private static final String LONG_NAME =
            "Dijamantska kruna za keramiku i gres plocice sa vakuum brazing tehnologijom profesionalna";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductLinkRepository linkRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    void updatePathBuildsWhitelistedPayload() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, 4242L, "woo_to_olx");
        createMapping(jwt, 10, 6);

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.action").value("update"))
                .andExpect(jsonPath("$.product_link_id").value(linkId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(olxApiClient).updateListing(anyString(), eq(4242L), payload.capture());
        Map<String, Object> sent = payload.getValue();

        assertThat((String) sent.get("title")).hasSizeLessThanOrEqualTo(65).startsWith("Dijamantska kruna");
        assertThat(sent.get("price")).isEqualTo(39.99);          // price blank -> regular_price fallback
        assertThat(sent.get("country_id")).isEqualTo(49L);
        assertThat(sent.get("city_id")).isEqualTo(5L);
        assertThat(sent.get("category_id")).isEqualTo(6L);       // via mapping
        assertThat(sent.get("attributes")).isEqualTo(List.of()); // OLX rejects payloads without it
        assertThat(sent.get("sku_number")).isEqualTo("SKU-1");
        assertThat(sent.get("available")).isEqualTo(true);
        assertThat(sent.get("listing_type")).isEqualTo("sell");
        assertThat(sent.get("state")).isEqualTo("new");
        assertThat(sent.get("short_description")).isEqualTo("Kratki opis");   // HTML stripped
        verify(olxApiClient, never()).createListing(anyString(), anyMap());
        verify(olxApiClient, never()).publishListing(anyString(), anyLong());

        ProductLink link = linkRepository.findAll().getFirst();
        assertThat(link.getWooHash()).isEqualTo("hash-1001");
        assertThat(link.getLastSyncedAt()).isNotNull();
    }

    @Test
    void createPathCreatesUploadsImagesAndPublishes() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");
        createMapping(jwt, 10, 6);

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.createListing(anyString(), anyMap())).thenReturn(listingDto(555L));
        when(olxApiClient.uploadImageByUrl(anyString(), eq(555L), anyString()))
                .thenReturn(List.of(new OlxImageDto(77L, "img", null, true)))
                .thenReturn(List.of(new OlxImageDto(78L, "img2", null, false)));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.action").value("create"));

        verify(olxApiClient).uploadImageByUrl(anyString(), eq(555L), eq("https://img.test/1.jpg"));
        verify(olxApiClient).uploadImageByUrl(anyString(), eq(555L), eq("https://img.test/2.jpg"));
        verify(olxApiClient).setMainImage(anyString(), eq(555L), eq(77L));
        verify(olxApiClient).publishListing(anyString(), eq(555L));

        ProductLink link = linkRepository.findAll().getFirst();
        assertThat(link.getOlxListingId()).isEqualTo(555L);
        assertThat(link.getWooHash()).isEqualTo("hash-1001");
        assertThat(link.getLastSyncedAt()).isNotNull();
    }

    @Test
    void createWithoutMappingIsSkipped() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("skipped"))
                .andExpect(jsonPath("$.message").value(
                        "No category mapping for Woo category 'Auto dijelovi' (id 10)"));

        verify(olxApiClient, never()).createListing(anyString(), anyMap());
        assertThat(linkRepository.findAll().getFirst().getLastSyncedAt()).isNull();
    }

    @Test
    void createWithoutCategoriesIsSkipped() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L)))
                .thenReturn(productWithoutCategories(1001));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("skipped"));

        verify(olxApiClient, never()).createListing(anyString(), anyMap());
    }

    @Test
    void unsupportedDirectionsAreSkippedWithoutClientCalls() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkOlxToWoo = createLink(jwt, accountId, storeId, 1001, null, "olx_to_woo");
        long linkBidirectional = createLink(jwt, accountId, storeId, 1002, null, "bidirectional");

        for (long linkId : new long[]{linkOlxToWoo, linkBidirectional}) {
            mockMvc.perform(post("/sync")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"product_link_id\": " + linkId + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("skipped"))
                    .andExpect(jsonPath("$.message").value("Sync direction not supported yet"));
        }

        verify(wooPluginClient, never()).getProduct(anyString(), anyString(), anyLong());
    }

    @Test
    void olxCreateFailureIsLoggedAsFailed() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");
        createMapping(jwt, 10, 6);

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.createListing(anyString(), anyMap()))
                .thenThrow(new OlxApiException("Naslov je obavezan", 422));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.message").value("Naslov je obavezan"));

        ProductLink link = linkRepository.findAll().getFirst();
        assertThat(link.getOlxListingId()).isNull();
        assertThat(link.getLastSyncedAt()).isNull();
        assertThat(syncLogRepository.count()).isEqualTo(1);
    }

    @Test
    void publishFailureKeepsCreatedListingIdOnLink() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");
        createMapping(jwt, 10, 6);

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.createListing(anyString(), anyMap())).thenReturn(listingDto(555L));
        when(olxApiClient.uploadImageByUrl(anyString(), anyLong(), anyString()))
                .thenReturn(List.of(new OlxImageDto(77L, "img", null, true)));
        doThrow(new OlxApiException("Publish rejected", 422))
                .when(olxApiClient).publishListing(anyString(), eq(555L));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.message").value("Publish rejected"));

        // The draft id must be persisted so a retry takes the update path.
        assertThat(linkRepository.findAll().getFirst().getOlxListingId()).isEqualTo(555L);
    }

    @Test
    void wooFetchFailureIsLoggedAsFailed() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "woo_to_olx");

        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L)))
                .thenThrow(new WooPluginException("Product not found", 404));

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void triggerValidatesAndScopes() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long accountId = createOlxAccount(jwtA, 5L);
        long storeId = createWooStore(jwtA);
        long linkId = createLink(jwtA, accountId, storeId, 1001, null, "woo_to_olx");

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": null}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": 999999}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerEvictsAllListingsCache() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt, 5L);
        long storeId = createWooStore(jwt);
        long linkId = createLink(jwt, accountId, storeId, 1001, null, "olx_to_woo");

        Long userId = userRepository.findByEmail("a@test.ba").orElseThrow().getId();
        Objects.requireNonNull(cacheManager.getCache("olx-listings-all")).put(userId, new ArrayList<>());

        mockMvc.perform(post("/sync")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_link_id\": " + linkId + "}"))
                .andExpect(status().isOk());

        assertThat(Objects.requireNonNull(cacheManager.getCache("olx-listings-all")).get(userId)).isNull();
    }

    // Fixtures

    private static WooPluginProductDto product(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, LONG_NAME, "proizvod-" + id, "SKU-1", "publish",
                "", "39.99", "", "instock", 5,
                List.of(new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", null)),
                List.of(new WooPluginImageDto(1L, "https://img.test/1.jpg", "a", ""),
                        new WooPluginImageDto(2L, "https://img.test/2.jpg", "b", "")),
                "<h2>Opis</h2> proizvoda", "<p>Kratki&nbsp;opis</p>"
        );
    }

    private static WooPluginProductDto productWithoutCategories(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, "Proizvod", "proizvod", "SKU-1", "publish",
                "10.00", "10.00", "", "instock", 5,
                List.of(), List.of(), "Opis", "Kratki"
        );
    }

    private static OlxListingDto listingDto(long id) {
        return new OlxListingDto(id, null, null, null, null, null, null, null, null, "draft",
                null, null, null, null, null, null, null, null, null);
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

    private long createOlxAccount(String jwt, Long defaultCityId) throws Exception {
        when(olxApiClient.login(anyString(), anyString()))
                .thenReturn(new OlxLoginResult("163|token", 555L));
        MvcResult result = mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "olxuser-%d", "password": "olx-pass", "default_city_id": %d}
                                """.formatted(System.nanoTime(), defaultCityId)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createWooStore(String jwt) throws Exception {
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooHashPageDto(List.of(), 1, 200, 1));
        MvcResult result = mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shop", "store_url": "https://mojshop-%d.ba", "api_key": "woo-secret-api-key"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createLink(String jwt, long accountId, long storeId, long productId,
                            Long listingId, String direction) throws Exception {
        MvcResult result = mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"olx_account_id": %d, "woo_store_id": %d, "olx_listing_id": %s,
                                 "woo_product_id": %d, "sync_direction": "%s"}
                                """.formatted(accountId, storeId, listingId, productId, direction)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void createMapping(String jwt, long wooCategoryId, long olxCategoryId) throws Exception {
        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"woo_category_id": %d, "woo_category_name": "Auto dijelovi",
                                 "olx_category_id": %d, "olx_category_name": "Auto dijelovi OLX"}
                                """.formatted(wooCategoryId, olxCategoryId)))
                .andExpect(status().isCreated());
    }
}
