
package com.labresource.platform.budget.repository;

import com.labresource.platform.budget.entity.DepartmentBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentBudgetRepository
        extends JpaRepository<DepartmentBudget, Long> {

    Optional<DepartmentBudget> findByDepartment_IdAndFiscalYear(
            Long departmentId,
            Integer fiscalYear
    );

    List<DepartmentBudget> findByDepartment_Institution_IdAndFiscalYear(
            Long institutionId,
            Integer fiscalYear
    );

    boolean existsByDepartment_IdAndFiscalYear(
            Long departmentId,
            Integer fiscalYear
    );
}
