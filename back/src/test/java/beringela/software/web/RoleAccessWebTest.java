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
class RoleAccessWebTest {

    @Autowired
    WebApplicationContext context;

    final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void waiterReadsCatalogAndTablesButNotReports() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");

        mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/shifts/clock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Início do turno da noite\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/tables").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/reports/waiters").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Teste\",\"displayOrder\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void kitchenReadsOrdersAndKitchenButNotCatalogOrTables() throws Exception {
        String token = login("demo", "maria@demo.pt", "demo1234");

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/kitchen/queue").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/tables").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reports/waiters").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void kitchenCannotCreateOrders() throws Exception {
        String token = login("demo", "maria@demo.pt", "demo1234");

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"teste\"}"))
                .andExpect(status().isForbidden());
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
