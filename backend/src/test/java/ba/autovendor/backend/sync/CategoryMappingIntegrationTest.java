package ba.autovendor.backend.sync;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.olx.client.OlxApiClient;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryMappingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    @MockitoBean
    private WooPluginClient wooPluginClient;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createReturnsMappingShape() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.woo_category_id").value(10))
                .andExpect(jsonPath("$.woo_category_name").value("Auto dijelovi"))
                .andExpect(jsonPath("$.olx_category_id").value(6))
                .andExpect(jsonPath("$.olx_category_name").value("Auto dijelovi OLX"));
    }

    @Test
    void createRejectsDuplicateWooCategoryPerUser() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        createMapping(jwtA, 10);

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 7, "Druga")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category already mapped"));

        // The same woo category is fine for another user.
        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(10, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated());
    }

    @Test
    void createValidatesInput() throws Exception {
        String jwt = registerUser("a@test.ba");

        mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"woo_category_id\": null, \"woo_category_name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    void mappingsAreScopedToOwner() throws Exception {
        String jwtA = registerUser("a@test.ba");
        String jwtB = registerUser("b@test.ba");
        long mappingId = createMapping(jwtA, 10);

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mappingId));
    }

    @Test
    void deleteRemovesMapping() throws Exception {
        String jwt = registerUser("a@test.ba");
        long mappingId = createMapping(jwt, 10);

        mockMvc.perform(delete("/sync/mappings/" + mappingId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/sync/mappings"))
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

    private long createMapping(String jwt, long wooCategoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/sync/mappings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson(wooCategoryId, "Auto dijelovi", 6, "Auto dijelovi OLX")))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String mappingJson(long wooId, String wooName, long olxId, String olxName) {
        return """
                {"woo_category_id": %d, "woo_category_name": "%s",
                 "olx_category_id": %d, "olx_category_name": "%s"}
                """.formatted(wooId, wooName, olxId, olxName);
    }
}
