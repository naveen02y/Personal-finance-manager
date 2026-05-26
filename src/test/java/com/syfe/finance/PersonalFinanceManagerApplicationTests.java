package com.syfe.finance;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PersonalFinanceManagerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void userCanManageFinanceWorkflowEndToEnd() throws Exception {
        MockHttpSession session = registerAndLogin("user@example.com");

        mockMvc.perform(get("/api/categories").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[0].name").value("Salary"))
            .andExpect(jsonPath("$.categories[0].isCustom").value(false));

        long freelanceCategoryId = createCategory(session, "Freelance", "INCOME");
        createCategory(session, "SideProject", "EXPENSE");
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("name", "Freelance", "type", "INCOME")))).andExpect(status().isConflict());

        long salaryId = createTransaction(session, 3000, "2024-01-15", "Salary", "January salary");
        createTransaction(session, 500, "2024-01-10", "Freelance", "Client work");
        long foodId = createTransaction(session, 400, "2024-01-20", "Food", "Groceries");

        mockMvc.perform(get("/api/transactions").session(session).param("type", "INCOME"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions", hasSize(2)))
            .andExpect(jsonPath("$.transactions[0].category").value("Salary"));

        mockMvc.perform(get("/api/transactions").session(session)
            .param("categoryId", String.valueOf(freelanceCategoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions", hasSize(1)))
            .andExpect(jsonPath("$.transactions[0].category").value("Freelance"));

        mockMvc.perform(put("/api/transactions/{id}", salaryId).session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("amount", 3500, "description", "Updated salary"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(3500.00))
            .andExpect(jsonPath("$.date").value("2024-01-15"))
            .andExpect(jsonPath("$.description").value("Updated salary"));

        mockMvc.perform(put("/api/transactions/{id}", salaryId).session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("date", "2024-02-01")))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("date cannot be updated")));

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome.Salary").value(3500.00))
            .andExpect(jsonPath("$.totalIncome.Freelance").value(500.00))
            .andExpect(jsonPath("$.totalExpenses.Food").value(400.00))
            .andExpect(jsonPath("$.netSavings").value(3600.00));

        long goalId = createGoal(session);
        mockMvc.perform(get("/api/goals/{id}", goalId).session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.currentProgress").value(3600.00))
            .andExpect(jsonPath("$.progressPercentage").value(72.00))
            .andExpect(jsonPath("$.remainingAmount").value(1400.00));

        mockMvc.perform(delete("/api/transactions/{id}", foodId).session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));

        mockMvc.perform(get("/api/reports/yearly/2024").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome.Salary").value(3500.00))
            .andExpect(jsonPath("$.totalExpenses.Food").doesNotExist())
            .andExpect(jsonPath("$.netSavings").value(4000.00));

        mockMvc.perform(delete("/api/categories/Food").session(session)).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/categories/Freelance").session(session)).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/categories/SideProject").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    void sessionsEnforceUserDataIsolation() throws Exception {
        MockHttpSession firstUser = registerAndLogin("first@example.com");
        long transactionId = createTransaction(firstUser, 1000, "2024-03-10", "Salary", "March salary");
        long goalId = createGoal(firstUser);
        createCategory(firstUser, "Secret", "EXPENSE");

        MockHttpSession secondUser = registerAndLogin("second@example.com");
        mockMvc.perform(get("/api/goals/{id}", goalId).session(secondUser)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/categories/Secret").session(secondUser)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/transactions/{id}", transactionId).session(secondUser))
            .andExpect(status().isForbidden());
    }

    @Test
    void validationAndAuthenticationFailuresReturnClientErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", "not-an-email", "password", "short", "fullName", "Bad User",
                    "phoneNumber", "123"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", "login@example.com", "password", "password123", "fullName", "Login User",
                    "phoneNumber", "+1234567890"))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", "login@example.com", "password", "wrong-password"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));

        MockHttpSession session = login("login@example.com", "password123");
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("amount", 10, "date", "2999-01-01", "category", "Food"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/goals").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("goalName", "Invalid", "targetAmount", -1, "targetDate", "2027-01-01"))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isOk());
    }

    private MockHttpSession registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", username, "password", "password123", "fullName", "Test User",
                    "phoneNumber", "+1234567890"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("User registered successfully"));
        return login(username, "password123");
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long createCategory(MockHttpSession session, String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("name", name, "type", type))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(name))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTransaction(MockHttpSession session, int amount, String date, String category, String description)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("amount", amount, "date", date, "category", category, "description", description))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value(category))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createGoal(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/goals").session(session).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("goalName", "Emergency Fund", "targetAmount", 5000, "targetDate", "2027-01-01",
                    "startDate", "2024-01-01"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
