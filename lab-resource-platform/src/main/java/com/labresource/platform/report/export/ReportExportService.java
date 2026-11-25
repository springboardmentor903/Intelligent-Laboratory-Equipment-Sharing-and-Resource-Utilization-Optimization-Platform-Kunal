
package com.labresource.platform.report.export;

import com.labresource.platform.report.preview.ReportPreviewResponse;

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
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    private static final int MAX_EXPORT_ROWS = 10_000;

    public byte[] exportPdf(ReportPreviewResponse report) {
        validateRows(report);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());

            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 9);

            document.add(new Paragraph(
                    report.title() == null ? "Report" : report.title(),
                    titleFont
            ));

            document.add(new Paragraph(
                    "Period: " + report.fromDate() + " to " + report.toDate(),
                    normalFont
            ));

            document.add(new Paragraph(
                    "Total records: " + report.totalRows(),
                    normalFont
            ));

            document.add(new Paragraph(" "));

            List<String> columns = report.columns();
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);

            for (String column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column));
                cell.setBackgroundColor(
                        new java.awt.Color(220, 220, 220)
                );
                table.addCell(cell);
            }

            for (Map<String, Object> row : report.rows()) {
                for (String column : columns) {
                    Object value = row.get(column);
                    table.addCell(value == null ? "" : value.toString());
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Summary: " + report.totals(),
                    normalFont
            ));

            document.close();
            return output.toByteArray();

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate PDF report",
                    exception
            );
        }
    }

    private void validateRows(ReportPreviewResponse report) {
        if (report == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report data is required"
            );
        }

        if (report.totalRows() > MAX_EXPORT_ROWS || report.truncated()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report exceeds the maximum export limit of 10,000 rows"
            );
        }

        if (report.rows() == null || report.columns() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report rows and columns are required"
            );
        }

        if (report.rows().size() != report.totalRows()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report data is incomplete and cannot be exported"
            );
        }
    }
}
