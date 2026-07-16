package com.meingehirn.detection_svc.repository;

import com.meingehirn.detection_svc.model.entity.WebhookConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByTenantIdAndEnabledTrue(String tenantId);

    List<WebhookConfig> findByTenantId(String tenantId);
}
