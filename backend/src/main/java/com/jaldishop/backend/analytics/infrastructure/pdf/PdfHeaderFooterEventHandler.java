package com.jaldishop.backend.analytics.infrastructure.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

public class PdfHeaderFooterEventHandler extends PdfPageEventHelper {

    private final String documentTitle;
    private PdfTemplate totalPagesTemplate;
    private BaseFont baseFont;
    private final Font headerFooterFont;

    public PdfHeaderFooterEventHandler(String documentTitle) {
        this.documentTitle = documentTitle;
        this.headerFooterFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(100, 116, 139));
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing PDF template", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();

        float left = document.left();
        float right = document.right();
        float top = document.top() + 14;
        float bottom = document.bottom() - 14;

        // Running Header
        Phrase headerPhrase = new Phrase("JaldiShop Analytics • " + documentTitle, headerFooterFont);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, headerPhrase, left, top, 0);

        cb.setColorStroke(new Color(226, 232, 240));
        cb.setLineWidth(0.5f);
        cb.moveTo(left, top - 4);
        cb.lineTo(right, top - 4);
        cb.stroke();

        // Running Footer: Confidential notice on left, Page X of Y on right
        Phrase footerNotice = new Phrase("Documento confidencial — Generado por JaldiShop Platform", headerFooterFont);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, footerNotice, left, bottom, 0);

        String pageText = "Página " + writer.getPageNumber() + " de ";
        float textWidth = baseFont.getWidthPoint(pageText, 8);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(pageText, headerFooterFont), right - 18, bottom, 0);
        cb.addTemplate(totalPagesTemplate, right - 18, bottom);

        cb.moveTo(left, bottom + 10);
        cb.lineTo(right, bottom + 10);
        cb.stroke();
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPagesTemplate.beginText();
        totalPagesTemplate.setFontAndSize(baseFont, 8);
        totalPagesTemplate.setColorFill(new Color(100, 116, 139));
        totalPagesTemplate.showText(String.valueOf(writer.getPageNumber()));
        totalPagesTemplate.endText();
    }
}
