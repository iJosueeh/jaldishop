package com.jaldishop.backend.analytics.infrastructure.pdf;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CapacityDemandSummary;
import com.jaldishop.backend.analytics.domain.model.CommercialSummary;
import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CustomerSpendingSummary;
import com.jaldishop.backend.analytics.domain.model.FinancialSummary;
import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.model.ProductSalesRank;
import com.jaldishop.backend.analytics.domain.model.StorePerformanceRank;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class OpenPdfReportGeneratorAdapter implements AnalyticsPdfExportPort {

    private static final int MAX_TABLE_ROWS = 50;
    private static final Color COLOR_PRIMARY = new Color(15, 23, 42);          // Slate 900
    private static final Color COLOR_ACCENT = new Color(37, 99, 235);          // Blue 600
    private static final Color COLOR_MUTED = new Color(100, 116, 139);         // Slate 500
    private static final Color COLOR_CARD_BG = new Color(248, 250, 252);       // Slate 50
    private static final Color COLOR_BORDER = new Color(226, 232, 240);        // Slate 200
    private static final Color COLOR_HEADER_BG = new Color(30, 41, 59);        // Slate 800
    private static final Color COLOR_ALT_ROW = new Color(241, 245, 249);        // Slate 100
    private static final Color COLOR_DISCLOSURE_BG = new Color(254, 249, 195);    // Amber 100
    private static final Color COLOR_DISCLOSURE_BORDER = new Color(251, 191, 36);// Amber 400

    private static final Font FONT_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRIMARY);
    private static final Font FONT_SECTION_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_PRIMARY);
    private static final Font FONT_META_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_MUTED);
    private static final Font FONT_META_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_PRIMARY);
    private static final Font FONT_CARD_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_MUTED);
    private static final Font FONT_CARD_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_ACCENT);
    private static final Font FONT_CARD_SUB = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_PRIMARY);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_PRIMARY);
    private static final Font FONT_TD_MUTED = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, COLOR_MUTED);
    private static final Font FONT_DISCLOSURE_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(146, 64, 14));
    private static final Font FONT_DISCLOSURE_TEXT = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 53, 15));

    private final DecimalFormat currencyFormat;
    private final DecimalFormat numberFormat;
    private final DateTimeFormatter instantFormatter;

    public OpenPdfReportGeneratorAdapter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        this.currencyFormat = new DecimalFormat("S/ #,##0.00", symbols);
        this.numberFormat = new DecimalFormat("#,##0", symbols);
        this.instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(PeriodFilter.DEFAULT_ZONE);
    }

    @Override
    public byte[] generateCustomerReportPdf(CustomerAnalyticsReport report) {
        Instant generatedAt = Instant.now();
        return buildPdf("Reporte de Actividad y Compras", (document, writer) -> {
            addHeaderBlock(document,
                    "Reporte de Actividad y Compras",
                    "Resumen consolidado de transacciones personales en JaldiShop",
                    "Cliente ID",
                    report.userId().toString(),
                    report.period(),
                    generatedAt
            );

            addCustomerSpendingSection(document, report.spending());
            addTopPurchasedProductsSection(document, report.topPurchasedProducts());

            addDisclosuresSection(document, List.of(
                    "Alcance de compras: El reporte incluye únicamente los pedidos realizados con la cuenta del usuario autenticado.",
                    "Estado financiero: Los importes corresponden a órdenes confirmadas y pagadas mediante la pasarela integrada.",
                    "Zona horaria oficial: Todas las fechas y rangos corresponden a la zona horaria America/Lima (UTC-05:00)."
            ));
        });
    }

    @Override
    public byte[] generateMerchantReportPdf(MerchantAnalyticsReport report) {
        Instant generatedAt = Instant.now();
        return buildPdf("Reporte Analítico de Tienda", (document, writer) -> {
            addHeaderBlock(document,
                    "Reporte Analítico de Tienda",
                    "Rendimiento comercial, movimientos de pasarela y demanda de capacidad",
                    "Tienda ID",
                    report.storeId().toString(),
                    report.period(),
                    generatedAt
            );

            addCommercialSection(document, report.commercial());
            addFinancialSection(document, report.financial(), false);
            addCapacitySection(document, report.capacityDemand());
            addTopProductsSection(document, report.topProducts(), "Ranking de Productos Más Vendidos");

            addDisclosuresSection(document, List.of(
                    "Liquidación bancaria: Los movimientos financieros corresponden a transacciones aprobadas y reembolsadas en Mercado Pago. No representan liquidaciones bancarias efectivas ni abonos irrevocables en cuenta bancaria propia.",
                    "Compromiso comercial (GMV): El GMV confirmado representa compromisos comerciales al momento de la confirmación del pedido. Las cancelaciones posteriores no modifican el GMV histórico y se expresan en el volumen comercial activo y volumen de cancelaciones.",
                    "Capacidad de servicios: Las reservas COMMITTED y RELEASED cuantifican eventos transaccionales registrados en el periodo. No constituyen ocupación histórica retroactiva ni garantizan asistencia efectiva al servicio."
            ));
        });
    }

    @Override
    public byte[] generateAdminReportPdf(AdminPlatformAnalyticsReport report) {
        Instant generatedAt = Instant.now();
        return buildPdf("Reporte Analítico Global de Plataforma", (document, writer) -> {
            String scopeLabel = report.filterStoreId() != null ? "Tienda Filtrada" : "Alcance Global";
            String scopeValue = report.filterStoreId() != null ? report.filterStoreId().toString() : "Toda la plataforma JaldiShop";

            addHeaderBlock(document,
                    "Reporte Analítico Global de Plataforma",
                    "Monitoreo ejecutivo comercial, financiero, rankings de tiendas y productos",
                    scopeLabel,
                    scopeValue,
                    report.period(),
                    generatedAt
            );

            addCommercialSection(document, report.commercial());
            addFinancialSection(document, report.financial(), true);
            addCapacitySection(document, report.capacityDemand());
            addStoreRankingsSection(document, report.storeRankings());
            addTopProductsSection(document, report.topProducts(), "Ranking de Productos Globales");

            addDisclosuresSection(document, List.of(
                    "Auditoría financiera: Las cifras reflejan capturas y devoluciones procesadas por la pasarela de pagos. No equivalen a fondos acreditados en cuentas corrientes ni balance contable bancario cerrado.",
                    "Pagos huérfanos: Los pagos aprobados sin pedido vinculado se registran de forma aislada para conciliación y no alteran las ventas comerciales atribuidas a comercios.",
                    "Estabilidad histórica: Las cancelaciones comerciales no recalculan el GMV histórico ya confirmado, garantizando trazabilidad y comparabilidad temporal.",
                    "Capacidad operativa: Las métricas de capacidad miden reservas registradas durante el rango y no representan ocupación retroactiva porcentual."
            ));
        });
    }

    private byte[] buildPdf(String title, DocumentRenderer renderer) {
        Document document = new Document(PageSize.A4, 36, 36, 44, 44);
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfHeaderFooterEventHandler(title));
            document.open();

            renderer.render(document, writer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error al generar el documento PDF", e);
        }
    }

    private void addHeaderBlock(Document doc,
                                String title,
                                String subtitle,
                                String scopeLabel,
                                String scopeValue,
                                PeriodFilter period,
                                Instant generatedAt) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{58, 42});
        headerTable.setSpacingAfter(10);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Paragraph brand = new Paragraph("JALDISHOP PLATFORM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_ACCENT));
        brand.setSpacingAfter(2);
        leftCell.addElement(brand);

        Paragraph mainTitle = new Paragraph(title, FONT_DOC_TITLE);
        mainTitle.setSpacingAfter(2);
        leftCell.addElement(mainTitle);

        Paragraph sub = new Paragraph(subtitle, FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED));
        leftCell.addElement(sub);

        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(COLOR_CARD_BG);
        rightCell.setBorderColor(COLOR_BORDER);
        rightCell.setBorderWidth(1);
        rightCell.setPadding(6);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{36, 64});

        addMetaRow(metaTable, "Periodo:", period.fromDate() + " al " + period.toDate());
        addMetaRow(metaTable, "Zona horaria:", "America/Lima (UTC-05:00)");
        addMetaRow(metaTable, "Generado:", instantFormatter.format(generatedAt));
        addMetaRow(metaTable, scopeLabel + ":", scopeValue);

        rightCell.addElement(metaTable);
        headerTable.addCell(rightCell);

        doc.add(headerTable);
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, FONT_META_LABEL));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingBottom(2);

        PdfPCell vCell = new PdfPCell(new Phrase(value, FONT_META_VALUE));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingBottom(2);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addSectionTitle(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, FONT_SECTION_TITLE);
        p.setSpacingBefore(6);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private void addCustomerSpendingSection(Document doc, CustomerSpendingSummary spending) throws DocumentException {
        addSectionTitle(doc, "Resumen de Compras y Gasto Personal");

        if (spending == null) {
            addEmptyStateBox(doc, "No se registraron movimientos de gasto para el usuario en el periodo.");
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});
        table.setSpacingAfter(8);

        BigDecimal averageTicket = spending.totalOrdersCount() > 0
                ? spending.totalSpentAmount().divide(BigDecimal.valueOf(spending.totalOrdersCount()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        addMetricCard(table, "Gasto Total", formatMoney(spending.totalSpentAmount()), "Pedidos completados");
        addMetricCard(table, "Total de Pedidos", formatCount(spending.totalOrdersCount()), "Órdenes realizadas");
        addMetricCard(table, "Total Reembolsado", formatMoney(spending.totalRefundedAmount()), "Devoluciones acreditadas");
        addMetricCard(table, "Ticket Promedio", formatMoney(averageTicket), "Por orden completada");

        doc.add(table);
    }

    private void addTopPurchasedProductsSection(Document doc, List<ProductSalesRank> products) throws DocumentException {
        addSectionTitle(doc, "Productos Más Comprados");

        if (products == null || products.isEmpty()) {
            addEmptyStateBox(doc, "No se registraron productos comprados en el periodo seleccionado.");
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 42, 22, 13, 15});
        table.setHeaderRows(1);
        table.setSpacingAfter(8);

        addTableHeaderCell(table, "#", Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Producto", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Variante", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Unidades", Element.ALIGN_RIGHT);
        addTableHeaderCell(table, "Gasto Total", Element.ALIGN_RIGHT);

        int count = Math.min(products.size(), MAX_TABLE_ROWS);
        for (int i = 0; i < count; i++) {
            ProductSalesRank p = products.get(i);
            Color rowBg = (i % 2 == 0) ? Color.WHITE : COLOR_ALT_ROW;

            addTableCell(table, String.valueOf(i + 1), Element.ALIGN_CENTER, FONT_TD, rowBg);
            addTableCell(table, p.productName(), Element.ALIGN_LEFT, FONT_TD, rowBg);
            addTableCell(table, p.variantName() != null ? p.variantName() : "—", Element.ALIGN_LEFT, FONT_TD, rowBg);
            addTableCell(table, formatCount(p.unitsSold()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
            addTableCell(table, formatMoney(p.totalRevenue()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
        }

        doc.add(table);
    }

    private void addCommercialSection(Document doc, CommercialSummary comm) throws DocumentException {
        addSectionTitle(doc, "Volumen Comercial");

        if (comm == null) {
            addEmptyStateBox(doc, "No se registró actividad comercial en el periodo.");
            return;
        }

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{33.3f, 33.3f, 33.4f});
        table.setSpacingAfter(6);

        addMetricCard(table, "Ventas Comerciales Activas", formatMoney(comm.activeCommercialVolume()),
                comm.activeOrdersCount() + " pedidos vigentes");
        addMetricCard(table, "Ticket Promedio", formatMoney(comm.averageTicket()),
                "Ventas / Pedidos confirmados");
        addMetricCard(table, "GMV Confirmado Histórico", formatMoney(comm.historicalConfirmedGmv()),
                comm.historicalConfirmedOrdersCount() + " confirmados en periodo");

        doc.add(table);

        PdfPTable cancelTable = new PdfPTable(2);
        cancelTable.setWidthPercentage(100);
        cancelTable.setWidths(new float[]{50, 50});
        cancelTable.setSpacingAfter(8);

        addMetricCard(cancelTable, "Cancelaciones del Periodo", formatMoney(comm.cancelledOrdersVolume()),
                comm.cancelledOrdersCount() + " pedidos cancelados");
        addMetricCard(cancelTable, "Tasa de Retención Comercial",
                calculateRetentionRate(comm), "Respecto al GMV confirmado");

        doc.add(cancelTable);
    }

    private String calculateRetentionRate(CommercialSummary comm) {
        if (comm == null || comm.historicalConfirmedGmv() == null || comm.historicalConfirmedGmv().compareTo(BigDecimal.ZERO) == 0) {
            return "100.0%";
        }
        BigDecimal active = comm.activeCommercialVolume() != null ? comm.activeCommercialVolume() : BigDecimal.ZERO;
        BigDecimal gmv = comm.historicalConfirmedGmv();
        BigDecimal rate = active.divide(gmv, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return rate.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private void addFinancialSection(Document doc, FinancialSummary fin, boolean isAdmin) throws DocumentException {
        addSectionTitle(doc, "Movimientos Financieros en Pasarela");

        if (fin == null) {
            addEmptyStateBox(doc, "No se registraron movimientos financieros en pasarela.");
            return;
        }

        int cols = isAdmin ? 4 : 3;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        if (isAdmin) {
            table.setWidths(new float[]{25, 25, 25, 25});
        } else {
            table.setWidths(new float[]{33.3f, 33.3f, 33.4f});
        }
        table.setSpacingAfter(8);

        addMetricCard(table, "Pagos Aprobados Brutos", formatMoney(fin.grossInflowAmount()),
                fin.approvedPaymentsCount() + " transacciones capturadas");
        addMetricCard(table, "Reembolsos Efectivos", formatMoney(fin.settledRefundsAmount()),
                fin.settledRefundsCount() + " reembolsos completados");
        addMetricCard(table, "Volumen Neto Recaudado", formatMoney(fin.netFinancialInflow()),
                "Aprobados menos reembolsos");

        if (isAdmin) {
            addMetricCard(table, "Pagos Huérfanos Aprobados", formatMoney(fin.orphanPaymentsAmount()),
                    fin.orphanPaymentsCount() + " sin orden vinculada");
        }

        doc.add(table);
    }

    private void addCapacitySection(Document doc, CapacityDemandSummary cap) throws DocumentException {
        if (cap == null) return;
        addSectionTitle(doc, "Demanda de Capacidad y Servicios");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingAfter(8);

        addMetricCard(table, "Reservas Confirmadas (COMMITTED)", formatCount(cap.committedReservationsCount()),
                "Cupos comprometidos en confirmación");
        addMetricCard(table, "Reservas Liberadas (RELEASED)", formatCount(cap.releasedReservationsCount()),
                "Cupos cancelados o expirados");

        doc.add(table);
    }

    private void addTopProductsSection(Document doc, List<ProductSalesRank> products, String sectionTitle) throws DocumentException {
        addSectionTitle(doc, sectionTitle);

        if (products == null || products.isEmpty()) {
            addEmptyStateBox(doc, "No se registraron ventas de productos en el periodo seleccionado.");
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 42, 22, 13, 15});
        table.setHeaderRows(1);
        table.setSpacingAfter(8);

        addTableHeaderCell(table, "#", Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Producto", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Variante", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Unidades", Element.ALIGN_RIGHT);
        addTableHeaderCell(table, "Ingreso Bruto", Element.ALIGN_RIGHT);

        int count = Math.min(products.size(), MAX_TABLE_ROWS);
        for (int i = 0; i < count; i++) {
            ProductSalesRank p = products.get(i);
            Color rowBg = (i % 2 == 0) ? Color.WHITE : COLOR_ALT_ROW;

            addTableCell(table, String.valueOf(i + 1), Element.ALIGN_CENTER, FONT_TD, rowBg);
            addTableCell(table, p.productName(), Element.ALIGN_LEFT, FONT_TD, rowBg);
            addTableCell(table, p.variantName() != null ? p.variantName() : "—", Element.ALIGN_LEFT, FONT_TD, rowBg);
            addTableCell(table, formatCount(p.unitsSold()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
            addTableCell(table, formatMoney(p.totalRevenue()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
        }

        doc.add(table);
    }

    private void addStoreRankingsSection(Document doc, List<StorePerformanceRank> stores) throws DocumentException {
        addSectionTitle(doc, "Ranking de Rendimiento por Tienda");

        if (stores == null || stores.isEmpty()) {
            addEmptyStateBox(doc, "No se registraron ventas de tiendas en el periodo seleccionado.");
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 30, 26, 16, 20});
        table.setHeaderRows(1);
        table.setSpacingAfter(8);

        addTableHeaderCell(table, "#", Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Tienda", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "ID Tienda", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Pedidos Conf.", Element.ALIGN_RIGHT);
        addTableHeaderCell(table, "GMV Confirmado", Element.ALIGN_RIGHT);

        int count = Math.min(stores.size(), MAX_TABLE_ROWS);
        for (int i = 0; i < count; i++) {
            StorePerformanceRank s = stores.get(i);
            Color rowBg = (i % 2 == 0) ? Color.WHITE : COLOR_ALT_ROW;

            addTableCell(table, String.valueOf(i + 1), Element.ALIGN_CENTER, FONT_TD, rowBg);
            addTableCell(table, s.storeName(), Element.ALIGN_LEFT, FONT_TD, rowBg);
            addTableCell(table, s.storeId().toString(), Element.ALIGN_LEFT, FONT_TD_MUTED, rowBg);
            addTableCell(table, formatCount(s.confirmedOrdersCount()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
            addTableCell(table, formatMoney(s.commercialGmv()), Element.ALIGN_RIGHT, FONT_TD, rowBg);
        }

        doc.add(table);
    }

    private void addEmptyStateBox(Document doc, String message) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell(new Phrase(message, FONT_TD_MUTED));
        cell.setBackgroundColor(COLOR_CARD_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(cell);
        doc.add(table);
    }

    private void addDisclosuresSection(Document doc, List<String> notes) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_DISCLOSURE_BG);
        cell.setBorderColor(COLOR_DISCLOSURE_BORDER);
        cell.setBorderWidth(1);
        cell.setPadding(6);

        Paragraph title = new Paragraph("Aclaraciones Técnicas y Regulatorias", FONT_DISCLOSURE_TITLE);
        title.setSpacingAfter(3);
        cell.addElement(title);

        for (String note : notes) {
            Paragraph p = new Paragraph("• " + note, FONT_DISCLOSURE_TEXT);
            p.setSpacingAfter(2);
            cell.addElement(p);
        }

        table.addCell(cell);
        doc.add(table);
    }

    private void addMetricCard(PdfPTable parent, String label, String value, String subtext) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_CARD_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1);
        cell.setPadding(6);

        Paragraph pLabel = new Paragraph(label.toUpperCase(Locale.ROOT), FONT_CARD_LABEL);
        pLabel.setSpacingAfter(2);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value, FONT_CARD_VALUE);
        pVal.setSpacingAfter(2);
        cell.addElement(pVal);

        if (subtext != null && !subtext.isBlank()) {
            Paragraph pSub = new Paragraph(subtext, FONT_CARD_SUB);
            cell.addElement(pSub);
        }

        parent.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setBorderColor(COLOR_HEADER_BG);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, int align, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4.5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "S/ 0.00";
        return currencyFormat.format(amount);
    }

    private String formatCount(long count) {
        return numberFormat.format(count);
    }

    @FunctionalInterface
    private interface DocumentRenderer {
        void render(Document document, PdfWriter writer) throws DocumentException;
    }
}
