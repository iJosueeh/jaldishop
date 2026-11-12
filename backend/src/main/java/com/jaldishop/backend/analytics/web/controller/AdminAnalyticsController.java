package com.jaldishop.backend.analytics.web.controller;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.port.in.GetAdminPlatformAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.analytics.web.dto.AdminAnalyticsSummaryResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
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
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminAnalyticsController {

    private final GetAdminPlatformAnalyticsUseCase getAdminPlatformAnalyticsUseCase;
    private final AnalyticsPdfExportPort analyticsPdfExportPort;

    public AdminAnalyticsController(
            GetAdminPlatformAnalyticsUseCase getAdminPlatformAnalyticsUseCase,
            AnalyticsPdfExportPort analyticsPdfExportPort
    ) {
        this.getAdminPlatformAnalyticsUseCase = getAdminPlatformAnalyticsUseCase;
        this.analyticsPdfExportPort = analyticsPdfExportPort;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminAnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        AdminPlatformAnalyticsReport report = getAdminPlatformAnalyticsUseCase.execute(period, storeId);

        return ResponseEntity.ok(AdminAnalyticsSummaryResponse.from(report));
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        AdminPlatformAnalyticsReport report = getAdminPlatformAnalyticsUseCase.execute(period, storeId);
        byte[] pdfBytes = analyticsPdfExportPort.generateAdminReportPdf(report);

        String filename = "jaldishop-admin-analytics-" + fromDate + "-al-" + toDate + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
