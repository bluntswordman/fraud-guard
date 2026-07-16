package com.meingehirn.detection_svc.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRegisterRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String url;

    private String secret;
}
