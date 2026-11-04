
package com.labresource.platform.department.repository;

import com.labresource.platform.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    boolean existsByInstitution_IdAndCode(
            Long institutionId,
            String code
    );

    List<Department> findByInstitution_Id(Long institutionId);
}
