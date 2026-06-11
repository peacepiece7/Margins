package com.margins;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.margins.session.controller.ReadingSessionController;
import com.margins.session.controller.SessionWindowController;
import com.margins.session.service.ReadingSessionService;
import com.margins.session.service.SessionWindowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ReadingSessionController.class, SessionWindowController.class})
class SessionControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReadingSessionService readingSessionService;

    @MockBean
    private SessionWindowService sessionWindowService;

    @Test
    void readingSessionRejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/api/reading-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(readingSessionService);
    }

    @Test
    void sessionWindowRejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/api/session-windows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"windowType\":\"question\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(sessionWindowService);
    }

    @Test
    void messageRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/session-windows/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(sessionWindowService);
    }

    @Test
    void debateRejectsMissingPersona() throws Exception {
        mockMvc.perform(post("/api/session-windows/1/debate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hello\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(sessionWindowService);
    }

}
