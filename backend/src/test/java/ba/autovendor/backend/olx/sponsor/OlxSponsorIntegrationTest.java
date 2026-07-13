package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.olx.client.dto.OlxSponsorPriceDto;
import ba.autovendor.backend.user.UserRepository;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OlxSponsorIntegrationTest {

    private static final String TOKEN = "163|sponsor-token";
    private static final long LISTING_ID = 500L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorshipRepository sponsorshipRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private String jwt;
    private long accountId;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        sponsorshipRepository.deleteAll();
        userRepository.deleteAll();
        jwt = registerUser("sponsor@test.ba");
        accountId = createAccount(jwt, "sponsoruser");
        userId = userRepository.findByEmail("sponsor@test.ba").orElseThrow().getId();
    }

    @Test
    void createSponsorQuotesPriceAndPersistsRow() throws Exception {
        stubQuote(108);

        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 7, "refresh_every": 0, "locations": ["homepage"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing_id").value(LISTING_ID))
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.type").value(1))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.refresh_every").value(0))
                .andExpect(jsonPath("$.locations", hasSize(1)))
                .andExpect(jsonPath("$.locations[0]").value("homepage"))
                .andExpect(jsonPath("$.price_total").value(108))
                .andExpect(jsonPath("$.started_at").isNotEmpty())
                .andExpect(jsonPath("$.ends_at").isNotEmpty());

        verify(olxApiClient).sponsorListing(TOKEN, LISTING_ID, 1, 7, 0, List.of("homepage"));
        Sponsorship saved = sponsorshipRepository.findAll().getFirst();
        assertThat(saved.getPriceTotal()).isEqualByComparingTo("108");
        assertThat(saved.getEndsAt()).isAfter(OffsetDateTime.now().plusDays(6));
    }

    @Test
    void createSponsorRejectsInvalidDays() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 15, "refresh_every": 0, "locations": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("days")));

        verifyNoSponsorCalls();
    }

    @Test
    void createSponsorRejectsInvalidRefreshEvery() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 7, "refresh_every": 1, "locations": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("refresh_every")));

        verifyNoSponsorCalls();
    }

    @Test
    void createSponsorRejectsTypeZero() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 0, "days": 7, "refresh_every": 0, "locations": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("type")));

        verifyNoSponsorCalls();
    }

    @Test
    void createSponsorRejectsInvalidLocationToken() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 7, "refresh_every": 0, "locations": ["home page"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("location")));

        verifyNoSponsorCalls();
    }

    @Test
    void createSponsorSupersedesActiveRowForSameListing() throws Exception {
        stubQuote(60);
        createSponsor(1, 3);
        createSponsor(2, 7);

        mockMvc.perform(get("/olx/sponsored").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value(2))
                .andExpect(jsonPath("$[0].days").value(7));

        assertThat(sponsorshipRepository.findAll()).hasSize(2);
    }

    @Test
    void createSponsorForOtherUsersAccountReturns404() throws Exception {
        String otherJwt = registerUser("sponsor-other@test.ba");

        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + otherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 7, "refresh_every": 0, "locations": []}
                                """))
                .andExpect(status().isNotFound());

        verifyNoSponsorCalls();
    }

    @Test
    void createSponsorUpstream4xxMapsTo400AndPersistsNothing() throws Exception {
        stubQuote(108);
        when(olxApiClient.sponsorListing(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyList()))
                .thenThrow(new OlxApiException("Nemate dovoljno kredita", 422));

        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": 1, "days": 7, "refresh_every": 0, "locations": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Nemate dovoljno kredita"));

        assertThat(sponsorshipRepository.findAll()).isEmpty();
    }

    @Test
    void priceEndpointProxiesQuote() throws Exception {
        stubQuote(108);

        mockMvc.perform(get(createPath() + "/price?type=1&days=7&refresh_every=0&locations=homepage")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.search").value(48))
                .andExpect(jsonPath("$.refresh").value(0))
                .andExpect(jsonPath("$.locations").value(60))
                .andExpect(jsonPath("$.extras").value(0))
                .andExpect(jsonPath("$.total").value(108));

        verify(olxApiClient).getSponsorPrice(TOKEN, LISTING_ID, 1, 7, 0, List.of("homepage"));
    }

    @Test
    void priceEndpointValidatesParams() throws Exception {
        mockMvc.perform(get(createPath() + "/price?type=3&days=7&refresh_every=0")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("type")));

        verify(olxApiClient, never()).getSponsorPrice(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyList());
    }

    @Test
    void listReturnsOnlyActiveOwnedRows() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        Sponsorship active = sponsorshipRepository.save(row(userId, 601L, now.minusDays(1), now.plusDays(6)));
        Sponsorship ended = row(userId, 602L, now.minusDays(2), now.plusDays(5));
        ended.end(now.minusDays(1));
        sponsorshipRepository.save(ended);
        sponsorshipRepository.save(row(userId, 603L, now.minusDays(10), now.minusDays(3)));

        String otherJwt = registerUser("sponsor-list-other@test.ba");
        long otherUserId = userRepository.findByEmail("sponsor-list-other@test.ba").orElseThrow().getId();
        sponsorshipRepository.save(row(otherUserId, 604L, now.minusDays(1), now.plusDays(6)));

        mockMvc.perform(get("/olx/sponsored").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(active.getId()))
                .andExpect(jsonPath("$[0].listing_id").value(601));

        mockMvc.perform(get("/olx/sponsored").header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listing_id").value(604));
    }

    @Test
    void deleteSponsorCancelsOnOlxAndEndsRow() throws Exception {
        stubQuote(108);
        long id = createSponsor(1, 7);

        mockMvc.perform(delete("/olx/sponsored/" + id).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        verify(olxApiClient).cancelSponsorship(TOKEN, LISTING_ID);
        assertThat(sponsorshipRepository.findById(id).orElseThrow().getEndedAt()).isNotNull();

        mockMvc.perform(get("/olx/sponsored").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteSponsorEndsRowEvenWhenOlxReturns4xx() throws Exception {
        // The type-0 cancel is unverified against live OLX — a 4xx must not leave
        // the row stuck in the UI.
        stubQuote(108);
        long id = createSponsor(1, 7);
        doThrow(new OlxApiException("Oglas nije sponzorisan", 422))
                .when(olxApiClient).cancelSponsorship(TOKEN, LISTING_ID);

        mockMvc.perform(delete("/olx/sponsored/" + id).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        assertThat(sponsorshipRepository.findById(id).orElseThrow().getEndedAt()).isNotNull();
    }

    @Test
    void deleteSponsorKeepsRowWhenOlxIsUnreachable() throws Exception {
        stubQuote(108);
        long id = createSponsor(1, 7);
        doThrow(new OlxApiException("OLX API is unreachable"))
                .when(olxApiClient).cancelSponsorship(TOKEN, LISTING_ID);

        mockMvc.perform(delete("/olx/sponsored/" + id).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadGateway());

        assertThat(sponsorshipRepository.findById(id).orElseThrow().getEndedAt()).isNull();
    }

    @Test
    void deleteOtherUsersSponsorReturns404() throws Exception {
        stubQuote(108);
        long id = createSponsor(1, 7);
        String otherJwt = registerUser("sponsor-delete-other@test.ba");

        mockMvc.perform(delete("/olx/sponsored/" + id).header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());

        verify(olxApiClient, never()).cancelSponsorship(anyString(), anyLong());
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/olx/sponsored"))
                .andExpect(status().isUnauthorized());
    }

    private String createPath() {
        return "/olx/accounts/" + accountId + "/listings/" + LISTING_ID + "/sponsored";
    }

    private void stubQuote(int total) {
        when(olxApiClient.getSponsorPrice(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyList()))
                .thenReturn(new OlxSponsorPriceDto(
                        new BigDecimal(48), new BigDecimal(0), new BigDecimal(60),
                        new BigDecimal(0), new BigDecimal(total)));
    }

    private long createSponsor(int type, int days) throws Exception {
        MvcResult result = mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": %d, "days": %d, "refresh_every": 0, "locations": ["homepage"]}
                                """.formatted(type, days)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void verifyNoSponsorCalls() {
        verify(olxApiClient, never()).getSponsorPrice(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyList());
        verify(olxApiClient, never()).sponsorListing(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyList());
    }

    private Sponsorship row(long ownerId, long listingId, OffsetDateTime startedAt, OffsetDateTime endsAt) {
        return new Sponsorship(ownerId, accountId, listingId, 1, 7, 0, "homepage",
                new BigDecimal("108"), startedAt, endsAt);
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
                .thenReturn(new OlxLoginResult(TOKEN, 555L));
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
