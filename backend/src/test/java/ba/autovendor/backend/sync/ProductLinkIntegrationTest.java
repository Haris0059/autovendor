package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductLinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductLinkRepository linkRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createReturnsLinkWithNullListingId() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt);
        long storeId = createWooStore(jwt, "https://mojshop.ba");

        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, null, 1001, "woo_to_olx")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.olx_account_id").value(accountId))
                .andExpect(jsonPath("$.woo_store_id").value(storeId))
                .andExpect(jsonPath("$.olx_listing_id").value((Object) null))
                .andExpect(jsonPath("$.woo_product_id").value(1001))
                .andExpect(jsonPath("$.sync_direction").value("woo_to_olx"))
                .andExpect(jsonPath("$.last_synced_at").value((Object) null));
    }

    @Test
    void createAcceptsAllDirectionsAndListingId() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt);
        long storeId = createWooStore(jwt, "https://mojshop.ba");

        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, 5008L, 1001, "olx_to_woo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.olx_listing_id").value(5008))
                .andExpect(jsonPath("$.sync_direction").value("olx_to_woo"));

        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, null, 1002, "bidirectional")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sync_direction").value("bidirectional"));
    }

    @Test
    void createRejectsInvalidDirection() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt);
        long storeId = createWooStore(jwt, "https://mojshop.ba");

        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, null, 1001, "sideways")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsDuplicateProduct() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt);
        long storeId = createWooStore(jwt, "https://mojshop.ba");
        createLink(jwt, accountId, storeId, 1001);

        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, null, 1001, "woo_to_olx")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Product already linked"));
    }

    @Test
    void createRejectsForeignAccountAndStore() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long accountA = createOlxAccount(jwtA);
        long storeA = createWooStore(jwtA, "https://mojshop.ba");
        long accountB = createOlxAccount(jwtB);

        // B referencing A's account
        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountA, storeA, null, 1001, "woo_to_olx")))
                .andExpect(status().isNotFound());

        // B with own account but A's store
        mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountB, storeA, null, 1001, "woo_to_olx")))
                .andExpect(status().isNotFound());
    }

    @Test
    void linksAreScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long accountId = createOlxAccount(jwtA);
        long storeId = createWooStore(jwtA, "https://mojshop.ba");
        long linkId = createLink(jwtA, accountId, storeId, 1001);

        mockMvc.perform(get("/sync/links")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/sync/links/" + linkId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/sync/links")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(linkId));
    }

    @Test
    void deleteRemovesLink() throws Exception {
        String jwt = registerUser("a@test.ba");
        long accountId = createOlxAccount(jwt);
        long storeId = createWooStore(jwt, "https://mojshop.ba");
        long linkId = createLink(jwt, accountId, storeId, 1001);

        mockMvc.perform(delete("/sync/links/" + linkId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/sync/links")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/sync/links"))
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

    private long createOlxAccount(String jwt) throws Exception {
        when(olxApiClient.login(anyString(), anyString()))
                .thenReturn(new OlxLoginResult("163|token", 555L));
        MvcResult result = mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "olxuser-%d", "password": "olx-pass"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createWooStore(String jwt, String storeUrl) throws Exception {
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooHashPageDto(List.of(), 1, 200, 1));
        MvcResult result = mockMvc.perform(post("/woo/stores")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shop", "store_url": "%s", "api_key": "woo-secret-api-key"}
                                """.formatted(storeUrl)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createLink(String jwt, long accountId, long storeId, long productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/sync/links")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkJson(accountId, storeId, null, productId, "woo_to_olx")))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String linkJson(long accountId, long storeId, Long listingId, long productId, String direction) {
        return """
                {"olx_account_id": %d, "woo_store_id": %d, "olx_listing_id": %s,
                 "woo_product_id": %d, "sync_direction": "%s"}
                """.formatted(accountId, storeId, listingId, productId, direction);
    }
}
