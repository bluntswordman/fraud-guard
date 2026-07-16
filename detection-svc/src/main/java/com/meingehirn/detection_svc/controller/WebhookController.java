package com.meingehirn.detection_svc.controller;

import com.meingehirn.detection_svc.model.dto.WebhookRegisterRequest;
import com.meingehirn.detection_svc.model.dto.WebhookResponse;
import com.meingehirn.detection_svc.model.entity.WebhookConfig;
import com.meingehirn.detection_svc.service.WebhookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<WebhookResponse> register(@Valid @RequestBody WebhookRegisterRequest request) {
        WebhookConfig config = webhookService.register(
                request.getTenantId(), request.getUrl(), request.getSecret());
        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookResponse.from(config));
    }

    @GetMapping
    public ResponseEntity<List<WebhookResponse>> list(@RequestParam("tenantId") String tenantId) {
        List<WebhookResponse> hooks = webhookService.list(tenantId).stream()
                .map(WebhookResponse::from)
                .toList();
        return ResponseEntity.ok(hooks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        webhookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
