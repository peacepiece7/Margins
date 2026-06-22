package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "margins.auth.single-user.password=reader"
})
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiSpecIsPublicAndListsMvpRoutes() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        JsonNode paths = root.path("paths");

        assertThat(root.path("info").path("title").asText()).isEqualTo("Margins API");
        assertThat(paths.has("/api/auth/login")).isTrue();
        assertThat(paths.has("/api/reading-sessions")).isTrue();
        assertThat(paths.has("/api/reading-sessions/{id}/metrics/snapshot")).isTrue();
        assertThat(paths.has("/api/session-windows/{id}/messages/stream")).isTrue();
        assertThat(paths.has("/api/personas")).isTrue();
        assertThat(paths.path("/api/session-windows/{id}/debate").path("post").path("requestBody").toString())
            .contains("DebateMessageRequest");
        assertThat(paths.path("/api/session-windows/{id}/debate/all").path("post").path("requestBody").toString())
            .contains("DebateAllMessageRequest")
            .doesNotContain("DebateMessageRequest");
    }

    @Test
    void protectedApiRejectsAnonymousRequestsInFullContext() throws Exception {
        mockMvc.perform(get("/api/reading-sessions/latest"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void validationFailuresUseApiResponseInFullContext() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"reader\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void loginIssuedTokenPassesFullContextAuthFilter() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"peacepiece\",\"password\":\"reader\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String accessToken = objectMapper.readTree(loginBody).path("data").path("accessToken").asText();

        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/protected-contract-probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void loginRejectsInvalidSingleUserCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"wrong-user\",\"password\":\"reader\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"peacepiece\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("invalid username or password"));
    }
}
