package com.meingehirn.ingestion_svc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meingehirn.ingestion_svc.exception.GlobalExceptionHandler;
import com.meingehirn.ingestion_svc.model.dto.StatsResponse;
import com.meingehirn.ingestion_svc.model.dto.UploadResponse;
import com.meingehirn.ingestion_svc.service.IngestionService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestionController.class)
@Import(GlobalExceptionHandler.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionService ingestionService;

    @Test
    void shouldUploadCsvSuccessfully() throws Exception {
        when(ingestionService.processCsv(any()))
                .thenReturn(UploadResponse.success("test.csv", 100, 95, 5, 95));

        MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                "text/csv", "step,type,amount,nameOrig,nameDest\n".getBytes());

        mockMvc.perform(multipart("/api/transactions/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fileName").value("test.csv"))
                .andExpect(jsonPath("$.data.processedRows").value(95))
                .andExpect(jsonPath("$.data.skippedRows").value(5));
    }

    @Test
    void shouldReturnStats() throws Exception {
        when(ingestionService.getStats(null))
                .thenReturn(StatsResponse.success(100, 5, new BigDecimal("5000.00"),
                        Map.of("TRANSFER", 50L, "CASH_OUT", 30L, "PAYMENT", 20L),
                        Map.of("TRANSFER", 3L, "CASH_OUT", 2L)));

        mockMvc.perform(get("/api/transactions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalTransactions").value(100))
                .andExpect(jsonPath("$.data.totalFlaggedFraud").value(5))
                .andExpect(jsonPath("$.data.totalClean").value(95))
                .andExpect(jsonPath("$.data.totalAmount").value(5000.00))
                .andExpect(jsonPath("$.data.transactionsByType.TRANSFER").value(50))
                .andExpect(jsonPath("$.data.flaggedFraudByType.TRANSFER").value(3))
                .andExpect(jsonPath("$.data.flaggedFraudByType.CASH_OUT").value(2));
    }
}
