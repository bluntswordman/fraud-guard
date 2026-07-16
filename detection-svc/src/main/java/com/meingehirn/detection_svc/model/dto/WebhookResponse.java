package com.meingehirn.detection_svc.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private Long id;
    private String tenantId;
    private String url;
    private boolean enabled;
    private LocalDateTime createdAt;

    public static WebhookResponse from(com.meingehirn.detection_svc.model.entity.WebhookConfig config) {
        return WebhookResponse.builder()
                .id(config.getId())
                .tenantId(config.getTenantId())
                .url(config.getUrl())
                .enabled(config.isEnabled())
                .createdAt(config.getCreatedAt())
                .build();
    }
}
