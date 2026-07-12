package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.user.UserRepository;
import ba.autovendor.backend.woo.client.WooPluginClient;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SyncHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    private String jwtA;
    private Long userAId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        userAId = userId("a@test.ba");
        Long userBId = userId("b@test.ba");

        // User A: 25 logs across two accounts/stores and mixed statuses.
        for (int i = 0; i < 25; i++) {
            SyncStatus status = i % 3 == 0 ? SyncStatus.failed
                    : i % 3 == 1 ? SyncStatus.success : SyncStatus.skipped;
            long accountId = i % 2 == 0 ? 1L : 2L;
            long storeId = i % 2 == 0 ? 10L : 20L;
            syncLogRepository.save(new SyncLog(userAId, null, accountId, storeId,
                    "update", status, "log " + i));
        }
        // User B: one log that must never leak into A's history.
        syncLogRepository.save(new SyncLog(userBId, null, 1L, 10L, "create", SyncStatus.success, "b log"));
    }

    @Test
    void historyPaginatesNewestFirst() throws Exception {
        mockMvc.perform(get("/sync/history?page=1&per_page=10")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.per_page").value(10))
                .andExpect(jsonPath("$.last_page").value(3))
                .andExpect(jsonPath("$.data[0].message").value("log 24"))
                .andExpect(jsonPath("$.data[0].product_link_id").value((Object) null));

        mockMvc.perform(get("/sync/history?page=3&per_page=10")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[4].message").value("log 0"));
    }

    @Test
    void historyDefaultsPageSize() throws Exception {
        mockMvc.perform(get("/sync/history")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.per_page").value(20))
                .andExpect(jsonPath("$.last_page").value(2));
    }

    @Test
    void historyFiltersByStatusAccountAndStore() throws Exception {
        mockMvc.perform(get("/sync/history?status=failed&per_page=100")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.data[0].status").value("failed"));

        mockMvc.perform(get("/sync/history?account_id=1&per_page=100")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(13));

        mockMvc.perform(get("/sync/history?store_id=20&per_page=100")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12));

        // Combined: failed logs for account 2 / store 20 (i % 3 == 0 and i odd).
        mockMvc.perform(get("/sync/history?status=failed&account_id=2&store_id=20&per_page=100")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void historyIsScopedToUser() throws Exception {
        String jwtB = login("b@test.ba");
        mockMvc.perform(get("/sync/history?per_page=100")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].message").value("b log"));
    }

    @Test
    void emptyHistoryHasSanePagination() throws Exception {
        syncLogRepository.deleteAll();
        mockMvc.perform(get("/sync/history")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.last_page").value(1));
    }

    @Test
    void historyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/sync/history"))
                .andExpect(status().isUnauthorized());
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

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).map(User::getId).orElseThrow();
    }
}
