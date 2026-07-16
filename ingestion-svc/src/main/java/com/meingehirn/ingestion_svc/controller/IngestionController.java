package com.meingehirn.ingestion_svc.controller;

import com.meingehirn.ingestion_svc.model.dto.EvaluationResponse;
import com.meingehirn.ingestion_svc.model.dto.StatsResponse;
import com.meingehirn.ingestion_svc.model.dto.TransactionDetailResponse;
import com.meingehirn.ingestion_svc.model.dto.TransactionRequest;
import com.meingehirn.ingestion_svc.model.dto.UploadResponse;
import com.meingehirn.ingestion_svc.service.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadCsv(@RequestParam("file") MultipartFile file) {
        UploadResponse response = ingestionService.processCsv(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UploadResponse> ingestSingle(@Valid @RequestBody TransactionRequest request) {
        UploadResponse response = ingestionService.ingestSingle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDetailResponse> getById(@PathVariable Long id) {
        TransactionDetailResponse response = ingestionService.getTransactionById(id);
        if (response.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(
            @RequestParam(required = false) String tenantId) {
        StatsResponse response = ingestionService.getStats(tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evaluation")
    public ResponseEntity<EvaluationResponse> getEvaluation(
            @RequestParam(required = false) String tenantId) {
        EvaluationResponse response = ingestionService.getEvaluation(tenantId);
        return ResponseEntity.ok(response);
    }
}
