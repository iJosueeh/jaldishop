package com.jaldishop.backend.analytics.web.controller;

import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.port.in.GetCustomerAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.analytics.web.dto.CustomerAnalyticsSummaryResponse;
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

@RestController
@RequestMapping("/api/v1/customer/analytics")
@PreAuthorize("hasRole('CUSTOMER')")
@Validated
public class CustomerAnalyticsController {

    private final GetCustomerAnalyticsUseCase getCustomerAnalyticsUseCase;
    private final AnalyticsPdfExportPort analyticsPdfExportPort;

    public CustomerAnalyticsController(
            GetCustomerAnalyticsUseCase getCustomerAnalyticsUseCase,
            AnalyticsPdfExportPort analyticsPdfExportPort
    ) {
        this.getCustomerAnalyticsUseCase = getCustomerAnalyticsUseCase;
        this.analyticsPdfExportPort = analyticsPdfExportPort;
    }

    @GetMapping("/summary")
    public ResponseEntity<CustomerAnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        CustomerAnalyticsReport report = getCustomerAnalyticsUseCase.getCustomerAnalytics(principal.userId(), period);

        return ResponseEntity.ok(CustomerAnalyticsSummaryResponse.from(report));
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        LocalDate toDate = to != null ? to : LocalDate.now(PeriodFilter.DEFAULT_ZONE);
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        PeriodFilter period = PeriodFilter.of(fromDate, toDate);
        CustomerAnalyticsReport report = getCustomerAnalyticsUseCase.getCustomerAnalytics(principal.userId(), period);
        byte[] pdfBytes = analyticsPdfExportPort.generateCustomerReportPdf(report);

        String filename = "jaldishop-customer-analytics-" + fromDate + "-al-" + toDate + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
