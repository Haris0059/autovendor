package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.WooPluginException;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooCatalogPageDto;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BulkSyncIntegrationTest {

    @Autowired
    private BulkSyncService bulkSyncService;

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
    private OlxTokenManager tokenManager;

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

        user = userRepository.save(new User("bulk@test.ba", "irrelevant", "Bulk Tester"));
        account = new OlxAccount(user.getId(), "olx-user", "olx-pass", 5L);
        account.updateToken("163|token", 555L, OffsetDateTime.now().plusDays(10));
        account = accountRepository.save(account);
        store = storeRepository.save(new WooStore(user.getId(), "Shop", "https://shop.test", "api-key"));
    }

    @Test
    void changedHashIsResyncedAndLogged() {
        ProductLink link = savedLink(1001L, 4242L, "old-hash");
        stubHashes(hash(1001L, "hash-1001"));
        stubCatalog(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        bulkSyncService.syncStore(store);

        verify(olxApiClient).updateListing(anyString(), eq(4242L), anyMap());
        assertThat(linkRepository.findById(link.getId()).orElseThrow().getWooHash()).isEqualTo("hash-1001");
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.success);
        assertThat(logs.getFirst().getAction()).isEqualTo("update");
    }

    @Test
    void unchangedHashIsUntouched() {
        savedLink(1001L, 4242L, "hash-1001");
        stubHashes(hash(1001L, "hash-1001"));

        bulkSyncService.syncStore(store);

        verify(wooPluginClient, never()).getCatalogByIds(anyString(), anyString(), anyList());
        verify(olxApiClient, never()).updateListing(anyString(), anyLong(), anyMap());
        assertThat(syncLogRepository.count()).isZero();
    }

    @Test
    void nullHashLinkIsSynced() {
        savedLink(1001L, 4242L, null);
        stubHashes(hash(1001L, "hash-1001"));
        stubCatalog(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        bulkSyncService.syncStore(store);

        verify(olxApiClient).updateListing(anyString(), eq(4242L), anyMap());
        assertThat(syncLogRepository.count()).isEqualTo(1);
    }

    @Test
    void unlinkedMappedProductIsAutoLinkedAndCreated() {
        saveMapping(10L, 6L);
        stubHashes(hash(2002L, "hash-2002"));
        stubCatalog(product(2002));
        when(olxApiClient.createListing(anyString(), anyMap())).thenReturn(listingDto(555L));
        when(olxApiClient.uploadImageByUrl(anyString(), eq(555L), anyString()))
                .thenReturn(List.of(new OlxImageDto(77L, "img-1.jpg", null, true)));

        bulkSyncService.syncStore(store);

        ProductLink created = linkRepository.findByWooStoreIdAndWooProductId(store.getId(), 2002L).orElseThrow();
        assertThat(created.getUserId()).isEqualTo(user.getId());
        assertThat(created.getOlxAccountId()).isEqualTo(account.getId());
        assertThat(created.getSyncDirection()).isEqualTo(SyncDirection.woo_to_olx);
        assertThat(created.getOlxListingId()).isEqualTo(555L);
        verify(olxApiClient).publishListing(anyString(), eq(555L));
        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getAction()).isEqualTo("create");
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.success);
    }

    @Test
    void autoLinkSkippedWhenUserHasSeveralAccounts() {
        accountRepository.save(new OlxAccount(user.getId(), "second-olx", "pass", 5L));
        saveMapping(10L, 6L);
        stubHashes(hash(2002L, "hash-2002"));
        stubCatalog(product(2002));

        bulkSyncService.syncStore(store);

        assertThat(linkRepository.count()).isZero();
        assertThat(syncLogRepository.count()).isZero();
        verify(olxApiClient, never()).createListing(anyString(), anyMap());
    }

    @Test
    void autoLinkSkippedForUnmappedCategoryOrNonPublishOrNoCity() {
        // No mapping for category 10.
        stubHashes(hash(2002L, "hash-2002"));
        stubCatalog(product(2002));
        bulkSyncService.syncStore(store);
        assertThat(linkRepository.count()).isZero();

        // Mapped, but the product is a draft.
        saveMapping(10L, 6L);
        stubCatalog(draftProduct(2002));
        bulkSyncService.syncStore(store);
        assertThat(linkRepository.count()).isZero();

        // Mapped and published, but the account has no default city.
        OlxAccount cityless = accountRepository.findById(account.getId()).orElseThrow();
        cityless.setDefaultCityId(null);
        accountRepository.save(cityless);
        stubCatalog(product(2002));
        bulkSyncService.syncStore(store);
        assertThat(linkRepository.count()).isZero();

        assertThat(syncLogRepository.count()).isZero();
        verify(olxApiClient, never()).createListing(anyString(), anyMap());
    }

    @Test
    void repeatedFailureIsLoggedOnce() {
        savedLink(1001L, 4242L, "old-hash");
        stubHashes(hash(1001L, "hash-1001"));
        stubCatalog(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap()))
                .thenThrow(new RuntimeException("OLX request failed with status 500"));

        bulkSyncService.syncStore(store);
        bulkSyncService.syncStore(store);

        List<SyncLog> logs = syncLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getStatus()).isEqualTo(SyncStatus.failed);
    }

    @Test
    void brokenStoreDoesNotStallOthers() {
        WooStore second = storeRepository.save(
                new WooStore(user.getId(), "Shop 2", "https://shop2.test", "api-key-2"));
        savedLinkFor(second, 1001L, 4242L, "old-hash");

        when(wooPluginClient.getCatalogHashes(eq("https://shop.test"), anyString(), anyInt(), anyInt()))
                .thenThrow(new WooPluginException("Store unreachable"));
        when(wooPluginClient.getCatalogHashes(eq("https://shop2.test"), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooHashPageDto(List.of(hash(1001L, "hash-1001")), 1, 200, 1));
        when(wooPluginClient.getCatalogByIds(eq("https://shop2.test"), anyString(), anyList()))
                .thenReturn(new WooCatalogPageDto(List.of(product(1001)), 1, 50, 1));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        bulkSyncService.sweepAllStores();

        verify(olxApiClient).updateListing(anyString(), eq(4242L), anyMap());
        assertThat(syncLogRepository.count()).isEqualTo(1);
    }

    @Test
    void sweepEvictsListingsCacheForAffectedUser() {
        savedLink(1001L, 4242L, "old-hash");
        stubHashes(hash(1001L, "hash-1001"));
        stubCatalog(product(1001));
        when(olxApiClient.updateListing(anyString(), eq(4242L), anyMap())).thenReturn(listingDto(4242L));

        Objects.requireNonNull(cacheManager.getCache("olx-listings-all")).put(user.getId(), "cached");
        bulkSyncService.syncStore(store);

        assertThat(Objects.requireNonNull(cacheManager.getCache("olx-listings-all"))
                .get(user.getId())).isNull();
    }

    @Test
    void expiringTokenIsRefreshed() {
        OlxAccount expiring = accountRepository.findById(account.getId()).orElseThrow();
        expiring.updateToken("163|old", 555L, OffsetDateTime.now().plusMinutes(10));
        accountRepository.save(expiring);
        when(olxApiClient.login(anyString(), anyString())).thenReturn(new OlxLoginResult("164|fresh", 555L));

        bulkSyncService.refreshExpiringTokens(Duration.ofMinutes(30));

        verify(olxApiClient).login("olx-user", "olx-pass");
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getToken())
                .isEqualTo("164|fresh");
    }

    @Test
    void freshTokenIsNotRefreshed() {
        bulkSyncService.refreshExpiringTokens(Duration.ofMinutes(30));

        verify(olxApiClient, never()).login(anyString(), anyString());
    }

    // Fixtures

    private ProductLink savedLink(long productId, Long listingId, String wooHash) {
        return savedLinkFor(store, productId, listingId, wooHash);
    }

    private ProductLink savedLinkFor(WooStore target, long productId, Long listingId, String wooHash) {
        ProductLink link = new ProductLink(user.getId(), account.getId(), target.getId(),
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

    private void stubHashes(WooHashPageDto.ProductHash... hashes) {
        when(wooPluginClient.getCatalogHashes(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new WooHashPageDto(List.of(hashes), 1, 200, hashes.length));
    }

    private void stubCatalog(WooPluginProductDto... products) {
        when(wooPluginClient.getCatalogByIds(anyString(), anyString(), anyList()))
                .thenReturn(new WooCatalogPageDto(List.of(products), 1, 50, products.length));
    }

    private static WooHashPageDto.ProductHash hash(long id, String value) {
        return new WooHashPageDto.ProductHash(id, value);
    }

    private static WooPluginProductDto product(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, "Proizvod " + id, "proizvod-" + id, "SKU-" + id, "publish",
                "19.99", "19.99", "", "instock", 5,
                List.of(new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", null)),
                List.of(new WooPluginImageDto(1L, "https://img.test/1.jpg", "a", "")),
                "Opis", "Kratki opis", null
        );
    }

    private static WooPluginProductDto draftProduct(long id) {
        return new WooPluginProductDto(
                id, "hash-" + id, "Proizvod " + id, "proizvod-" + id, "SKU-" + id, "draft",
                "19.99", "19.99", "", "instock", 5,
                List.of(new WooPluginCategoryDto(10L, "Auto dijelovi", "auto-dijelovi", null)),
                List.of(), "Opis", "Kratki opis", null
        );
    }

    private static OlxListingDto listingDto(long id) {
        return new OlxListingDto(id, null, null, null, null, null, null, null, null, "draft",
                null, null, null, null, null, null, null, null, null);
    }
}
