package com.meingehirn.detection_svc.service;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.WebhookConfig;
import com.meingehirn.detection_svc.repository.WebhookConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String[] BLOCKED_PREFIXES = {"10.", "172.16.", "192.168.", "127.", "0."};

    private final WebhookConfigRepository webhookConfigRepository;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fraudguard.webhook.enabled:true}")
    private boolean webhookEnabled;

    @Async
    public void deliver(DetectionResult result) {
        if (!webhookEnabled) {
            return;
        }
        if (result.getTenantId() == null) {
            return;
        }

        List<WebhookConfig> hooks = webhookConfigRepository
                .findByTenantIdAndEnabledTrue(result.getTenantId());

        if (hooks.isEmpty()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(result);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            for (WebhookConfig hook : hooks) {
                if (hook.getSecret() != null && !hook.getSecret().isBlank()) {
                    headers.set("X-Webhook-Secret", hook.getSecret());
                }
                deliverToHook(hook.getUrl(), payload, headers, result.getTxId());
            }
        } catch (Exception e) {
            log.error("event=webhook_serialize_fail txId={}", result.getTxId(), e);
        }
    }

    private void deliverToHook(String url, String payload, HttpHeaders headers, Long txId) {
        try {
            URI uri = URI.create(url);
            if (!isAllowedUrl(uri)) {
                log.warn("event=webhook_ssrf_blocked url={} txId={}", url, txId);
                return;
            }

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            webhookRestTemplate.postForEntity(uri, entity, String.class);
            log.info("event=webhook_delivered url={} txId={}", url, txId);
        } catch (RestClientException e) {
            log.warn("event=webhook_delivery_fail url={} txId={} error={}", url, txId, e.getMessage());
        }
    }

    boolean isAllowedUrl(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }

        for (String prefix : BLOCKED_PREFIXES) {
            if (host.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }

    public WebhookConfig register(String tenantId, String url, String secret) {
        URI uri = URI.create(url);
        if (!isAllowedUrl(uri)) {
            throw new IllegalArgumentException("URL is not allowed: must be HTTPS and not a private address");
        }

        WebhookConfig config = WebhookConfig.builder()
                .tenantId(tenantId)
                .url(url)
                .secret(secret)
                .enabled(true)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return webhookConfigRepository.save(config);
    }

    public List<WebhookConfig> list(String tenantId) {
        return webhookConfigRepository.findByTenantId(tenantId);
    }

    public void delete(Long id) {
        webhookConfigRepository.findById(id).ifPresent(webhookConfigRepository::delete);
    }
}
