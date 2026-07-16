package com.meingehirn.detection_svc.service.strategy;

import com.meingehirn.detection_svc.model.entity.Transaction;

public interface DetectionStrategy {
    String getName();
    boolean detectFraud(Transaction tx);
}
