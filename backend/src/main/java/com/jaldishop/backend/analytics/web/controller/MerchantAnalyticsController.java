package com.jaldishop.backend.analytics.web.controller;

import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.port.in.GetMerchantAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.analytics.web.dto.MerchantAnalyticsSummaryResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/analytics")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
public class MerchantAnalyticsController {

    private final GetMerchantAnalyticsUseCase getMerchantAnalyticsUseCase;
    private final StoreContextService storeContextService;
    private final AnalyticsPdfExportPort analyticsPdfExportPort;

    public MerchantAnalyticsController(
            GetMerchantAnalyticsUseCase getMerchantAnalyticsUseCase,
            StoreContextService storeContextService,
            AnalyticsPdfExportPort analyticsPdfExportPort
    ) {
        this.getMerchantAnalyticsUseCase = getMerchantAnalyticsUseCase;
        this.storeContextService = storeContextService;
        this.analyticsPdfExportPort = analyticsPdfExportPort;
    }

    @GetMapping("/summary")
    public ResponseEntity<MerchantAnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID authenticatedStoreId = storeContextService.requireStoreId(principal);
        if (storeId != null) {
            storeContextService.validateStoreOwnership(storeId, principal);
        }

        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        MerchantAnalyticsReport report = getMerchantAnalyticsUseCase.getMerchantAnalytics(authenticatedStoreId, period);

        return ResponseEntity.ok(MerchantAnalyticsSummaryResponse.from(report));
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID authenticatedStoreId = storeContextService.requireStoreId(principal);
        if (storeId != null) {
            storeContextService.validateStoreOwnership(storeId, principal);
        }

        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        MerchantAnalyticsReport report = getMerchantAnalyticsUseCase.getMerchantAnalytics(authenticatedStoreId, period);
        byte[] pdfBytes = analyticsPdfExportPort.generateMerchantReportPdf(report);

        String filename = "jaldishop-merchant-analytics-" + fromDate + "-al-" + toDate + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
