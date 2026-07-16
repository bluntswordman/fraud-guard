package com.meingehirn.ingestion_svc.service;

import com.meingehirn.ingestion_svc.exception.InvalidCsvException;
import com.meingehirn.ingestion_svc.model.dto.EvaluationResponse;
import com.meingehirn.ingestion_svc.model.dto.StatsResponse;
import com.meingehirn.ingestion_svc.model.dto.TransactionDetailResponse;
import com.meingehirn.ingestion_svc.model.dto.TransactionRequest;
import com.meingehirn.ingestion_svc.model.dto.UploadResponse;
import com.meingehirn.ingestion_svc.model.entity.PendingDetection;
import com.meingehirn.ingestion_svc.model.entity.Transaction;
import com.meingehirn.ingestion_svc.repository.PendingDetectionRepository;
import com.meingehirn.ingestion_svc.repository.TransactionRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final int BATCH_SIZE = 1000;

    private final TransactionRepository repository;
    private final PendingDetectionRepository pendingDetectionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fraudguard.redis.channel:fraudguard.tx.channel}")
    private String channel;

    public UploadResponse processCsv(MultipartFile file) {
        validateFile(file);

        String fileName = extractSafeFileName(file);
        int totalRows = 0;
        int skippedRows = 0;
        int processedRows = 0;
        int publishedEvents = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new InvalidCsvException("CSV file is empty or has no header", "ING-002");
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerLine);

            List<Transaction> batch = new ArrayList<>(BATCH_SIZE);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                Transaction tx = parseRow(line, headerMap);
                if (tx == null) {
                    skippedRows++;
                    continue;
                }
                batch.add(tx);

                if (batch.size() >= BATCH_SIZE) {
                    List<Transaction> valid = filterAndTransform(batch);
                    skippedRows += (batch.size() - valid.size());
                    List<Transaction> saved = persistBatch(valid, null);
                    processedRows += saved.size();
                    publishedEvents += publishToRedis(saved);
                    batch.clear();
                    log.info("Batch processed: {} rows saved, {}/{} total",
                            saved.size(), processedRows, totalRows);
                }
            }

            if (!batch.isEmpty()) {
                List<Transaction> valid = filterAndTransform(batch);
                skippedRows += (batch.size() - valid.size());
                List<Transaction> saved = persistBatch(valid, null);
                processedRows += saved.size();
                publishedEvents += publishToRedis(saved);
            }
        } catch (InvalidCsvException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV", e);
            throw new InvalidCsvException("Failed to read CSV file: " + e.getMessage(), "ING-003");
        }

        log.info("CSV {} processed: total={}, processed={}, skipped={}, published={}",
                fileName, totalRows, processedRows, skippedRows, publishedEvents);

        return UploadResponse.success(fileName, totalRows, processedRows, skippedRows, publishedEvents);
    }

    public UploadResponse ingestSingle(TransactionRequest request) {
        Transaction tx = mapToEntity(request);
        List<Transaction> saved = persistBatch(List.of(tx), request.getRules());
        int published = publishToRedis(saved);
        return UploadResponse.success("single", 1, saved.size(), 0, published);
    }

    private Transaction mapToEntity(TransactionRequest request) {
        Transaction tx = Transaction.builder()
                .step(request.getStep())
                .type(request.getType())
                .amount(request.getAmount())
                .nameOrig(request.getNameOrig())
                .nameDest(request.getNameDest())
                .oldBalanceOrg(request.getOldBalanceOrg())
                .newBalanceOrig(request.getNewBalanceOrig())
                .isFraud(request.getIsFraud() != null ? request.getIsFraud() : false)
                .isFlaggedFraud(false)
                .tenantId(request.getTenantId())
                .build();
        return tx;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidCsvException("File is empty", "ING-001");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new InvalidCsvException("Invalid file format. Please upload a CSV file.", "ING-001");
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equals("text/csv")
                && !contentType.equals("application/vnd.ms-excel")) {
            log.warn("Unexpected content type: {}, continuing with extension check", contentType);
        }
    }

    private String extractSafeFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "unknown.csv";
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private Map<String, Integer> buildHeaderMap(String headerLine) {
        String[] headers = headerLine.split(",");
        Map<String, Integer> headerMap = new java.util.HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i].trim().toLowerCase(), i);
        }

        String[] requiredHeaders = {"step", "type", "amount", "nameorig", "namedest"};
        for (String required : requiredHeaders) {
            if (!headerMap.containsKey(required)) {
                throw new InvalidCsvException(
                        "Missing required CSV header: " + required, "ING-003");
            }
        }
        return headerMap;
    }

    private Transaction parseRow(String line, Map<String, Integer> headerMap) {
        try {
            String[] fields = line.split(",", -1);

            String stepVal = getField(fields, headerMap, "step");
            String typeVal = getField(fields, headerMap, "type");
            String amountVal = getField(fields, headerMap, "amount");
            String nameOrigVal = getField(fields, headerMap, "nameOrig");
            String nameDestVal = getField(fields, headerMap, "nameDest");

            if (stepVal == null || typeVal == null || amountVal == null
                    || nameOrigVal == null || nameDestVal == null) {
                return null;
            }

            Transaction tx = new Transaction();
            tx.setStep(Integer.parseInt(stepVal.trim()));
            tx.setType(typeVal.trim());
            tx.setAmount(new BigDecimal(amountVal.trim()));
            tx.setNameOrig(nameOrigVal.trim());
            tx.setNameDest(nameDestVal.trim());
            tx.setTenantId("PAYSIM");

            String oldBalanceVal = getField(fields, headerMap, "oldbalanceOrg");
            if (oldBalanceVal != null && !oldBalanceVal.isBlank()) {
                tx.setOldBalanceOrg(new BigDecimal(oldBalanceVal.trim()));
            }

            String newBalanceVal = getField(fields, headerMap, "newbalanceOrig");
            if (newBalanceVal != null && !newBalanceVal.isBlank()) {
                tx.setNewBalanceOrig(new BigDecimal(newBalanceVal.trim()));
            }

            String isFraudVal = getField(fields, headerMap, "isFraud");
            if (isFraudVal != null && !isFraudVal.isBlank()) {
                tx.setIsFraud("1".equals(isFraudVal.trim()));
            }

            String isFlaggedVal = getField(fields, headerMap, "isFlaggedFraud");
            if (isFlaggedVal != null && !isFlaggedVal.isBlank()) {
                tx.setIsFlaggedFraud("1".equals(isFlaggedVal.trim()));
            }

            return tx;
        } catch (Exception e) {
            log.debug("Skipping invalid CSV row: {}", e.getMessage());
            return null;
        }
    }

    private String getField(String[] fields, Map<String, Integer> headerMap, String columnName) {
        Integer index = headerMap.get(columnName.toLowerCase());
        if (index == null || index >= fields.length) {
            return null;
        }
        return fields[index];
    }

    private List<Transaction> filterAndTransform(List<Transaction> rawTransactions) {
        return rawTransactions.stream()
                .filter(tx -> tx.getAmount() != null && tx.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(tx -> tx.getType() != null && !tx.getType().isBlank())
                .filter(tx -> tx.getNameOrig() != null && !tx.getNameOrig().isBlank())
                .filter(tx -> tx.getNameDest() != null && !tx.getNameDest().isBlank())
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Transaction> persistBatch(List<Transaction> transactions, List<String> rules) {
        if (transactions.isEmpty()) {
            return List.of();
        }
        List<Transaction> deduplicated = transactions.stream()
                .filter(tx -> !repository.existsByNameOrigAndStepAndAmountAndNameDest(
                        tx.getNameOrig(), tx.getStep(), tx.getAmount(), tx.getNameDest()))
                .collect(Collectors.toList());
        if (deduplicated.isEmpty()) {
            return List.of();
        }
        List<Transaction> saved = repository.saveAll(deduplicated);

        String rulesStr = rules != null && !rules.isEmpty()
                ? String.join(",", rules) : null;

        List<PendingDetection> outboxEntries = saved.stream()
                .map(tx -> PendingDetection.builder()
                        .txId(tx.getId())
                        .status(PendingDetection.Status.PENDING)
                        .createdAt(LocalDateTime.now())
                        .rules(rulesStr)
                        .build())
                .collect(Collectors.toList());
        try {
            pendingDetectionRepository.saveAll(outboxEntries);
        } catch (Exception e) {
            log.warn("Outbox write failed for {} entries, continuing", outboxEntries.size(), e);
        }

        return saved;
    }

    public int publishToRedis(List<Transaction> savedTransactions) {
        int count = 0;
        for (Transaction tx : savedTransactions) {
            try {
                redisTemplate.convertAndSend(channel, String.valueOf(tx.getId()));
                count++;
            } catch (Exception e) {
                log.error("Failed to publish txId={} to Redis", tx.getId(), e);
            }
        }
        log.info("Published {} transactions to Redis channel {}", count, channel);
        return count;
    }

    public TransactionDetailResponse getTransactionById(Long id) {
        return repository.findById(id)
                .map(TransactionDetailResponse::success)
                .orElse(TransactionDetailResponse.notFound(id));
    }

    public StatsResponse getStats(String tenantId) {
        long total;
        long flaggedFraud;
        BigDecimal totalAmount;
        List<Object[]> typeCounts;
        List<Object[]> fraudByTypeCounts;

        if (tenantId != null && !tenantId.isBlank()) {
            total = repository.countAllByTenantId(tenantId);
            flaggedFraud = repository.countFlaggedFraudByTenantId(tenantId);
            totalAmount = repository.sumAllAmountsByTenantId(tenantId);
            typeCounts = repository.countByTypeAndTenantId(tenantId);
            fraudByTypeCounts = repository.countFlaggedFraudByTypeAndTenantId(tenantId);
        } else {
            total = repository.countAll();
            flaggedFraud = repository.countFlaggedFraud();
            totalAmount = repository.sumAllAmounts();
            typeCounts = repository.countByType();
            fraudByTypeCounts = repository.countFlaggedFraudByType();
        }

        Map<String, Long> byType = typeCounts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Map<String, Long> fraudByType = fraudByTypeCounts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        return StatsResponse.success(total, flaggedFraud, totalAmount, byType, fraudByType);
    }

    public EvaluationResponse getEvaluation(String tenantId) {
        List<Object[]> result = tenantId != null && !tenantId.isBlank()
                ? repository.evaluateByTenantId(tenantId)
                : repository.evaluate();
        if (result.isEmpty() || result.get(0) == null) {
            return EvaluationResponse.success(EvaluationResponse.EvaluationData.builder()
                    .truePositives(0).falsePositives(0).trueNegatives(0).falseNegatives(0)
                    .totalGroundTruth(0).precision(0).recall(0).f1Score(0).accuracy(0)
                    .build());
        }

        Object[] row = result.get(0);
        long tp = ((Number) row[0]).longValue();
        long fp = ((Number) row[1]).longValue();
        long tn = ((Number) row[2]).longValue();
        long fn = ((Number) row[3]).longValue();
        long totalGroundTruth = ((Number) row[4]).longValue();

        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = totalGroundTruth > 0 ? (double) tp / totalGroundTruth : 0.0;
        double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;
        double accuracy = (tp + tn + fp + fn) > 0
                ? (double) (tp + tn) / (tp + tn + fp + fn) : 0.0;

        EvaluationResponse.EvaluationData data = EvaluationResponse.EvaluationData.builder()
                .truePositives(tp)
                .falsePositives(fp)
                .trueNegatives(tn)
                .falseNegatives(fn)
                .totalGroundTruth(totalGroundTruth)
                .precision(round(precision))
                .recall(round(recall))
                .f1Score(round(f1))
                .accuracy(round(accuracy))
                .build();

        return EvaluationResponse.success(data);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
