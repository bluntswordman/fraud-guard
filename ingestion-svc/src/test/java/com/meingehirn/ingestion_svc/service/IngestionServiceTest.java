package com.meingehirn.ingestion_svc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meingehirn.ingestion_svc.exception.InvalidCsvException;
import com.meingehirn.ingestion_svc.model.entity.Transaction;
import com.meingehirn.ingestion_svc.repository.PendingDetectionRepository;
import com.meingehirn.ingestion_svc.repository.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private PendingDetectionRepository pendingDetectionRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private IngestionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "channel", "fraudguard.tx.channel");
        lenient().when(repository.existsByNameOrigAndStepAndAmountAndNameDest(
                anyString(), anyInt(), any(), anyString())).thenReturn(false);
    }

    @Test
    void shouldProcessValidCsv() {
        String csv = "step,type,amount,nameOrig,nameDest,oldbalanceOrg,newbalanceOrig,isFraud,isFlaggedFraud\n"
                + "1,TRANSFER,181.0,C123,C456,181.0,0.0,0,0\n"
                + "2,CASH_OUT,500.0,C789,M012,500.0,0.0,0,0\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", csv.getBytes());

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.processCsv(file);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getTotalRows()).isEqualTo(2);
        assertThat(response.getData().getProcessedRows()).isEqualTo(2);
        assertThat(response.getData().getSkippedRows()).isZero();
        assertThat(response.getData().getPublishedEvents()).isEqualTo(2);
    }

    @Test
    void shouldSkipInvalidRows() {
        String csv = "step,type,amount,nameOrig,nameDest,oldbalanceOrg,newbalanceOrig,isFraud,isFlaggedFraud\n"
                + "1,TRANSFER,0.0,C123,C456,0.0,0.0,0,0\n"
                + "2,CASH_OUT,500.0,C789,M012,500.0,0.0,0,0\n"
                + "3,,200.0,,M012,200.0,0.0,0,0\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", csv.getBytes());

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.processCsv(file);

        assertThat(response.getData().getTotalRows()).isEqualTo(3);
        assertThat(response.getData().getProcessedRows()).isEqualTo(1);
        assertThat(response.getData().getSkippedRows()).isEqualTo(2);
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.processCsv(file))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void shouldRejectNonCsvFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "data".getBytes());

        assertThatThrownBy(() -> service.processCsv(file))
                .isInstanceOf(InvalidCsvException.class);
    }

    @Test
    void shouldRejectCsvWithoutRequiredHeaders() {
        String csv = "step,amount,nameOrig\n1,100.0,C123\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", csv.getBytes());

        assertThatThrownBy(() -> service.processCsv(file))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("Missing required CSV header");
    }

    @Test
    void shouldPublishAfterPersist() {
        String csv = "step,type,amount,nameOrig,nameDest\n"
                + "1,TRANSFER,100.0,C123,C456\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", csv.getBytes());

        when(repository.saveAll(anyList())).thenAnswer(inv -> {
            List<Transaction> saved = inv.getArgument(0);
            saved.get(0).setId(42L);
            return saved;
        });

        service.processCsv(file);

        verify(repository).saveAll(anyList());
        verify(redisTemplate).convertAndSend("fraudguard.tx.channel", "42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnStats() {
        when(repository.countAll()).thenReturn(3L);
        when(repository.countFlaggedFraud()).thenReturn(1L);
        when(repository.sumAllAmounts()).thenReturn(new BigDecimal("350.00"));
        List<Object[]> typeCounts = new java.util.ArrayList<>();
        typeCounts.add(new Object[]{"TRANSFER", 2L});
        typeCounts.add(new Object[]{"CASH_OUT", 1L});
        when(repository.countByType()).thenReturn(typeCounts);
        List<Object[]> fraudTypeCounts = new java.util.ArrayList<>();
        fraudTypeCounts.add(new Object[]{"TRANSFER", 1L});
        when(repository.countFlaggedFraudByType()).thenReturn(fraudTypeCounts);

        var stats = service.getStats(null);

        assertThat(stats.getStatus()).isEqualTo("success");
        assertThat(stats.getData().getTotalTransactions()).isEqualTo(3);
        assertThat(stats.getData().getTotalFlaggedFraud()).isEqualTo(1);
        assertThat(stats.getData().getTotalClean()).isEqualTo(2);
        assertThat(stats.getData().getTotalAmount()).isEqualByComparingTo("350.00");
        assertThat(stats.getData().getTransactionsByType()).containsEntry("TRANSFER", 2L);
        assertThat(stats.getData().getTransactionsByType()).containsEntry("CASH_OUT", 1L);
        assertThat(stats.getData().getFlaggedFraudByType()).containsEntry("TRANSFER", 1L);
    }

    @Test
    void shouldExtractSafeFileName() {
        String csv = "step,type,amount,nameOrig,nameDest\n1,TRANSFER,100.0,C123,C456\n";
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.csv",
                "text/csv", csv.getBytes());

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.processCsv(file);

        assertThat(response.getData().getFileName()).isEqualTo("passwd.csv");
    }
}
