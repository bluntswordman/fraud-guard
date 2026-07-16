package com.meingehirn.detection_svc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meingehirn.detection_svc.model.dto.WebhookRegisterRequest;
import com.meingehirn.detection_svc.model.entity.WebhookConfig;
import com.meingehirn.detection_svc.service.WebhookService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WebhookController(webhookService))
                .build();
    }

    @Test
    void shouldRegisterWebhook() throws Exception {
        var config = WebhookConfig.builder()
                .id(1L).tenantId("TENANT_A").url("https://example.com/webhook")
                .enabled(true).createdAt(LocalDateTime.now()).build();
        when(webhookService.register(anyString(), anyString(), any())).thenReturn(config);

        var request = WebhookRegisterRequest.builder()
                .tenantId("TENANT_A").url("https://example.com/webhook")
                .secret("secret123").build();

        mockMvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tenantId").value("TENANT_A"))
                .andExpect(jsonPath("$.url").value("https://example.com/webhook"));
    }

    @Test
    void shouldRejectInvalidRegisterRequest() throws Exception {
        var request = WebhookRegisterRequest.builder()
                .tenantId("").url("").build();

        mockMvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListWebhooks() throws Exception {
        var config = WebhookConfig.builder()
                .id(1L).tenantId("TENANT_A").url("https://example.com/webhook")
                .enabled(true).createdAt(LocalDateTime.now()).build();
        when(webhookService.list("TENANT_A")).thenReturn(List.of(config));

        mockMvc.perform(get("/api/webhooks")
                        .param("tenantId", "TENANT_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("https://example.com/webhook"));
    }

    @Test
    void shouldDeleteWebhook() throws Exception {
        mockMvc.perform(delete("/api/webhooks/1"))
                .andExpect(status().isNoContent());

        verify(webhookService).delete(1L);
    }
}
