package com.triage.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triage.agent.TriageAgentService;
import com.triage.agent.TriageDecision;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private TriageAgentService triageAgentService;

    @Test
    void submitTicket_returnsDecisionFromAgent() throws Exception {
        TriageDecision decision = new TriageDecision(
                TriageDecision.Action.REPLY, 0.85, "Here's your answer", "Grounded in policy");
        when(triageAgentService.triage(any(Ticket.class))).thenReturn(decision);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketRequest request = new TicketRequest("CUST-1", "Where is my order?", "ORD-1001 status?");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPLIED"))
                .andExpect(jsonPath("$.confidence").value(0.85))
                .andExpect(jsonPath("$.draftResponse").value("Here's your answer"));
    }

    @Test
    void submitTicket_rejectsBlankFields() throws Exception {
        String invalidJson = "{\"customerId\": \"\", \"subject\": \"\", \"body\": \"\"}";

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTicket_returnsNotFoundWhenMissing() throws Exception {
        when(ticketRepository.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/tickets/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitTicket_returns503WhenAiProviderRateLimited() throws Exception {
        when(triageAgentService.triage(any(Ticket.class)))
                .thenThrow(new NonTransientAiException("HTTP 429 - rate limit exceeded"));

        TicketRequest request = new TicketRequest("CUST-1", "Where is my order?", "ORD-1001 status?");

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(
                        "The AI provider is temporarily rate-limited or unavailable. Please try again shortly."));
    }
}
