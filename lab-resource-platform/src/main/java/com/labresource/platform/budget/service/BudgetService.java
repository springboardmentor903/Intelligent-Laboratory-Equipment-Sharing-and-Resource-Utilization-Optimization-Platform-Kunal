
package com.labresource.platform.budget.service;

import com.labresource.platform.budget.dto.BudgetRequest;
import com.labresource.platform.budget.dto.BudgetResponse;
import com.labresource.platform.budget.entity.DepartmentBudget;
import com.labresource.platform.budget.repository.DepartmentBudgetRepository;
import com.labresource.platform.department.entity.Department;
import com.labresource.platform.department.repository.DepartmentRepository;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class BudgetService {

    private final DepartmentBudgetRepository budgetRepository;
    private final DepartmentRepository departmentRepository;
    private final UsageChargeRepository chargeRepository;

    @Value("${app.fiscal-year.start-month:1}")
    private int fiscalYearStartMonth;

    public BudgetService(
            DepartmentBudgetRepository budgetRepository,
            DepartmentRepository departmentRepository,
            UsageChargeRepository chargeRepository) {
        this.budgetRepository = budgetRepository;
        this.departmentRepository = departmentRepository;
        this.chargeRepository = chargeRepository;
    }

    public BudgetResponse createOrUpdateBudget(
            Long institutionId,
            BudgetRequest request) {

        validateRequest(request);

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"));

        if (department.getInstitution() == null ||
                !institutionId.equals(department.getInstitution().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Department does not belong to your institution");
        }

        DepartmentBudget budget = budgetRepository
                .findByDepartment_IdAndFiscalYear(
                        department.getId(), request.getFiscalYear())
                .orElseGet(DepartmentBudget::new);

        budget.setDepartment(department);
        budget.setFiscalYear(request.getFiscalYear());
        budget.setBudgetAmount(request.getBudgetAmount());

        DepartmentBudget saved = budgetRepository.save(budget);

        return buildResponse(
                department,
                saved,
                request.getFiscalYear(),
                institutionId
        );
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getDepartmentBudgets(
            Long institutionId,
            Integer fiscalYear) {

        validateFiscalYear(fiscalYear);

        List<Department> departments =
                departmentRepository.findByInstitution_Id(institutionId);

        Map<Long, DepartmentBudget> budgets = new HashMap<>();

        for (DepartmentBudget budget :
                budgetRepository.findByDepartment_Institution_IdAndFiscalYear(
                        institutionId, fiscalYear)) {
            budgets.put(budget.getDepartment().getId(), budget);
        }

        List<BudgetResponse> responses = new ArrayList<>();

        for (Department department : departments) {
            DepartmentBudget budget = budgets.get(department.getId());

            responses.add(buildResponse(
                    department, budget, fiscalYear, institutionId
            ));
        }

        return responses;
    }

    private BudgetResponse buildResponse(
            Department department,
            DepartmentBudget budget,
            Integer fiscalYear,
            Long institutionId) {

        BigDecimal usedAmount = calculateDepartmentUsage(
                institutionId, department.getId(), fiscalYear);

        if (budget == null) {
            return new BudgetResponse(
                    department.getId(),
                    department.getName(),
                    fiscalYear,
                    BigDecimal.ZERO,
                    usedAmount,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "NO_BUDGET"
            );
        }

        BigDecimal budgetAmount = budget.getBudgetAmount();
        BigDecimal remaining = budgetAmount.subtract(usedAmount);

        BigDecimal utilization = budgetAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : usedAmount.multiply(BigDecimal.valueOf(100))
                  .divide(budgetAmount, 2, RoundingMode.HALF_UP);

        String status;

        if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
            status = "EXCEEDED";
        } else if (utilization.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = "WARNING";
        } else {
            status = "OK";
        }

        return new BudgetResponse(
                department.getId(),
                department.getName(),
                fiscalYear,
                budgetAmount,
                usedAmount,
                remaining,
                utilization,
                status
        );
    }

    private BigDecimal calculateDepartmentUsage(
            Long institutionId,
            Long departmentId,
            Integer fiscalYear) {

        LocalDate start = LocalDate.of(
                fiscalYear, fiscalYearStartMonth, 1);

        LocalDate end = start.plusYears(1);

        Instant from = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = end.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<UsageCharge> charges =
                chargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId, from, to);

        return charges.stream()
                .filter(charge -> departmentId.equals(charge.getDepartmentId()))
                .filter(charge ->
                        charge.getChargeType() == UsageCharge.ChargeType.INTERNAL
                                || charge.getChargeType() == UsageCharge.ChargeType.EXTERNAL)
                .map(UsageCharge::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRequest(BudgetRequest request) {
        if (request == null ||
                request.getDepartmentId() == null ||
                request.getFiscalYear() == null ||
                request.getBudgetAmount() == null ||
                request.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Department, fiscal year and positive budget amount are required");
        }

        validateFiscalYear(request.getFiscalYear());
    }

    private void validateFiscalYear(Integer fiscalYear) {
        if (fiscalYear == null ||
                fiscalYear < 2000 ||
                fiscalYear > LocalDate.now().getYear() + 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid fiscal year");
        }

        if (fiscalYearStartMonth < 1 || fiscalYearStartMonth > 12) {
            throw new IllegalStateException(
                    "app.fiscal-year.start-month must be between 1 and 12");
        }
    }
}
