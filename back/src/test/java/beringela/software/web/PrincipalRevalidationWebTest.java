package beringela.software.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import beringela.software.domain.StaffMember;
import beringela.software.domain.Tenant;
import beringela.software.repository.StaffMemberRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.tenant.TenantContext;
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
class PrincipalRevalidationWebTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    StaffMemberRepository staffRepository;

    final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
        reactivateJoao();
    }

    @Test
    void deactivatedStaffGetsUnauthorizedWithJsonBody() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");
        deactivateJoao();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unauthenticatedRequestReturnsJsonError() throws Exception {
        mockMvc.perform(get("/tables"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    private void deactivateJoao() {
        Tenant tenant = tenantRepository.findBySlug("demo").orElseThrow();
        TenantContext.set(tenant.getId());
        try {
            StaffMember joao = staffRepository.findByEmailIgnoreCase("joao@demo.pt").orElseThrow();
            joao.setActive(false);
            staffRepository.save(joao);
        } finally {
            TenantContext.clear();
        }
    }

    private void reactivateJoao() {
        Tenant tenant = tenantRepository.findBySlug("demo").orElseThrow();
        TenantContext.set(tenant.getId());
        try {
            staffRepository.findByEmailIgnoreCase("joao@demo.pt").ifPresent(joao -> {
                joao.setActive(true);
                staffRepository.save(joao);
            });
        } finally {
            TenantContext.clear();
        }
    }

    private String login(String slug, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                new LoginBody(slug, email, password));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private record LoginBody(String slug, String email, String password) {
    }
}
