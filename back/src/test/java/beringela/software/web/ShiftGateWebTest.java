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
class ShiftGateWebTest {

    @Autowired
    WebApplicationContext context;

    final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    private void ensureClockedOut(String token) throws Exception {
        int status = mockMvc.perform(get("/shifts/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
        if (status == 200) {
            mockMvc.perform(post("/shifts/clock-out").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void kitchenNeedsClockInToSeeQueue() throws Exception {
        String token = login("demo", "maria@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(get("/kitchen/queue").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/kitchen/queue").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void kitchenNeedsClockInToListOrders() throws Exception {
        String token = login("demo", "maria@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void waiterNeedsClockInToSendToKitchen() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(post("/shifts/clock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Turno teste\"}"))
                .andExpect(status().isCreated());

        String tablesJson = mockMvc.perform(get("/tables").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tableId = objectMapper.readTree(tablesJson).get(0).get("id").asText();

        String orderJson = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":\"" + tableId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderJson).get("id").asText();

        String menuJson = mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String menuItemId = objectMapper.readTree(menuJson).get(0).get("id").asText();

        mockMvc.perform(post("/orders/" + orderId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuItemId\":\"" + menuItemId + "\",\"quantity\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/" + orderId + "/send-to-kitchen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void waiterBlockedAfterClockOut() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shifts/clock-out")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Saída\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void waiterCannotSeeAnythingWithoutClockIn() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(get("/tables").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/menu-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shiftNotesVisibleToSelfAndManager() throws Exception {
        String waiterToken = login("demo", "joao@demo.pt", "demo1234");
        ensureClockedOut(waiterToken);

        String shiftJson = mockMvc.perform(post("/shifts/clock-in")
                        .header("Authorization", "Bearer " + waiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Nota privada do empregado\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode shift = objectMapper.readTree(shiftJson);
        org.junit.jupiter.api.Assertions.assertEquals(
                "Nota privada do empregado", shift.get("clockInNotes").asText());

        String managerToken = login("demo", "ana@demo.pt", "demo1234");
        String staffId = shift.get("staffId").asText();
        String historyJson = mockMvc.perform(get("/shifts/history")
                        .param("staffId", staffId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode history = objectMapper.readTree(historyJson);
        org.junit.jupiter.api.Assertions.assertFalse(history.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Nota privada do empregado", history.get(0).get("clockInNotes").asText());
    }

    @Test
    void cannotClockInTwiceWithoutClockOut() throws Exception {
        String token = login("demo", "joao@demo.pt", "demo1234");
        ensureClockedOut(token);

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shifts/clock-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
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
        return objectMapper.readTree(response).get("token").asText();
    }

    private record LoginBody(String slug, String email, String password) {
    }
}
