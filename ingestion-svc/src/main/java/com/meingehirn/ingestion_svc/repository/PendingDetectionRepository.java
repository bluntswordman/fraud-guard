package com.meingehirn.ingestion_svc.repository;

import com.meingehirn.ingestion_svc.model.entity.PendingDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingDetectionRepository extends JpaRepository<PendingDetection, Long> {
}
