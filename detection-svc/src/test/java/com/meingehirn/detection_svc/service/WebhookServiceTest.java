package com.meingehirn.detection_svc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.WebhookConfig;
import com.meingehirn.detection_svc.repository.WebhookConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookConfigRepository webhookConfigRepository;

    @Mock
    private RestTemplate webhookRestTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "webhookEnabled", true);
    }

    @Test
    void shouldAllowValidHttpsUrl() {
        assertTrue(webhookService.isAllowedUrl(URI.create("https://example.com/webhook")));
    }

    @Test
    void shouldRejectHttpUrl() {
        assertFalse(webhookService.isAllowedUrl(URI.create("http://example.com/webhook")));
    }

    @Test
    void shouldRejectPrivateIp10() {
        assertFalse(webhookService.isAllowedUrl(URI.create("https://10.0.0.1/webhook")));
    }

    @Test
    void shouldRejectPrivateIp192() {
        assertFalse(webhookService.isAllowedUrl(URI.create("https://192.168.1.1/webhook")));
    }

    @Test
    void shouldRejectPrivateIp172() {
        assertFalse(webhookService.isAllowedUrl(URI.create("https://172.16.0.1/webhook")));
    }

    @Test
    void shouldRejectLocalhost() {
        assertFalse(webhookService.isAllowedUrl(URI.create("https://127.0.0.1/webhook")));
    }

    @Test
    void shouldRejectEmptyHost() {
        assertThrows(IllegalArgumentException.class, () ->
                webhookService.isAllowedUrl(URI.create("https://")));
    }

    @Test
    void shouldRegisterWebhookWithValidUrl() {
        when(webhookConfigRepository.save(any(WebhookConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WebhookConfig config = webhookService.register("TENANT_A",
                "https://example.com/webhook", "secret123");

        assertTrue(config.isEnabled());
        assertTrue(config.getUrl().contains("example.com"));
        verify(webhookConfigRepository).save(any(WebhookConfig.class));
    }

    @Test
    void shouldThrowWhenRegisteringInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                webhookService.register("TENANT_A", "http://example.com/webhook", null));
    }

    @Test
    void shouldListWebhooksByTenant() {
        when(webhookConfigRepository.findByTenantId("TENANT_A")).thenReturn(List.of());

        List<WebhookConfig> hooks = webhookService.list("TENANT_A");

        assertTrue(hooks.isEmpty());
        verify(webhookConfigRepository).findByTenantId("TENANT_A");
    }

    @Test
    void shouldSkipDeliverWhenWebhookDisabled() {
        ReflectionTestUtils.setField(webhookService, "webhookEnabled", false);

        var result = DetectionResult.builder().txId(1L).tenantId("TENANT_A").build();
        webhookService.deliver(result);

        verifyNoInteractions(webhookConfigRepository);
    }

    @Test
    void shouldSkipDeliverWhenNoTenantId() throws Exception {
        var result = DetectionResult.builder().txId(1L).build();
        webhookService.deliver(result);

        verifyNoInteractions(objectMapper, webhookConfigRepository, webhookRestTemplate);
    }

    @Test
    void shouldDeliverToEnabledWebhooks() throws Exception {
        var hook = WebhookConfig.builder()
                .id(1L).tenantId("TENANT_A").url("https://example.com/webhook")
                .enabled(true).build();
        var result = DetectionResult.builder().txId(1L).tenantId("TENANT_A")
                .isFlaggedFraud(true).triggeredRules(List.of("FraudCheck")).build();

        when(webhookConfigRepository.findByTenantIdAndEnabledTrue("TENANT_A"))
                .thenReturn(List.of(hook));
        when(objectMapper.writeValueAsString(result)).thenReturn("{\"txId\":1}");
        when(webhookRestTemplate.postForEntity(any(URI.class), any(), any()))
                .thenReturn(null);

        webhookService.deliver(result);

        verify(objectMapper).writeValueAsString(result);
        verify(webhookRestTemplate).postForEntity(any(URI.class), any(), any());
    }
}
