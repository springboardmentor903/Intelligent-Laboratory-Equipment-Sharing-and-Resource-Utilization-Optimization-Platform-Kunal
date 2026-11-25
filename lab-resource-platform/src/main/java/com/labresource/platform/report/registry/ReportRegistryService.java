
package com.labresource.platform.report.registry;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ReportRegistryService {

    private final UserRepository userRepository;

    private static final List<ReportDefinition> REPORTS = List.of(
            new ReportDefinition(
                    "UTILIZATION",
                    "Resource Utilization",
                    "Equipment booking, utilization and actual usage summary",
                    List.of("INSTITUTION_ADMIN", "LAB_MANAGER",
                            "DEPARTMENT_HEAD", "SYSTEM_ADMIN"),
                    true
            ),
            new ReportDefinition(
                    "DEPARTMENT_USAGE",
                    "Department Usage",
                    "Department-wise equipment usage and resource consumption",
                    List.of("INSTITUTION_ADMIN", "DEPARTMENT_HEAD",
                            "SYSTEM_ADMIN"),
                    true
            ),
            new ReportDefinition(
                    "MAINTENANCE_DOWNTIME",
                    "Maintenance and Downtime",
                    "Work orders, maintenance costs and equipment downtime",
                    List.of("INSTITUTION_ADMIN", "LAB_MANAGER"),
                    true
            ),
            new ReportDefinition(
                    "INTER_INSTITUTION_SHARING",
                    "Inter-Institution Sharing",
                    "External resource sharing and booking analytics",
                    List.of("INSTITUTION_ADMIN", "SYSTEM_ADMIN"),
                    true
            ),
            new ReportDefinition(
                    "PROCUREMENT_COST_ANALYSIS",
                    "Procurement and Cost Analysis",
                    "Equipment procurement, maintenance and usage cost analysis",
                    List.of("INSTITUTION_ADMIN", "SYSTEM_ADMIN"),
                    true
            )
    );

    public ReportRegistryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportDefinition> getAvailableReports(String email) {
        String role = getUserRole(email);

        return REPORTS.stream()
                .filter(report -> report.allowedRoles().contains(role))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDefinition getReportDefinition(
            String type,
            String email
    ) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report type is required"
            );
        }

        ReportType reportType;

        try {
            reportType = ReportType.valueOf(
                    type.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid report type"
            );
        }

        String role = getUserRole(email);

        ReportDefinition definition = REPORTS.stream()
                .filter(report -> report.type().equals(reportType.name()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Report type not found"
                ));

        if (!definition.allowedRoles().contains(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this report"
            );
        }

        return definition;
    }

    private String getUserRole(String email) {
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User profile not found"
                ));

        if (user.getRole() == null
                || user.getRole().getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User role is not configured"
            );
        }

        String role = user.getRole().getName()
                .trim()
                .toUpperCase(Locale.ROOT);

        return role.startsWith("ROLE_")
                ? role.substring(5)
                : role;
    }
}
