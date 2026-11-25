
package com.labresource.platform.report.export;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

public enum ReportExportFormat {
    PDF,
    XLSX;

    public static ReportExportFormat from(String value) {
        try {
            return ReportExportFormat.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Supported formats are pdf and xlsx"
            );
        }
    }
}
