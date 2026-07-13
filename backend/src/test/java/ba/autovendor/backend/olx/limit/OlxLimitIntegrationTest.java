package ba.autovendor.backend.olx.limit;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.olx.client.dto.OlxRefreshLimitsDto;
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
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OlxLimitIntegrationTest {

    private static final String TOKEN = "163|limit-token";

    // Live shape, pinned July 2026 — note the hyphenated keys, the extra
    // "car-parts" category and {limit, unlimited, listings} fields.
    private static final String LIVE_LIMITS_JSON = """
            {"data":{"cars":{"limit":2,"unlimited":false,"listings":0},
                     "real-estate":{"limit":2,"unlimited":false,"listings":1},
                     "car-parts":{"limit":20,"unlimited":false,"listings":0},
                     "other":{"limit":30,"unlimited":false,"listings":9}}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private String jwt;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        jwt = registerUser("limits@test.ba");
        accountId = createAccount(jwt, "limituser");
    }

    @Test
    void listingLimitsMapsLiveOlxShape() throws Exception {
        when(olxApiClient.getListingLimits(TOKEN)).thenReturn(objectMapper.readTree(LIVE_LIMITS_JSON));

        mockMvc.perform(get(base() + "/listing-limits").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cars.used").value(0))
                .andExpect(jsonPath("$.cars.limit").value(2))
                .andExpect(jsonPath("$.real_estate.used").value(1))
                .andExpect(jsonPath("$.real_estate.limit").value(2))
                .andExpect(jsonPath("$.other.used").value(9))
                .andExpect(jsonPath("$.other.limit").value(30));
    }

    @Test
    void listingLimitsToleratesMissingCategories() throws Exception {
        when(olxApiClient.getListingLimits(TOKEN))
                .thenReturn(objectMapper.readTree("""
                        {"data":{"cars":{"limit":2,"unlimited":false,"listings":1}}}
                        """));

        mockMvc.perform(get(base() + "/listing-limits").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cars.used").value(1))
                .andExpect(jsonPath("$.real_estate.used").value(0))
                .andExpect(jsonPath("$.real_estate.limit").value(0))
                .andExpect(jsonPath("$.other.limit").value(0));
    }

    @Test
    void refreshLimitsPassThrough() throws Exception {
        when(olxApiClient.getRefreshLimits(TOKEN))
                .thenReturn(new OlxRefreshLimitsDto(5, 4, 0, 5));

        mockMvc.perform(get(base() + "/listing/refresh/limits").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.free_limit").value(5))
                .andExpect(jsonPath("$.free_count").value(4))
                .andExpect(jsonPath("$.paid_count").value(0))
                .andExpect(jsonPath("$.listing_count").value(5));
    }

    @Test
    void limitsAreScopedToAccountOwner() throws Exception {
        String otherJwt = registerUser("limits-other@test.ba");

        mockMvc.perform(get(base() + "/listing-limits").header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base() + "/listing/refresh/limits").header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base() + "/listing-limits"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upstreamClientErrorSurfacesOlxMessageAs400() throws Exception {
        when(olxApiClient.getListingLimits(TOKEN))
                .thenThrow(new OlxApiException("Limit info nije dostupan", 429));

        mockMvc.perform(get(base() + "/listing-limits").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Limit info nije dostupan"));
    }

    @Test
    void upstreamFailureReturns502() throws Exception {
        when(olxApiClient.getRefreshLimits(TOKEN))
                .thenThrow(new OlxApiException("OLX API is unreachable"));

        mockMvc.perform(get(base() + "/listing/refresh/limits").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("OLX API is unreachable"));
    }

    private String base() {
        return "/olx/accounts/" + accountId;
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
                .thenReturn(new OlxLoginResult(TOKEN, 557L));
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
