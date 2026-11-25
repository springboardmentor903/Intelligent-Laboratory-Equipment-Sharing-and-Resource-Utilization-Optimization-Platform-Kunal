package com.labresource.platform.dashboard.service;

import com.labresource.platform.budget.dto.BudgetResponse;
import com.labresource.platform.budget.service.BudgetService;
import com.labresource.platform.dashboard.dto.InstitutionDashboardResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstitutionDashboardSupplementService {

    private final EntityManager entityManager;
    private final BudgetService budgetService;
    private final UsageChargeRepository usageChargeRepository;

    public InstitutionDashboardSupplementService(
            EntityManager entityManager,
            BudgetService budgetService,
            UsageChargeRepository usageChargeRepository
    ) {
        this.entityManager = entityManager;
        this.budgetService = budgetService;
        this.usageChargeRepository = usageChargeRepository;
    }

    public record DashboardSupplement(
            InstitutionDashboardResponse.InterInstitutionSharing sharing,
            InstitutionDashboardResponse.CrossDepartmentSharing crossDepartment,
            InstitutionDashboardResponse.CostBudgetSummary costBudget
    ) {}

    @Transactional(readOnly = true)
    public DashboardSupplement build(Long institutionId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(90);

        Object[] sharingRow = (Object[]) entityManager.createNativeQuery("""
                SELECT
                    COUNT(*) FILTER (
                        WHERE rs.shared_by_institution_id = :institutionId
                    ),
                    COUNT(*) FILTER (
                        WHERE rs.shared_with_institution_id = :institutionId
                    ),
                    COALESCE(SUM(
                        EXTRACT(EPOCH FROM (eb.end_time - eb.start_time)) / 3600.0
                    ) FILTER (
                        WHERE rs.shared_by_institution_id = :institutionId
                    ), 0),
                    COALESCE(SUM(
                        (EXTRACT(EPOCH FROM (eb.end_time - eb.start_time)) / 3600.0)
                        * COALESCE(rs.fee_per_hour, 0)
                    ) FILTER (
                        WHERE rs.shared_by_institution_id = :institutionId
                    ), 0),
                    COUNT(DISTINCT CASE
                        WHEN rs.shared_by_institution_id = :institutionId
                        THEN rs.shared_with_institution_id
                        ELSE rs.shared_by_institution_id
                    END)
                FROM external_bookings eb
                JOIN resource_shares rs ON rs.id = eb.resource_share_id
                WHERE eb.status = 'APPROVED'
                  AND eb.start_time >= :fromDate
                  AND eb.start_time < :toDate
                  AND (
                    rs.shared_by_institution_id = :institutionId
                    OR rs.shared_with_institution_id = :institutionId
                  )
                """)
                .setParameter("institutionId", institutionId)
                .setParameter("fromDate", from)
                .setParameter("toDate", now)
                .getSingleResult();

        var sharing = new InstitutionDashboardResponse.InterInstitutionSharing(
                number(sharingRow[0]).longValue(),
                number(sharingRow[1]).longValue(),
                decimal(sharingRow[2]),
                decimal(sharingRow[3]),
                number(sharingRow[4]).longValue()
        );

        List<InstitutionDashboardResponse.DepartmentPair> pairs =
                getDepartmentPairs(institutionId, from, now);

        BigDecimal crossDepartmentHours = pairs.stream()
                .map(InstitutionDashboardResponse.DepartmentPair::sharedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var crossDepartment =
                new InstitutionDashboardResponse.CrossDepartmentSharing(
                        crossDepartmentHours, pairs);

        List<BudgetResponse> budgets = budgetService.getDepartmentBudgets(
                institutionId, LocalDate.now().getYear());

        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;

        for (BudgetResponse budget : budgets) {
            allocated = allocated.add(nvl(budget.getBudgetAmount()));
            consumed = consumed.add(nvl(budget.getUsedAmount()));
        }

        BigDecimal remaining = allocated.subtract(consumed);

        BigDecimal budgetUtilization = allocated.signum() == 0
                ? BigDecimal.ZERO
                : consumed.multiply(BigDecimal.valueOf(100))
                  .divide(allocated, 2, RoundingMode.HALF_UP);

        Instant fromInstant = from.atZone(ZoneId.systemDefault()).toInstant();
        Instant toInstant = now.atZone(ZoneId.systemDefault()).toInstant();

        List<UsageCharge> charges =
                usageChargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId, fromInstant, toInstant);

        BigDecimal usageCharges = charges.stream()
                .map(UsageCharge::getAmount)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maintenanceCost = entityManager.createQuery("""
                SELECT COALESCE(SUM(w.actualCost), 0)
                FROM WorkOrder w
                WHERE w.resource.institution.id = :institutionId
                  AND w.createdAt >= :fromDate
                  AND w.createdAt < :toDate
                """, BigDecimal.class)
                .setParameter("institutionId", institutionId)
                .setParameter("fromDate", now.minusYears(1))
                .setParameter("toDate", now)
                .getSingleResult();

        var costBudget = new InstitutionDashboardResponse.CostBudgetSummary(
                usageCharges,
                nvl(maintenanceCost),
                allocated,
                consumed,
                remaining,
                budgetUtilization
        );

        return new DashboardSupplement(sharing, crossDepartment, costBudget);
    }

    private List<InstitutionDashboardResponse.DepartmentPair>
    getDepartmentPairs(
            Long institutionId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    owner_department.id,
                    owner_department.name,
                    user_department.id,
                    user_department.name,
                    COALESCE(SUM(
                        EXTRACT(EPOCH FROM (eb.end_time - eb.start_time)) / 3600.0
                    ), 0) AS shared_hours
                FROM external_bookings eb
                JOIN resource_shares rs ON rs.id = eb.resource_share_id
                JOIN resources r ON r.id = rs.resource_id
                JOIN departments owner_department
                    ON owner_department.id = r.department_id
                JOIN users requester ON requester.id = eb.requested_by_user_id
                JOIN departments user_department
                    ON user_department.id = requester.department_id
                WHERE eb.status = 'APPROVED'
                  AND eb.requesting_institution_id = :institutionId
                  AND rs.shared_by_institution_id = :institutionId
                  AND eb.start_time >= :fromDate
                  AND eb.start_time < :toDate
                GROUP BY
                    owner_department.id,
                    owner_department.name,
                    user_department.id,
                    user_department.name
                ORDER BY shared_hours DESC
                LIMIT 5
                """)
                .setParameter("institutionId", institutionId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();

        List<InstitutionDashboardResponse.DepartmentPair> result =
                new ArrayList<>();

        for (Object[] row : rows) {
            result.add(new InstitutionDashboardResponse.DepartmentPair(
                    number(row[0]).longValue(),
                    (String) row[1],
                    number(row[2]).longValue(),
                    (String) row[3],
                    decimal(row[4])
            ));
        }

        return result;
    }

    private Number number(Object value) {
        return value == null ? 0 : (Number) value;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}