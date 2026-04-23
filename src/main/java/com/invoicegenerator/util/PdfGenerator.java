package com.invoicegenerator.util;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import com.invoicegenerator.dto.InvoiceRequest;
import com.invoicegenerator.dto.ProductItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class PdfGenerator {

    // ── Colors ────────────────────────────────────────────────────
    private static final Color DARK_NAVY  = new Color(26, 26, 46);
    private static final Color PURPLE_ACC = new Color(124, 106, 245);
    private static final Color LIGHT_BG   = new Color(248, 248, 251);
    private static final Color BORDER_CLR = new Color(220, 220, 228);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color TEXT_DARK  = new Color(17, 24, 39);
    private static final Color WHITE      = Color.WHITE;

    // ── Font shorthand ────────────────────────────────────────────
    private static Font f(String base, int size, int style, Color color) {
        return FontFactory.getFont(base, size, style, color);
    }

    public static ByteArrayInputStream generate(InvoiceRequest request) {

        // Tight margins to maximise usable space on one page
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ════════════════════════════════════════════════════════
            // 1. HEADER
            // ════════════════════════════════════════════════════════
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.1f, 0.9f});
            header.setSpacingAfter(5);

            // ── Left: logo (on white box) + company info ───────────
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setBackgroundColor(DARK_NAVY);
            leftCell.setPadding(12);
            leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            if (request.getCompanyLogo() != null && !request.getCompanyLogo().isEmpty()) {
                try {
                    String b64 = request.getCompanyLogo();
                    if (b64.contains(",")) b64 = b64.split(",")[1];
                    byte[] imgBytes = Base64.getDecoder().decode(b64);
                    Image logo = Image.getInstance(imgBytes);
                    logo.scaleToFit(48, 48);

                    // Wrap logo in a white-background cell so black logos show clearly
                    PdfPTable logoWrapper = new PdfPTable(1);
                    logoWrapper.setTotalWidth(64);
                    logoWrapper.setLockedWidth(true);
                    logoWrapper.setHorizontalAlignment(Element.ALIGN_LEFT);

                    PdfPCell logoBox = new PdfPCell(logo, false);
                    logoBox.setBackgroundColor(WHITE);       // white bg = logo always visible
                    logoBox.setBorder(Rectangle.NO_BORDER);
                    logoBox.setPadding(6);
                    logoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
                    logoBox.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    logoWrapper.addCell(logoBox);

                    leftCell.addElement(logoWrapper);
                    leftCell.addElement(spacer(4));
                } catch (Exception ignored) {}
            }

            leftCell.addElement(new Paragraph(
                safe(request.getCompanyName()),
                f(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, WHITE)
            ));
            leftCell.addElement(tinyPara(safe(request.getCompanyAddress()), new Color(190, 190, 210)));
            leftCell.addElement(tinyPara("Ph: " + safe(request.getCompanyPhone()), new Color(190, 190, 210)));
            leftCell.addElement(tinyPara("GSTIN: " + safe(request.getCompanyGstin()), new Color(190, 190, 210)));
            header.addCell(leftCell);

            // ── Right: invoice title + meta ────────────────────────
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setBackgroundColor(DARK_NAVY);
            rightCell.setPadding(12);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph invTitle = new Paragraph(
                "TAX INVOICE",
                f(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, WHITE)
            );
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invTitle);

            Paragraph invNum = new Paragraph(
                "#" + safe(request.getInvoiceNumber()),
                f(FontFactory.HELVETICA, 9, Font.NORMAL, new Color(160, 160, 205))
            );
            invNum.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invNum);

            rightCell.addElement(spacer(4));
            rightCell.addElement(rightPara("Date: " + safe(request.getInvoiceDate())));
            if (!safe(request.getPaymentTerms()).isEmpty())
                rightCell.addElement(rightPara("Terms: " + safe(request.getPaymentTerms())));
            if (!safe(request.getPlaceOfSupply()).isEmpty())
                rightCell.addElement(rightPara("Place of Supply: " + safe(request.getPlaceOfSupply())));
            header.addCell(rightCell);

            document.add(header);

            // ════════════════════════════════════════════════════════
            // 2. BILL TO  +  DELIVERY & PAYMENT
            // ════════════════════════════════════════════════════════
            PdfPTable billing = new PdfPTable(2);
            billing.setWidthPercentage(100);
            billing.setWidths(new float[]{1f, 1f});
            billing.setSpacingAfter(5);

            billing.addCell(infoCard("BILL TO", new String[]{
                safe(request.getBuyerName()),
                safe(request.getBuyerAddress()),
                "Ph: " + safe(request.getBuyerPhone()),
                "GSTIN: " + safe(request.getBuyerGstin())
            }));

            billing.addCell(infoCard("DELIVERY & PAYMENT", new String[]{
                safe(request.getDeliveryNote()).isEmpty()
                    ? "" : "Delivery Note: " + safe(request.getDeliveryNote()),
                "Payment Terms: " + safe(request.getPaymentTerms()),
                "Place of Supply: " + safe(request.getPlaceOfSupply())
            }));

            document.add(billing);

            // ════════════════════════════════════════════════════════
            // 3. PRODUCT TABLE
            // ════════════════════════════════════════════════════════
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.2f, 1.3f, 0.9f, 1.4f, 1.4f});
            table.setSpacingAfter(5);

            String[] cols     = {"PRODUCT / SERVICE", "HSN/SAC", "QTY", "RATE (Rs.)", "AMOUNT (Rs.)"};
            int[]    colAlign = {Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_CENTER,
                                 Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};

            for (int i = 0; i < cols.length; i++) {
                PdfPCell hc = new PdfPCell(new Phrase(
                    cols[i], f(FontFactory.HELVETICA_BOLD, 7, Font.BOLD, WHITE)
                ));
                hc.setBackgroundColor(DARK_NAVY);
                hc.setPadding(7);
                hc.setBorder(Rectangle.NO_BORDER);
                hc.setHorizontalAlignment(colAlign[i]);
                table.addCell(hc);
            }

            boolean alt = false;
            for (ProductItem item : request.getProducts()) {
                Color rowBg = alt ? LIGHT_BG : WHITE;
                alt = !alt;
                productCell(table, safe(item.getProductName()), Element.ALIGN_LEFT,   rowBg, false);
                productCell(table, safe(item.getHsnSac()),      Element.ALIGN_CENTER, rowBg, false);
                productCell(table, str(item.getQuantity()),     Element.ALIGN_CENTER, rowBg, false);
                productCell(table, fmt(item.getRate()),         Element.ALIGN_RIGHT,  rowBg, false);
                productCell(table, fmt(item.getAmount()),       Element.ALIGN_RIGHT,  rowBg, true);
            }

            document.add(table);

            // ════════════════════════════════════════════════════════
            // 4. BANK DETAILS  +  TAX SUMMARY
            // ════════════════════════════════════════════════════════
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.setWidths(new float[]{1.25f, 0.75f});
            summary.setSpacingAfter(5);

            PdfPCell bankCell = new PdfPCell();
            bankCell.setBorder(Rectangle.BOX);
            bankCell.setBorderColor(BORDER_CLR);
            bankCell.setBorderWidth(0.5f);
            bankCell.setPadding(9);
            bankCell.setBackgroundColor(LIGHT_BG);
            sectionHeading(bankCell, "BANK DETAILS");
            kvLine(bankCell, "Bank",       safe(request.getBankName()));
            kvLine(bankCell, "Account No", safe(request.getAccountNumber()));
            kvLine(bankCell, "IFSC",       safe(request.getIfscCode()));
            kvLine(bankCell, "Branch",     safe(request.getBranchName()));
            summary.addCell(bankCell);

            PdfPCell taxCell = new PdfPCell();
            taxCell.setBorder(Rectangle.BOX);
            taxCell.setBorderColor(BORDER_CLR);
            taxCell.setBorderWidth(0.5f);
            taxCell.setPadding(9);
            sectionHeading(taxCell, "TAX SUMMARY");
            kvLine(taxCell, "CGST",      "Rs. " + fmt(request.getCgst()));
            kvLine(taxCell, "SGST",      "Rs. " + fmt(request.getSgst()));
            kvLine(taxCell, "IGST",      "Rs. " + fmt(request.getIgst()));
            kvLine(taxCell, "Round Off", "Rs. " + fmt(request.getRoundOff()));
            summary.addCell(taxCell);

            document.add(summary);

            // ════════════════════════════════════════════════════════
            // 5. GRAND TOTAL BANNER
            // ════════════════════════════════════════════════════════
            PdfPTable totalBanner = new PdfPTable(2);
            totalBanner.setWidthPercentage(100);
            totalBanner.setWidths(new float[]{1f, 1f});
            totalBanner.setSpacingAfter(5);

            PdfPCell totalLabel = new PdfPCell(new Phrase(
                "GRAND TOTAL",
                f(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, WHITE)
            ));
            totalLabel.setBackgroundColor(DARK_NAVY);
            totalLabel.setPadding(11);
            totalLabel.setBorder(Rectangle.NO_BORDER);
            totalLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalBanner.addCell(totalLabel);

            PdfPCell totalAmt = new PdfPCell(new Phrase(
                "Rs. " + fmt(request.getGrandTotal()),
                f(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, WHITE)
            ));
            totalAmt.setBackgroundColor(PURPLE_ACC);
            totalAmt.setPadding(11);
            totalAmt.setBorder(Rectangle.NO_BORDER);
            totalAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalAmt.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalBanner.addCell(totalAmt);

            document.add(totalBanner);

            // ════════════════════════════════════════════════════════
            // 6. DECLARATION  +  SIGNATORY
            // ════════════════════════════════════════════════════════
            PdfPTable declRow = new PdfPTable(2);
            declRow.setWidthPercentage(100);
            declRow.setWidths(new float[]{1.5f, 0.5f});
            declRow.setSpacingAfter(8);

            PdfPCell declCell = new PdfPCell();
            declCell.setBorder(Rectangle.BOX);
            declCell.setBorderColor(BORDER_CLR);
            declCell.setBorderWidth(0.5f);
            declCell.setPadding(9);
            declCell.setBackgroundColor(LIGHT_BG);
            sectionHeading(declCell, "DECLARATION");
            declCell.addElement(new Paragraph(
                safe(request.getDeclaration()),
                f(FontFactory.HELVETICA, 8, Font.ITALIC, TEXT_MUTED)
            ));
            declRow.addCell(declCell);

            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.BOX);
            signCell.setBorderColor(BORDER_CLR);
            signCell.setBorderWidth(0.5f);
            signCell.setPadding(9);
            sectionHeading(signCell, "AUTHORISED SIGNATORY");
            signCell.addElement(spacer(18));
            Paragraph sigLine = new Paragraph(
                "________________________",
                f(FontFactory.HELVETICA, 9, Font.NORMAL, BORDER_CLR)
            );
            sigLine.setAlignment(Element.ALIGN_CENTER);
            signCell.addElement(sigLine);
            Paragraph sigName = new Paragraph(
                safe(request.getCompanyName()),
                f(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, TEXT_DARK)
            );
            sigName.setAlignment(Element.ALIGN_CENTER);
            signCell.addElement(sigName);
            declRow.addCell(signCell);

            document.add(declRow);

            // ════════════════════════════════════════════════════════
            // 7. FOOTER
            // ════════════════════════════════════════════════════════
            Paragraph footer = new Paragraph(
                "Thank you for your business  \u2022  This is a computer-generated invoice",
                f(FontFactory.HELVETICA, 7, Font.ITALIC, TEXT_MUTED)
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ── Utility helpers ───────────────────────────────────────────

    private static String safe(Object v) {
        return v == null ? "" : v.toString().trim();
    }

    private static String fmt(Object v) {
        if (v == null) return "0.00";
        try { return String.format("%.2f", Double.parseDouble(v.toString())); }
        catch (NumberFormatException e) { return v.toString(); }
    }

    private static String str(Object v) {
        return v == null ? "" : v.toString();
    }

    /** Blank paragraph used as vertical spacer */
    private static Paragraph spacer(int pts) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(pts);
        return p;
    }

    private static Paragraph tinyPara(String text, Color color) {
        Paragraph p = new Paragraph(text, f(FontFactory.HELVETICA, 8, Font.NORMAL, color));
        p.setSpacingAfter(1);
        return p;
    }

    private static Paragraph rightPara(String text) {
        Paragraph p = new Paragraph(
            text, f(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(180, 180, 205))
        );
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(1);
        return p;
    }

    /** Card cell with heading + lines (first line bold as name/title) */
    private static PdfPCell infoCard(String heading, String[] lines) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_CLR);
        cell.setBorderWidth(0.5f);
        cell.setPadding(9);
        cell.setBackgroundColor(LIGHT_BG);
        sectionHeading(cell, heading);
        boolean first = true;
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            Font lineFont = first
                ? f(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, TEXT_DARK)
                : f(FontFactory.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);
            Paragraph p = new Paragraph(line, lineFont);
            p.setSpacingAfter(1);
            cell.addElement(p);
            first = false;
        }
        return cell;
    }

    /** Purple label + thin dotted separator */
    private static void sectionHeading(PdfPCell cell, String text) {
        Paragraph label = new Paragraph(
            text, f(FontFactory.HELVETICA_BOLD, 7, Font.BOLD, PURPLE_ACC)
        );
        label.setSpacingAfter(1);
        cell.addElement(label);
        Paragraph rule = new Paragraph(
            "____________________________________________________________",
            f(FontFactory.HELVETICA, 3, Font.NORMAL, BORDER_CLR)
        );
        rule.setSpacingAfter(3);
        cell.addElement(rule);
    }

    /** Key-value row inside a card */
    private static void kvLine(PdfPCell cell, String key, String value) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        try { t.setWidths(new float[]{1f, 1.8f}); } catch (Exception ignored) {}

        PdfPCell kc = new PdfPCell(new Phrase(key, f(FontFactory.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)));
        kc.setBorder(Rectangle.NO_BORDER);
        kc.setPadding(2);

        PdfPCell vc = new PdfPCell(new Phrase(value, f(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, TEXT_DARK)));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPadding(2);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);

        t.addCell(kc);
        t.addCell(vc);
        cell.addElement(t);
    }

    /** Single product table data cell */
    private static void productCell(PdfPTable table, String text, int align, Color bg, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(
            text,
            bold
                ? f(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, TEXT_DARK)
                : f(FontFactory.HELVETICA, 9, Font.NORMAL, TEXT_DARK)
        ));
        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setBorderColor(BORDER_CLR);
        cell.setBorderWidth(0.5f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
}