
package com.labresource.platform.usage.service;

import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;
import com.labresource.platform.usage.dto.EquipmentCostResponse;
import com.labresource.platform.usage.dto.DepartmentAllocationResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.department.entity.Department;
import com.labresource.platform.department.repository.DepartmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CostOverviewService {

    private final UsageChargeRepository chargeRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DepartmentRepository departmentRepository;

    public CostOverviewService(
            UsageChargeRepository chargeRepository,
            WorkOrderRepository workOrderRepository,
            DepartmentRepository departmentRepository) {
        this.chargeRepository = chargeRepository;
        this.workOrderRepository = workOrderRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<EquipmentCostResponse> getEquipmentCosts(
            Long institutionId,
            LocalDate fromDate,
            LocalDate toDate,
            Long departmentId) {

        Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<UsageCharge> charges =
                chargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId, from, to);

        Map<Long, EquipmentAggregate> aggregates = new LinkedHashMap<>();

        for (UsageCharge charge : charges) {
            if (charge.getResource() == null) continue;

            Long resourceId = charge.getResource().getId();
            Long chargeDepartmentId = charge.getDepartmentId();

            if (departmentId != null &&
                    !departmentId.equals(chargeDepartmentId)) {
                continue;
            }

            EquipmentAggregate aggregate = aggregates.computeIfAbsent(
                    resourceId,
                    id -> new EquipmentAggregate(
                            id,
                            charge.getResource().getName()
                    )
            );

            aggregate.usageMinutes += charge.getUsageMinutes() == null
                    ? 0L : charge.getUsageMinutes().longValue();

            BigDecimal amount = safe(charge.getAmount());

            if (charge.getChargeType() == UsageCharge.ChargeType.INTERNAL) {
                aggregate.internalCost =
                        aggregate.internalCost.add(amount);
            } else if (charge.getChargeType() == UsageCharge.ChargeType.EXTERNAL) {
                aggregate.externalCost =
                        aggregate.externalCost.add(amount);
            } else {
                aggregate.unassignedCost =
                        aggregate.unassignedCost.add(amount);
            }
        }

        List<WorkOrder> workOrders =
                workOrderRepository.findCostedCompletedWorkOrders(
                        institutionId,
                        fromDate.atStartOfDay(),
                        toDate.plusDays(1).atStartOfDay()
                );

        for (WorkOrder workOrder : workOrders) {
            if (workOrder.getResource() == null) continue;

            Long resourceId = workOrder.getResource().getId();
            Long resourceDepartmentId =
                    workOrder.getResource().getDepartment() == null
                            ? null
                            : workOrder.getResource().getDepartment().getId();

            if (departmentId != null &&
                    !departmentId.equals(resourceDepartmentId)) {
                continue;
            }

            EquipmentAggregate aggregate = aggregates.computeIfAbsent(
                    resourceId,
                    id -> new EquipmentAggregate(
                            id,
                            workOrder.getResource().getName()
                    )
            );

            aggregate.maintenanceCost =
                    aggregate.maintenanceCost.add(
                            safe(workOrder.getActualCost())
                    );
        }

        return aggregates.values().stream()
                .map(a -> new EquipmentCostResponse(
                        a.resourceId,
                        a.equipmentName,
                        a.usageMinutes,
                        a.internalCost,
                        a.externalCost,
                        a.unassignedCost,
                        a.maintenanceCost
                ))
                .toList();
    }

    public List<DepartmentAllocationResponse> getDepartmentAllocation(
            Long institutionId,
            LocalDate fromDate,
            LocalDate toDate,
            Long departmentId) {

        Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<UsageCharge> charges =
                chargeRepository
                        .findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                institutionId, from, to);

        Map<Long, DepartmentAggregate> totals = new LinkedHashMap<>();

        for (UsageCharge charge : charges) {
            Long deptId = charge.getDepartmentId();

            if (departmentId != null && !departmentId.equals(deptId)) {
                continue;
            }

            DepartmentAggregate aggregate =
                    totals.computeIfAbsent(deptId, id -> new DepartmentAggregate());

            BigDecimal amount = safe(charge.getAmount());

            if (charge.getChargeType() == UsageCharge.ChargeType.INTERNAL) {
                aggregate.internalCost =
                        aggregate.internalCost.add(amount);
            } else if (charge.getChargeType() == UsageCharge.ChargeType.EXTERNAL) {
                aggregate.externalCost =
                        aggregate.externalCost.add(amount);
            }
        }

        BigDecimal grandTotal = totals.values().stream()
                .map(a -> a.internalCost.add(a.externalCost))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, String> departmentNames = new HashMap<>();
        for (Department department :
                departmentRepository.findByInstitution_Id(institutionId)) {
            departmentNames.put(department.getId(), department.getName());
        }

        return totals.entrySet().stream()
                .map(entry -> {
                    Long deptId = entry.getKey();
                    DepartmentAggregate a = entry.getValue();
                    BigDecimal total = a.internalCost.add(a.externalCost);

                    BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : total.multiply(BigDecimal.valueOf(100))
                              .divide(grandTotal, 2, RoundingMode.HALF_UP);

                    return new DepartmentAllocationResponse(
                            deptId,
                            deptId == null
                                    ? "Unallocated"
                                    : departmentNames.getOrDefault(
                                    deptId, "Department " + deptId),
                            a.internalCost,
                            a.externalCost,
                            total,
                            percentage
                    );
                })
                .sorted(Comparator.comparing(
                        DepartmentAllocationResponse::getTotalCost
                ).reversed())
                .toList();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class EquipmentAggregate {
        private final Long resourceId;
        private final String equipmentName;
        private long usageMinutes;
        private BigDecimal internalCost = BigDecimal.ZERO;
        private BigDecimal externalCost = BigDecimal.ZERO;
        private BigDecimal unassignedCost = BigDecimal.ZERO;
        private BigDecimal maintenanceCost = BigDecimal.ZERO;

        private EquipmentAggregate(Long resourceId, String equipmentName) {
            this.resourceId = resourceId;
            this.equipmentName = equipmentName;
        }
    }

    private static class DepartmentAggregate {
        private BigDecimal internalCost = BigDecimal.ZERO;
        private BigDecimal externalCost = BigDecimal.ZERO;
    }
}
