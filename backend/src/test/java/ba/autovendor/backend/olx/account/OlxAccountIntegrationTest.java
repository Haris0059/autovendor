package ba.autovendor.backend.olx.account;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.InvalidOlxCredentialsException;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.user.UserRepository;
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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
class OlxAccountIntegrationTest {

    private static final String OLX_USERNAME = "olxuser";
    private static final String OLX_PASSWORD = "olx-secret-pass";
    private static final String OLX_TOKEN = "163|fake-sanctum-token";
    private static final Long OLX_USER_ID = 555L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OlxAccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createReturnsAccountWithoutSecrets() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();

        mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(OLX_USERNAME, OLX_PASSWORD, 5L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(OLX_USERNAME))
                .andExpect(jsonPath("$.olx_user_id").value(OLX_USER_ID))
                .andExpect(jsonPath("$.default_city_id").value(5))
                .andExpect(jsonPath("$.token_expires_at").isNotEmpty())
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.encrypted_password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        OlxAccount saved = accountRepository.findAll().getFirst();
        assertThat(saved.getTokenExpiresAt())
                .isAfter(OffsetDateTime.now().plusDays(29))
                .isBefore(OffsetDateTime.now().plusDays(31));
    }

    @Test
    void createStoresCredentialsEncrypted() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);

        byte[] rawPassword = jdbcTemplate.queryForObject(
                "SELECT encrypted_password FROM olx_accounts LIMIT 1", byte[].class);
        byte[] rawToken = jdbcTemplate.queryForObject(
                "SELECT token_ciphertext FROM olx_accounts LIMIT 1", byte[].class);

        assertThat(rawPassword).isNotEqualTo(OLX_PASSWORD.getBytes(StandardCharsets.UTF_8));
        assertThat(new String(rawPassword, StandardCharsets.ISO_8859_1)).doesNotContain(OLX_PASSWORD);
        assertThat(rawToken).isNotEqualTo(OLX_TOKEN.getBytes(StandardCharsets.UTF_8));
        assertThat(new String(rawToken, StandardCharsets.ISO_8859_1)).doesNotContain(OLX_TOKEN);

        OlxAccount decrypted = accountRepository.findAll().getFirst();
        assertThat(decrypted.getPassword()).isEqualTo(OLX_PASSWORD);
        assertThat(decrypted.getToken()).isEqualTo(OLX_TOKEN);
    }

    @Test
    void createRejectsInvalidOlxCredentials() throws Exception {
        String jwt = registerUser("a@test.ba");
        when(olxApiClient.login(anyString(), anyString()))
                .thenThrow(new InvalidOlxCredentialsException("Invalid OLX credentials"));

        mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(OLX_USERNAME, "wrong", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid OLX credentials"));

        assertThat(accountRepository.count()).isZero();
    }

    @Test
    void createRejectsDuplicateUsernameForSameUser() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(OLX_USERNAME, OLX_PASSWORD, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("OLX account already added"));
    }

    @Test
    void createValidatesInput() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\", \"password\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));

        verify(olxApiClient, never()).login(anyString(), anyString());
    }

    @Test
    void accountsAreScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        stubLoginSuccess();
        long accountId = createAccount(jwtA, OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(get("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/olx/accounts")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/olx/accounts")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId));
    }

    @Test
    void updateCityOnlyDoesNotCallOlx() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        long accountId = createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);
        verify(olxApiClient).login(OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(put("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"default_city_id\": 42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default_city_id").value(42));

        verify(olxApiClient).login(anyString(), anyString());
    }

    @Test
    void updateUsernameReLogsInWithStoredPassword() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        long accountId = createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(put("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"newuser\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));

        verify(olxApiClient).login("newuser", OLX_PASSWORD);
    }

    @Test
    void updatePasswordReLogsInWithNewPassword() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        long accountId = createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(put("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"new-olx-pass\"}"))
                .andExpect(status().isOk());

        verify(olxApiClient).login(OLX_USERNAME, "new-olx-pass");
        assertThat(accountRepository.findAll().getFirst().getPassword()).isEqualTo("new-olx-pass");
    }

    @Test
    void deleteRemovesAccount() throws Exception {
        String jwt = registerUser("a@test.ba");
        stubLoginSuccess();
        long accountId = createAccount(jwt, OLX_USERNAME, OLX_PASSWORD);

        mockMvc.perform(delete("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/olx/accounts/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/olx/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Not authenticated"));
    }

    private void stubLoginSuccess() {
        when(olxApiClient.login(anyString(), anyString()))
                .thenReturn(new OlxLoginResult(OLX_TOKEN, OLX_USER_ID));
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

    private long createAccount(String jwt, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/olx/accounts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(username, password, null)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String accountJson(String username, String password, Long defaultCityId) {
        return """
                {"username": "%s", "password": "%s", "default_city_id": %s}
                """.formatted(username, password, defaultCityId);
    }
}
