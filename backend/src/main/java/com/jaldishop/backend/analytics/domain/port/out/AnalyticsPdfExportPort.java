package com.jaldishop.backend.analytics.domain.port.out;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;

public interface AnalyticsPdfExportPort {

    byte[] generateCustomerReportPdf(CustomerAnalyticsReport report);

    byte[] generateMerchantReportPdf(MerchantAnalyticsReport report);

    byte[] generateAdminReportPdf(AdminPlatformAnalyticsReport report);
}
