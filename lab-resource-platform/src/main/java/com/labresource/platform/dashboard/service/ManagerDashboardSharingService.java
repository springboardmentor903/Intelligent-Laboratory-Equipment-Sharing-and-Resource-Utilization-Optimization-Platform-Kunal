package com.labresource.platform.dashboard.service;

import com.labresource.platform.dashboard.dto.ManagerDashboardResponse;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ManagerDashboardSharingService {

    private final EntityManager entityManager;

    public ManagerDashboardSharingService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ManagerDashboardResponse.DepartmentSharingRequest>
    getDepartmentSharingRequests(Long departmentId) {

        if (departmentId == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    rs.id,
                    r.id,
                    r.name,
                    COALESCE(requester_department.name, ''),
                    COALESCE(target_institution.name, ''),
                    rs.created_at,
                    rs.status
                FROM resource_shares rs
                JOIN resources r ON r.id = rs.resource_id
                LEFT JOIN users requester
                    ON requester.id = rs.shared_by_user_id
                LEFT JOIN departments requester_department
                    ON requester_department.id = requester.department_id
                LEFT JOIN institutions target_institution
                    ON target_institution.id = rs.shared_with_institution_id
                WHERE r.department_id = :departmentId
                  AND rs.status = 'PENDING'
                ORDER BY rs.created_at DESC
                LIMIT 20
                """)
                .setParameter("departmentId", departmentId)
                .getResultList();

        List<ManagerDashboardResponse.DepartmentSharingRequest> result =
                new ArrayList<>();

        for (Object[] row : rows) {
            result.add(
                    new ManagerDashboardResponse.DepartmentSharingRequest(
                            ((Number) row[0]).longValue(),
                            ((Number) row[1]).longValue(),
                            (String) row[2],
                            (String) row[3],
                            (String) row[4],
                            (java.time.LocalDateTime) row[5],
                            (String) row[6]
                    )
            );
        }

        return result;
    }
}