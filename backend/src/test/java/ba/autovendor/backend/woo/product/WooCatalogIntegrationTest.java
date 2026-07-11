package ba.autovendor.backend.woo.product;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.InvalidWooApiKeyException;
import ba.autovendor.backend.common.WooPluginException;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooCatalogPageDto;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginAttributeDto;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
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
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WooCatalogIntegrationTest {

    private static final String STORE_URL = "https://mojshop.ba";
    private static final String API_KEY = "woo-secret-api-key-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    void productsAggregatePagesAndMapToFrontendShape() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        stubTwoCatalogPages();

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(101))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Proizvod 1"))
                .andExpect(jsonPath("$[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$[0].status").value("publish"))
                .andExpect(jsonPath("$[0].price").value("29.99"))
                .andExpect(jsonPath("$[0].regular_price").value("39.99"))
                .andExpect(jsonPath("$[0].sale_price").value("29.99"))
                .andExpect(jsonPath("$[0].currency").value("KM"))
                .andExpect(jsonPath("$[0].stock_status").value("instock"))
                .andExpect(jsonPath("$[0].stock_quantity").value(5))
                .andExpect(jsonPath("$[0].short_description").value("Kratki opis"))
                .andExpect(jsonPath("$[0].categories[0].id").value(10))
                .andExpect(jsonPath("$[0].categories[0].parent").value(0))
                .andExpect(jsonPath("$[0].images[0].src").value("https://img.test/1.jpg"))
                .andExpect(jsonPath("$[100].id").value(101));

        verify(wooPluginClient).getCatalog(eq(STORE_URL), eq(API_KEY), eq(1), anyInt());
        verify(wooPluginClient).getCatalog(eq(STORE_URL), eq(API_KEY), eq(2), anyInt());
    }

    @Test
    void productsAreCached() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        stubSingleCatalogPage();

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("KM"));

        verify(wooPluginClient, times(1)).getCatalog(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void categoriesFlattenTreeWithParents() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        when(wooPluginClient.getCategories(anyString(), anyString())).thenReturn(List.of(
                new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", List.of(
                        new WooPluginCategoryDto(11L, "Motor", "motor", List.of())
                )),
                new WooPluginCategoryDto(20L, "Gume", "gume", List.of())
        ));

        mockMvc.perform(get("/woo/stores/" + storeId + "/categories")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].parent").value(0))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name").value("Motor"))
                .andExpect(jsonPath("$[1].parent").value(10))
                .andExpect(jsonPath("$[2].id").value(20))
                .andExpect(jsonPath("$[2].parent").value(0));
    }

    @Test
    void attributesMapWithDefaultsAndTerms() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        when(wooPluginClient.getAttributes(anyString(), anyString())).thenReturn(List.of(
                new WooPluginAttributeDto(1L, "Boja", "pa_boja", "select", "menu_order", List.of(
                        new WooPluginAttributeDto.Term(5L, "Crvena", "crvena")
                ))
        ));

        mockMvc.perform(get("/woo/stores/" + storeId + "/attributes")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Boja"))
                .andExpect(jsonPath("$[0].order_by").value("menu_order"))
                .andExpect(jsonPath("$[0].has_archives").value(false))
                .andExpect(jsonPath("$[0].variation").value(false))
                .andExpect(jsonPath("$[0].terms[0].name").value("Crvena"));
    }

    @Test
    void catalogIsScopedToOwnerEvenWhenCached() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long storeId = createStore(jwtA);
        stubSingleCatalogPage();

        // Owner primes the cache.
        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());
    }

    @Test
    void storeUpdateEvictsCatalogCaches() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        stubSingleCatalogPage();

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(put("/woo/stores/" + storeId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"api_key\": \"new-woo-key-456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        verify(wooPluginClient, times(2)).getCatalog(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void upstreamFailureReturns502() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        when(wooPluginClient.getCatalog(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new WooPluginException("WooCommerce store is unreachable"));

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("WooCommerce store is unreachable"));
    }

    @Test
    void invalidStoredKeyReturns400() throws Exception {
        String jwt = registerUser("a@test.ba");
        long storeId = createStore(jwt);
        when(wooPluginClient.getCatalog(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new InvalidWooApiKeyException("Invalid WooCommerce API key"));

        mockMvc.perform(get("/woo/stores/" + storeId + "/products")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid WooCommerce API key"));
    }

    @Test
    void catalogEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/woo/stores/1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Not authenticated"));
    }

    private void stubTwoCatalogPages() {
        List<WooPluginProductDto> pageOne = IntStream.rangeClosed(1, 100)
                .mapToObj(WooCatalogIntegrationTest::product)
                .toList();
        when(wooPluginClient.getCatalog(anyString(), anyString(), eq(1), anyInt()))
                .thenReturn(new WooCatalogPageDto(pageOne, 1, 100, 100));
        when(wooPluginClient.getCatalog(anyString(), anyString(), eq(2), anyInt()))
                .thenReturn(new WooCatalogPageDto(List.of(product(101)), 2, 100, 1));
    }

    private void stubSingleCatalogPage() {
        when(wooPluginClient.getCatalog(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooCatalogPageDto(List.of(product(1)), 1, 100, 1));
    }

    private static WooPluginProductDto product(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, "Proizvod " + id, "proizvod-" + id, "SKU-" + id, "publish",
                "29.99", "39.99", "29.99", "instock", 5,
                List.of(new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", null)),
                List.of(new WooPluginImageDto(7L, "https://img.test/1.jpg", "slika", "alt")),
                "Opis proizvoda", "Kratki opis"
        );
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

    private long createStore(String jwt) throws Exception {
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooHashPageDto(List.of(), 1, 200, 1));
        MvcResult result = mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Moj Shop", "store_url": "%s", "api_key": "%s"}
                                """.formatted(STORE_URL, API_KEY)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
