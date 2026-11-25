
package com.labresource.platform.report.export;

import com.labresource.platform.report.preview.ReportPreviewResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;

@Service
public class StreamingExcelExportService {

    private static final int MAX_ROWS = 10_000;

    public StreamingResponseBody streamExcel(ReportPreviewResponse report) {
        validateReport(report);

        return outputStream -> {
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                workbook.setCompressTempFiles(true);

                Sheet sheet = workbook.createSheet("Report");

                CellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 14);
                titleStyle.setFont(titleFont);

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(
                        IndexedColors.GREY_25_PERCENT.getIndex()
                );
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                int rowIndex = 0;

                Row titleRow = sheet.createRow(rowIndex++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(
                        report.title() == null ? "Report" : report.title()
                );
                titleCell.setCellStyle(titleStyle);

                Row periodRow = sheet.createRow(rowIndex++);
                periodRow.createCell(0).setCellValue(
                        "Period: " + report.fromDate() + " to " + report.toDate()
                );

                rowIndex++;

                List<String> columns = report.columns();

                Row headerRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                    cell.setCellStyle(headerStyle);
                }

                for (Map<String, Object> data : report.rows()) {
                    Row excelRow = sheet.createRow(rowIndex++);

                    for (int i = 0; i < columns.size(); i++) {
                        Cell cell = excelRow.createCell(i);
                        writeCellValue(
                                cell,
                                data == null ? null : data.get(columns.get(i))
                        );
                    }
                }

                Map<String, Object> totals = report.totals();

                if (totals != null && !totals.isEmpty()) {
                    rowIndex++;
                    Row heading = sheet.createRow(rowIndex++);
                    Cell headingCell = heading.createCell(0);
                    headingCell.setCellValue("Summary");
                    headingCell.setCellStyle(headerStyle);

                    for (Map.Entry<String, Object> entry : totals.entrySet()) {
                        Row totalRow = sheet.createRow(rowIndex++);
                        totalRow.createCell(0).setCellValue(entry.getKey());
                        writeCellValue(
                                totalRow.createCell(1),
                                entry.getValue()
                        );
                    }
                }

                for (int i = 0; i < columns.size(); i++) {
                    sheet.setColumnWidth(i, 20 * 256);
                }

                workbook.write(outputStream);
                outputStream.flush();

            } catch (Exception exception) {
                throw new java.io.IOException(
                        "Failed to generate Excel report",
                        exception
                );
            }
        };
    }

    private void validateReport(ReportPreviewResponse report) {
        if (report == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report data is required"
            );
        }

        if (report.totalRows() > MAX_ROWS || report.truncated()) {
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

    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean booleanValue) {
            cell.setCellValue(booleanValue);
        } else if (value instanceof TemporalAccessor) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
