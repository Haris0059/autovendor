package ba.autovendor.backend.olx.listing;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.olx.client.dto.OlxListingPageDto;
import ba.autovendor.backend.user.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OlxListingIntegrationTest {

    private static final String TOKEN = "163|listing-token";
    private static final Long OLX_USER_ID = 555L;
    private static final String OLX_USERNAME = "olxuser";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private String jwt;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
        userRepository.deleteAll();
        jwt = registerUser("listings@test.ba");
        accountId = createAccount(jwt, OLX_USERNAME);
    }

    @Test
    void listDefaultsToActiveListingsByUsername() throws Exception {
        when(olxApiClient.getActiveListings(TOKEN, OLX_USERNAME, 1, null))
                .thenReturn(page(List.of(listing(1L, "active")), 1, 1, 20, 1));

        mockMvc.perform(get(base() + "?page=1").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].account_id").value(accountId))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[0].city_id").value(39))
                .andExpect(jsonPath("$.data[0].images[0].url").value("https://img/1.jpg"))
                .andExpect(jsonPath("$.data[0].images[0].is_main").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.per_page").value(20))
                .andExpect(jsonPath("$.last_page").value(1));
    }

    @Test
    void listRoutesStatusesToStatusEndpoint() throws Exception {
        when(olxApiClient.getListingsByStatus(TOKEN, OLX_USER_ID, "finished", 1, null))
                .thenReturn(page(List.of(listing(2L, "finished")), 1, 1, 20, 1));

        mockMvc.perform(get(base() + "?status=finished").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("finished"));

        when(olxApiClient.getListingsByStatus(TOKEN, OLX_USER_ID, "inactive", 1, null))
                .thenReturn(page(List.of(listing(3L, "draft")), 1, 1, 20, 1));

        mockMvc.perform(get(base() + "?status=draft").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("draft"));

        verify(olxApiClient).getListingsByStatus(TOKEN, OLX_USER_ID, "inactive", 1, null);
    }

    @Test
    void thumbnailFallsBackWhenImagesListIsNull() throws Exception {
        // Some list endpoints (e.g. finished) return images: null with only `image` set.
        when(olxApiClient.getListingsByStatus(TOKEN, OLX_USER_ID, "finished", 1, null))
                .thenReturn(page(List.of(listingWithThumbnailOnly(5L, "finished")), 1, 1, 20, 1));

        mockMvc.perform(get(base() + "?status=finished").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].images", hasSize(1)))
                .andExpect(jsonPath("$.data[0].images[0].url").value("https://img/thumb.jpg"))
                .andExpect(jsonPath("$.data[0].images[0].is_main").value(true));
    }

    @Test
    void hiddenListingsGetStatusOverride() throws Exception {
        // OLX's /hidden endpoint returns items whose status field still says "active".
        when(olxApiClient.getListingsByStatus(TOKEN, OLX_USER_ID, "hidden", 1, null))
                .thenReturn(page(List.of(listing(4L, "active")), 1, 1, 20, 1));

        mockMvc.perform(get(base() + "?status=hidden").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("hidden"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createWhitelistsPayloadAndReturnsDraft() throws Exception {
        when(olxApiClient.createListing(eq(TOKEN), any(Map.class)))
                .thenReturn(listing(10L, "draft"));

        mockMvc.perform(post(base())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Test artikal", "price": 10.5, "city_id": 39,
                                 "category_id": 299, "listing_type": "sell", "state": "used",
                                 "top_category_id": 5, "state_id": 1, "canton_id": 4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.status").value("draft"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(olxApiClient).createListing(eq(TOKEN), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("title", "Test artikal")
                .containsEntry("city_id", 39L)
                .containsEntry("category_id", 299L)
                .doesNotContainKeys("top_category_id", "state_id", "canton_id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAndDeleteAreRouted() throws Exception {
        when(olxApiClient.updateListing(eq(TOKEN), eq(10L), any(Map.class)))
                .thenReturn(listing(10L, "draft"));

        mockMvc.perform(put(base() + "/10")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Novi naslov\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(delete(base() + "/10").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        verify(olxApiClient).deleteListing(TOKEN, 10L);
    }

    @Test
    void actionsAreRoutedToClientMethods() throws Exception {
        mockMvc.perform(post(base() + "/10/publish").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).publishListing(TOKEN, 10L);

        mockMvc.perform(post(base() + "/10/finish").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).finishListing(TOKEN, 10L);

        mockMvc.perform(post(base() + "/10/hide").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).hideListing(TOKEN, 10L);

        mockMvc.perform(post(base() + "/10/unhide").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).unhideListing(TOKEN, 10L);

        mockMvc.perform(put(base() + "/10/refresh").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).refreshListing(TOKEN, 10L);
    }

    @Test
    void listingsAreScopedToAccountOwner() throws Exception {
        String jwtB = registerUser("other@test.ba");

        mockMvc.perform(get(base()).header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/olx/accounts/999999/listings").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(base()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allAggregatesPaginatesAndCaches() throws Exception {
        when(olxApiClient.getActiveListings(eq(TOKEN), eq(OLX_USERNAME), eq(1), isNull()))
                .thenReturn(page(List.of(listing(1L, "active")), 5, 1, 1, 2));
        when(olxApiClient.getActiveListings(eq(TOKEN), eq(OLX_USERNAME), eq(2), isNull()))
                .thenReturn(page(List.of(listing(2L, "active")), 5, 2, 1, 2));
        when(olxApiClient.getListingsByStatus(eq(TOKEN), eq(OLX_USER_ID), anyString(), anyInt(), isNull()))
                .thenReturn(page(List.of(), 0, 1, 20, 1));
        when(olxApiClient.getListingsByStatus(eq(TOKEN), eq(OLX_USER_ID), eq("finished"), eq(1), isNull()))
                .thenReturn(page(List.of(listing(3L, "finished")), 1, 1, 20, 1));

        mockMvc.perform(get("/olx/listings/all").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/olx/listings/all").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        verify(olxApiClient, times(1)).getActiveListings(eq(TOKEN), eq(OLX_USERNAME), eq(1), isNull());
        verify(olxApiClient, times(1)).getActiveListings(eq(TOKEN), eq(OLX_USERNAME), eq(2), isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allCacheIsEvictedByMutations() throws Exception {
        when(olxApiClient.getActiveListings(eq(TOKEN), eq(OLX_USERNAME), anyInt(), isNull()))
                .thenReturn(page(List.of(listing(1L, "active")), 1, 1, 20, 1));
        when(olxApiClient.getListingsByStatus(eq(TOKEN), eq(OLX_USER_ID), anyString(), anyInt(), isNull()))
                .thenReturn(page(List.of(), 0, 1, 20, 1));
        when(olxApiClient.createListing(eq(TOKEN), any(Map.class)))
                .thenReturn(listing(10L, "draft"));

        mockMvc.perform(get("/olx/listings/all").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(post(base())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Evict test\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/olx/listings/all").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        verify(olxApiClient, times(2)).getActiveListings(eq(TOKEN), eq(OLX_USERNAME), eq(1), isNull());
    }

    @Test
    void imagesUploadDeleteAndMainAreRouted() throws Exception {
        when(olxApiClient.uploadImages(eq(TOKEN), eq(10L), any()))
                .thenReturn(List.of(new OlxImageDto(77L, "a.jpg",
                        Map.of("sm", "https://img/sm.jpg", "lg", "https://img/lg.jpg"), true)));

        mockMvc.perform(multipart(base() + "/10/images")
                        .file(new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(77))
                .andExpect(jsonPath("$[0].url").value("https://img/lg.jpg"))
                .andExpect(jsonPath("$[0].is_main").value(true));

        mockMvc.perform(delete(base() + "/10/images/77").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        verify(olxApiClient).deleteImage(TOKEN, 10L, 77L);

        mockMvc.perform(post(base() + "/10/images/77/main").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        verify(olxApiClient).setMainImage(TOKEN, 10L, 77L);
    }

    @Test
    void upstreamClientErrorSurfacesOlxMessageAs400() throws Exception {
        org.mockito.Mockito.doThrow(new OlxApiException("Osvježavanje nije dostupno", 400))
                .when(olxApiClient).refreshListing(TOKEN, 10L);

        mockMvc.perform(put(base() + "/10/refresh").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Osvježavanje nije dostupno"));
    }

    @Test
    void upstreamFailureReturns502() throws Exception {
        when(olxApiClient.getActiveListings(anyString(), anyString(), anyInt(), isNull()))
                .thenThrow(new OlxApiException("OLX request failed with status 500"));

        mockMvc.perform(get(base()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("OLX request failed with status 500"));
    }

    private String base() {
        return "/olx/accounts/" + accountId + "/listings";
    }

    private static OlxListingDto listing(Long id, String status) {
        return new OlxListingDto(
                id, "Test artikal", 10.5, true, 299L, 39L, null,
                "sell", "used", status, null, null, "Kratki opis", null,
                new OlxListingDto.OlxListingAdditionalDto("<p>Opis</p>"),
                null,
                List.of("https://img/1.jpg", "https://img/2.jpg"),
                1749494992L, 1781513914L
        );
    }

    private static OlxListingDto listingWithThumbnailOnly(Long id, String status) {
        return new OlxListingDto(
                id, "Test artikal", 10.5, true, 299L, 39L, null,
                "sell", "used", status, null, null, "Kratki opis", null,
                null,
                "https://img/thumb.jpg", null,
                1749494992L, 1781513914L
        );
    }

    private static OlxListingPageDto page(List<OlxListingDto> data, long total, int current, int perPage, int last) {
        return new OlxListingPageDto(data, new OlxListingPageDto.OlxPageMetaDto(total, last, current, perPage));
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

    private long createAccount(String jwt, String username) throws Exception {
        when(olxApiClient.login(anyString(), anyString()))
                .thenReturn(new OlxLoginResult(TOKEN, OLX_USER_ID));
        MvcResult result = mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "olx-pass"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
