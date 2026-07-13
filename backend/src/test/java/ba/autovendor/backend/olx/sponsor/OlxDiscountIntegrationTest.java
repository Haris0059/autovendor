package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OlxDiscountIntegrationTest {

    private static final String TOKEN = "163|discount-token";
    private static final long LISTING_ID = 700L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private String jwt;
    private long accountId;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        discountRepository.deleteAll();
        userRepository.deleteAll();
        jwt = registerUser("discount@test.ba");
        accountId = createAccount(jwt, "discountuser");
        userId = userRepository.findByEmail("discount@test.ba").orElseThrow().getId();
    }

    @Test
    void createDiscountSendsOnlyDiscountPriceToOlx() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100.00, "discount_price": 80.50, "days": 3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing_id").value(LISTING_ID))
                .andExpect(jsonPath("$.account_id").value(accountId))
                .andExpect(jsonPath("$.original_price").value(100.00))
                .andExpect(jsonPath("$.discount_price").value(80.50))
                .andExpect(jsonPath("$.days").value(3))
                .andExpect(jsonPath("$.started_at").isNotEmpty())
                .andExpect(jsonPath("$.ends_at").isNotEmpty());

        ArgumentCaptor<BigDecimal> price = ArgumentCaptor.forClass(BigDecimal.class);
        verify(olxApiClient).createDiscount(eq(TOKEN), eq(LISTING_ID), price.capture(), eq(3));
        assertThat(price.getValue()).isEqualByComparingTo("80.50");

        Discount saved = discountRepository.findAll().getFirst();
        assertThat(saved.getOriginalPrice()).isEqualByComparingTo("100.00");
        assertThat(saved.getDiscountPrice()).isEqualByComparingTo("80.50");
    }

    @Test
    void createDiscountRejectsInvalidDays() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100, "discount_price": 80, "days": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("days")));

        verify(olxApiClient, never()).createDiscount(anyString(), anyLong(), any(), anyInt());
    }

    @Test
    void createDiscountRejectsPriceNotBelowOriginal() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100, "discount_price": 100, "days": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("below")));

        verify(olxApiClient, never()).createDiscount(anyString(), anyLong(), any(), anyInt());
    }

    @Test
    void createDiscountRejectsNonPositivePrice() throws Exception {
        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100, "discount_price": -5, "days": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("greater than zero")));

        verify(olxApiClient, never()).createDiscount(anyString(), anyLong(), any(), anyInt());
    }

    @Test
    void createDiscountForOtherUsersAccountReturns404() throws Exception {
        String otherJwt = registerUser("discount-other@test.ba");

        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + otherJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100, "discount_price": 80, "days": 3}
                                """))
                .andExpect(status().isNotFound());

        verify(olxApiClient, never()).createDiscount(anyString(), anyLong(), any(), anyInt());
    }

    @Test
    void createDiscountUpstream4xxMapsTo400AndPersistsNothing() throws Exception {
        doThrow(new OlxApiException("Sniženje nije moguće", 422))
                .when(olxApiClient).createDiscount(anyString(), anyLong(), any(), anyInt());

        mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": 100, "discount_price": 80, "days": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Sniženje nije moguće"));

        assertThat(discountRepository.findAll()).isEmpty();
    }

    @Test
    void createDiscountSupersedesActiveRowForSameListing() throws Exception {
        createDiscount("100", "90", 3);
        createDiscount("100", "80", 7);

        mockMvc.perform(get("/olx/discounts").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].discount_price").value(80))
                .andExpect(jsonPath("$[0].days").value(7));

        assertThat(discountRepository.findAll()).hasSize(2);
    }

    @Test
    void listReturnsOnlyActiveOwnedRows() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        Discount active = discountRepository.save(row(userId, 701L, now.minusDays(1), now.plusDays(6)));
        Discount ended = row(userId, 702L, now.minusDays(2), now.plusDays(5));
        ended.end(now.minusDays(1));
        discountRepository.save(ended);
        discountRepository.save(row(userId, 703L, now.minusDays(10), now.minusDays(3)));

        registerUser("discount-list-other@test.ba");
        long otherUserId = userRepository.findByEmail("discount-list-other@test.ba").orElseThrow().getId();
        discountRepository.save(row(otherUserId, 704L, now.minusDays(1), now.plusDays(6)));

        mockMvc.perform(get("/olx/discounts").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(active.getId()))
                .andExpect(jsonPath("$[0].listing_id").value(701));
    }

    @Test
    void finishDiscountCallsOlxAndEndsRow() throws Exception {
        long id = createDiscount("100", "80", 3);

        mockMvc.perform(post("/olx/discounts/" + id + "/finish").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        verify(olxApiClient).finishDiscount(TOKEN, LISTING_ID);
        assertThat(discountRepository.findById(id).orElseThrow().getEndedAt()).isNotNull();

        mockMvc.perform(get("/olx/discounts").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void finishDiscountPropagatesUpstream4xxAndKeepsRow() throws Exception {
        // Unlike the sponsor cancel, discount/finish is documented + live-verified,
        // so OLX errors are meaningful and must not silently end the row.
        long id = createDiscount("100", "80", 3);
        doThrow(new OlxApiException("Sniženje nije aktivno", 422))
                .when(olxApiClient).finishDiscount(TOKEN, LISTING_ID);

        mockMvc.perform(post("/olx/discounts/" + id + "/finish").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Sniženje nije aktivno"));

        assertThat(discountRepository.findById(id).orElseThrow().getEndedAt()).isNull();
    }

    @Test
    void finishOtherUsersDiscountReturns404() throws Exception {
        long id = createDiscount("100", "80", 3);
        String otherJwt = registerUser("discount-finish-other@test.ba");

        mockMvc.perform(post("/olx/discounts/" + id + "/finish").header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());

        verify(olxApiClient, never()).finishDiscount(anyString(), anyLong());
    }

    private String createPath() {
        return "/olx/accounts/" + accountId + "/listings/" + LISTING_ID + "/discount";
    }

    private long createDiscount(String original, String discounted, int days) throws Exception {
        MvcResult result = mockMvc.perform(post(createPath())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"original_price": %s, "discount_price": %s, "days": %d}
                                """.formatted(original, discounted, days)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Discount row(long ownerId, long listingId, OffsetDateTime startedAt, OffsetDateTime endsAt) {
        return new Discount(ownerId, accountId, listingId,
                new BigDecimal("100"), new BigDecimal("80"), 3, startedAt, endsAt);
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
                .thenReturn(new OlxLoginResult(TOKEN, 556L));
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
