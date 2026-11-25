
package com.labresource.platform.report.preview;

import com.labresource.platform.budget.dto.BudgetResponse;
import com.labresource.platform.budget.service.BudgetService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class ProcurementCostReportBuilder {

    private static final int MAX_ROWS = 10_000;

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    public ProcurementCostReportBuilder(
            BudgetService budgetService,
            UserRepository userRepository
    ) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
    }

    public ReportPreviewResponse build(
            LocalDate from, LocalDate to, String email
    ) {
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Institution is not assigned");
        }

        List<BudgetResponse> budgets =
                budgetService.getDepartmentBudgets(
                        user.getInstitution().getId(),
                        from.getYear()
                );

        List<Map<String, Object>> rows = new ArrayList<>();

        for (BudgetResponse budget : budgets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("departmentId", budget.getDepartmentId());
            row.put("departmentName", budget.getDepartmentName());
            row.put("fiscalYear", budget.getFiscalYear());
            row.put("budgetAmount", budget.getBudgetAmount());
            row.put("usedAmount", budget.getUsedAmount());
            row.put("remainingAmount", budget.getRemainingAmount());
            row.put("utilizationPercent", budget.getUtilizationPercent());
            row.put("status", budget.getStatus());
            rows.add(row);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("departmentCount", budgets.size());
        totals.put("totalBudget", budgets.stream()
                .map(BudgetResponse::getBudgetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalUsed", budgets.stream()
                .map(BudgetResponse::getUsedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return new ReportPreviewResponse(
                "PROCUREMENT_COST_ANALYSIS",
                "Procurement and Cost Analysis",
                from,
                to,
                List.of(
                        "departmentId", "departmentName", "fiscalYear",
                        "budgetAmount", "usedAmount", "remainingAmount",
                        "utilizationPercent", "status"
                ),
                rows.stream().limit(MAX_ROWS).toList(),
                totals,
                rows.size(),
                rows.size() > MAX_ROWS
        );
    }
}
