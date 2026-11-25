
package com.labresource.platform.billing.service;

import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.usage.entity.UsageCharge;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    public byte[] generate(Invoice invoice) {

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = new Font(
                    Font.HELVETICA, 20, Font.BOLD
            );

            Font headingFont = new Font(
                    Font.HELVETICA, 11, Font.BOLD
            );

            Font normalFont = new Font(
                    Font.HELVETICA, 10
            );

            document.add(new Paragraph(
                    "INVOICE",
                    titleFont
            ));

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Invoice Number: " + invoice.getInvoiceNumber(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "Invoice ID: " + invoice.getId(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "Invoice Type: " + invoice.getInvoiceType(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "Status: " + invoice.getStatus(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "Issuer Institution ID: "
                            + invoice.getIssuerInstitutionId(),
                    normalFont
            ));

            if (invoice.getRecipientInstitutionId() != null) {
                document.add(new Paragraph(
                        "Recipient Institution ID: "
                                + invoice.getRecipientInstitutionId(),
                        normalFont
                ));
            }

            if (invoice.getRecipientDepartmentId() != null) {
                document.add(new Paragraph(
                        "Recipient Department ID: "
                                + invoice.getRecipientDepartmentId(),
                        normalFont
                ));
            }

            document.add(new Paragraph(
                    "Billing Period: "
                            + invoice.getPeriodStart()
                            + " to "
                            + invoice.getPeriodEnd(),
                    normalFont
            ));

            if (invoice.getDueDate() != null) {
                document.add(new Paragraph(
                        "Due Date: " + invoice.getDueDate(),
                        normalFont
                ));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Charge Details",
                    headingFont
            ));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(
                    new float[]{1, 2, 3, 2, 2}
            );
            table.setWidthPercentage(100);

            addHeader(table, "Charge ID");
            addHeader(table, "Booking ID");
            addHeader(table, "Equipment");
            addHeader(table, "Usage Minutes");
            addHeader(table, "Amount");

            for (UsageCharge charge : invoice.getCharges()) {

                addCell(table, String.valueOf(charge.getId()));

                addCell(
                        table,
                        charge.getBooking() == null
                                ? "-"
                                : String.valueOf(charge.getBooking().getId())
                );

                addCell(
                        table,
                        charge.getResource() == null
                                ? "-"
                                : charge.getResource().getName()
                );

                addCell(
                        table,
                        charge.getUsageMinutes() == null
                                ? "0"
                                : charge.getUsageMinutes().toString()
                );

                addCell(
                        table,
                        money(charge.getAmount())
                );
            }

            document.add(table);
            document.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(55);
            summary.setHorizontalAlignment(
                    com.lowagie.text.Element.ALIGN_RIGHT
            );

            addSummaryRow(
                    summary, "Subtotal",
                    money(invoice.getSubtotal())
            );

            addSummaryRow(
                    summary,
                    "Tax Rate",
                    money(invoice.getTaxRate()) + "%"
            );

            addSummaryRow(
                    summary,
                    "Tax Amount",
                    money(invoice.getTaxAmount())
            );

            addSummaryRow(
                    summary,
                    "Total Amount",
                    invoice.getCurrency() + " "
                            + money(invoice.getTotalAmount())
            );

            document.add(summary);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "This is a system-generated invoice.",
                    normalFont
            ));

            document.close();

            return output.toByteArray();

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate invoice PDF",
                    ex
            );
        }
    }

    private void addHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value));
        cell.setBackgroundColor(
                new java.awt.Color(220, 220, 220)
        );
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value) {
        table.addCell(value == null ? "-" : value);
    }

    private void addSummaryRow(
            PdfPTable table,
            String label,
            String value
    ) {
        table.addCell(label);
        table.addCell(value);
    }

    private String money(BigDecimal amount) {
        return amount == null
                ? "0.00"
                : amount.setScale(
                2,
                java.math.RoundingMode.HALF_UP
        ).toPlainString();
    }
}
