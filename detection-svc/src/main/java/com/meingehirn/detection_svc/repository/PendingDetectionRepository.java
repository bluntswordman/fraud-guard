package com.meingehirn.detection_svc.repository;

import com.meingehirn.detection_svc.model.entity.PendingDetection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingDetectionRepository extends JpaRepository<PendingDetection, Long> {

    Optional<PendingDetection> findByTxId(Long txId);

    List<PendingDetection> findByStatusAndCreatedAtBefore(
            PendingDetection.Status status, LocalDateTime cutoff);
}
