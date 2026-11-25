
package com.labresource.platform.report.registry;

import java.util.List;

public record ReportDefinition(
        String type,
        String title,
        String description,
        List<String> allowedRoles,
        boolean exportSupported
) {
}
