package ba.autovendor.backend.webhook;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.sync.CategoryMapping;
import ba.autovendor.backend.sync.CategoryMappingRepository;
import ba.autovendor.backend.sync.ProductLink;
import ba.autovendor.backend.sync.ProductLinkRepository;
import ba.autovendor.backend.sync.SyncDirection;
import ba.autovendor.backend.sync.SyncLog;
import ba.autovendor.backend.sync.SyncLogRepository;
import ba.autovendor.backend.sync.SyncStatus;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebhookIntegrationTest {

    private static final String API_KEY = "store-api-key";
    private static final String SITE_URL = "https://shop.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OlxAccountRepository accountRepository;

    @Autowired
    private WooStoreRepository storeRepository;

    @Autowired
    private ProductLinkRepository linkRepository;

    @Autowired
    private CategoryMappingRepository mappingRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    private User user;
    private OlxAccount account;
    private WooStore store;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());

        user = userRepository.save(new User("hook@test.ba", "irrelevant", "Hook Tester"));
        account = new OlxAccount(user.getId(), "olx-user", "olx-pass", 5L);
        account.updateToken("163|token", 555L, OffsetDateTime.now().plusDays(10));
        account = accountRepository.save(account);
        store = storeRepository.save(new WooStore(user.getId(), "Shop", SITE_URL, API_KEY));
    }

    @Test
    void updatedEventSyncsExistingLink() throws Exception {
        savedLink(1001L, 4242L, "old-hash");
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        postWebhook("product.updated", 1001L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        verify(olxApiClient).updateListing(anyString(), eq(4242L), anyMap());
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getAction()).isEqualTo("update");
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.success);
    }

    @Test
    void unchangedHashAbsorbsDoubleFire() throws Exception {
        savedLink(1001L, 4242L, "hash-1001");
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));

        postWebhook("product.updated", 1001L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        verify(olxApiClient, never()).updateListing(anyString(), anyLong(), anyMap());
        assertThat(syncLogRepository.count()).isZero();
    }

    @Test
    void createdEventAutoLinksWhenPreconditionsHold() throws Exception {
        saveMapping(10L, 6L);
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(2002L))).thenReturn(product(2002));
        when(olxApiClient.createListing(anyString(), anyMap())).thenReturn(listingDto(555L));
        when(olxApiClient.uploadImageByUrl(anyString(), eq(555L), anyString()))
                .thenReturn(List.of(new OlxImageDto(77L, "img.jpg", null, true)));

        postWebhook("product.created", 2002L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        ProductLink created = linkRepository.findByWooStoreIdAndWooProductId(store.getId(), 2002L).orElseThrow();
        assertThat(created.getOlxListingId()).isEqualTo(555L);
        verify(olxApiClient).publishListing(anyString(), eq(555L));
        assertThat(syncLogRepository.count()).isEqualTo(1);
    }

    @Test
    void ambiguousAccountsSkipWithLogAndNoLink() throws Exception {
        accountRepository.save(new OlxAccount(user.getId(), "second-olx", "pass", 5L));
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(2002L))).thenReturn(product(2002));

        postWebhook("product.created", 2002L, API_KEY).andExpect(status().isOk());

        assertThat(linkRepository.count()).isZero();
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.skipped);
        assertThat(logs.getFirst().getProductLinkId()).isNull();
        assertThat(logs.getFirst().getMessage()).contains("exactly one OLX account");
        verify(olxApiClient, never()).createListing(anyString(), anyMap());
    }

    @Test
    void unmappedCategorySkipsWithLogAndNoLink() throws Exception {
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(2002L))).thenReturn(product(2002));

        postWebhook("product.updated", 2002L, API_KEY).andExpect(status().isOk());

        assertThat(linkRepository.count()).isZero();
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.skipped);
        assertThat(logs.getFirst().getMessage()).contains("No category mapping");
    }

    @Test
    void deletedEventHidesListingAndKeepsLink() throws Exception {
        ProductLink link = savedLink(1001L, 4242L, "hash-1001");

        postWebhook("product.deleted", 1001L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        verify(olxApiClient).hideListing(anyString(), eq(4242L));
        assertThat(linkRepository.findById(link.getId())).isPresent();
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getAction()).isEqualTo("hide");
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.success);
    }

    @Test
    void deletedEventForUnlinkedProductDoesNothing() throws Exception {
        postWebhook("product.deleted", 999L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        verify(olxApiClient, never()).hideListing(anyString(), anyLong());
        assertThat(syncLogRepository.count()).isZero();
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        postWebhook("product.updated", 1001L, "wrong-key")
                .andExpect(status().isUnauthorized());

        verify(wooPluginClient, never()).getProduct(anyString(), anyString(), anyLong());
    }

    @Test
    void missingKeyIsRejected() throws Exception {
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("product.updated", 1001L, SITE_URL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSiteUrlIsRejected() throws Exception {
        mockMvc.perform(post("/webhook")
                        .header("X-AutoVendor-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("product.updated", 1001L, "https://unknown.test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allUsersSharingTheStoreAreProcessed() throws Exception {
        User other = userRepository.save(new User("other@test.ba", "irrelevant", "Other"));
        OlxAccount otherAccount = new OlxAccount(other.getId(), "other-olx", "pass", 5L);
        otherAccount.updateToken("163|other", 556L, OffsetDateTime.now().plusDays(10));
        accountRepository.save(otherAccount);
        WooStore otherStore = storeRepository.save(new WooStore(other.getId(), "Shop", SITE_URL, API_KEY));

        savedLink(1001L, 4242L, "old-hash");
        linkRepository.save(new ProductLink(other.getId(), otherAccount.getId(), otherStore.getId(),
                5353L, 1001L, SyncDirection.woo_to_olx));
        when(wooPluginClient.getProduct(anyString(), anyString(), eq(1001L))).thenReturn(product(1001));
        when(olxApiClient.updateListing(anyString(), anyLong(), anyMap())).thenReturn(listingDto(4242L));

        postWebhook("product.updated", 1001L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        verify(olxApiClient).updateListing(anyString(), eq(4242L), anyMap());
        verify(olxApiClient).updateListing(anyString(), eq(5353L), anyMap());
        assertThat(syncLogRepository.count()).isEqualTo(2);
    }

    @Test
    void unknownEventIsAcceptedAndIgnored() throws Exception {
        postWebhook("product.renamed", 1001L, API_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        verify(wooPluginClient, never()).getProduct(anyString(), anyString(), anyLong());
        assertThat(syncLogRepository.count()).isZero();
    }

    // Helpers

    private org.springframework.test.web.servlet.ResultActions postWebhook(
            String event, long productId, String key) throws Exception {
        return mockMvc.perform(post("/webhook")
                .header("X-AutoVendor-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(event, productId, SITE_URL)));
    }

    private static String payload(String event, long productId, String siteUrl) {
        return """
                {"event": "%s", "product_id": %d, "site_url": "%s", "timestamp": "2026-07-12T10:00:00+00:00"}
                """.formatted(event, productId, siteUrl);
    }

    private ProductLink savedLink(long productId, Long listingId, String wooHash) {
        ProductLink link = new ProductLink(user.getId(), account.getId(), store.getId(),
                listingId, productId, SyncDirection.woo_to_olx);
        if (wooHash != null) {
            link.markSynced(wooHash, OffsetDateTime.now().minusDays(1));
        }
        return linkRepository.save(link);
    }

    private void saveMapping(long wooCategoryId, long olxCategoryId) {
        mappingRepository.save(new CategoryMapping(user.getId(), wooCategoryId, "Auto dijelovi",
                olxCategoryId, "Auto dijelovi OLX"));
    }

    private static WooPluginProductDto product(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, "Proizvod " + id, "proizvod-" + id, "SKU-" + id, "publish",
                "19.99", "19.99", "", "instock", 5,
                List.of(new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", null)),
                List.of(new WooPluginImageDto(1L, "https://img.test/1.jpg", "a", "")),
                "Opis", "Kratki opis"
        );
    }

    private static OlxListingDto listingDto(long id) {
        return new OlxListingDto(id, null, null, null, null, null, null, null, null, "draft",
                null, null, null, null, null, null, null, null, null);
    }
}
