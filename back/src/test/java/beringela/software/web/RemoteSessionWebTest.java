package beringela.software.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("dev")
class RemoteSessionWebTest {

    @Autowired
    WebApplicationContext context;

    final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void superadminStartsRemoteSessionAndAccessesTenantApis() throws Exception {
        String adminToken = platformLogin();
        String tenantId = findDemoTenantId(adminToken);

        String sessionBody = "{\"reason\":\"Suporte - mesas bloqueadas\",\"durationMinutes\":60}";
        String sessionResponse = mockMvc.perform(post("/platform/tenants/" + tenantId + "/remote-session")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(sessionResponse);
        String remoteToken = session.get("token").asText();
        String sessionId = session.get("sessionId").asText();

        mockMvc.perform(get("/tables").header("Authorization", "Bearer " + remoteToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/reports/waiters").header("Authorization", "Bearer " + remoteToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/platform/tenants/" + tenantId + "/remote-session/" + sessionId + "/end")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tables").header("Authorization", "Bearer " + remoteToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void platformHealthOverviewIsAccessible() throws Exception {
        String adminToken = platformLogin();
        mockMvc.perform(get("/platform/health").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String findDemoTenantId(String adminToken) throws Exception {
        String response = mockMvc.perform(get("/platform/tenants")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode tenants = objectMapper.readTree(response);
        for (JsonNode tenant : tenants) {
            if ("demo".equals(tenant.get("slug").asText())) {
                return tenant.get("id").asText();
            }
        }
        throw new IllegalStateException("Demo tenant not found");
    }

    private String platformLogin() throws Exception {
        String body = objectMapper.writeValueAsString(
                new LoginBody("superadmin@pratten.pt", "superadmin1234"));
        String response = mockMvc.perform(post("/platform/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private record LoginBody(String email, String password) {
    }
}
