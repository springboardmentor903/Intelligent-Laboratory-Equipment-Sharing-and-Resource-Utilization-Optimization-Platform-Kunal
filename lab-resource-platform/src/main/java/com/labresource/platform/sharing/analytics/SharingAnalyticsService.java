package com.labresource.platform.sharing.analytics;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SharingAnalyticsService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public SharingAnalyticsService(
            EntityManager entityManager,
            UserRepository userRepository
    ) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SharingAnalyticsResponse getAnalytics(
            LocalDate fromDate,
            LocalDate toDate,
            String email
    ) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String role = user.getRole() == null
                ? ""
                : user.getRole().getName().replace("ROLE_", "");

        Long institutionId = user.getInstitution() == null
                ? null
                : user.getInstitution().getId();

        Long departmentId = user.getDepartment() == null
                ? null
                : user.getDepartment().getId();

        boolean systemWide = "SYSTEM_ADMIN".equals(role);
        boolean departmentScope = "DEPARTMENT_HEAD".equals(role);

        if (!systemWide &&
                !List.of(
                        "INSTITUTION_ADMIN",
                        "LAB_MANAGER",
                        "DEPARTMENT_HEAD"
                ).contains(role)) {

            throw new IllegalArgumentException(
                    "You are not authorized to view sharing analytics");
        }

        if (!systemWide && departmentScope && departmentId == null) {
            throw new IllegalArgumentException(
                    "Department is not assigned to this user");
        }

        if (!systemWide && !departmentScope && institutionId == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this user");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay();

        String scope = systemWide
                ? ""
                : departmentScope
                  ? " AND r.department_id = :scopeId "
                  : " AND (rs.shared_by_institution_id = :scopeId " +
                    "OR rs.shared_with_institution_id = :scopeId) ";

        String totalsSql = """
            SELECT COUNT(eb.id),
                   COALESCE(SUM(
                       EXTRACT(EPOCH FROM (eb.end_time - eb.start_time))
                       / 3600.0
                   ), 0),
                   COALESCE(SUM(
                       (EXTRACT(EPOCH FROM (eb.end_time - eb.start_time))
                       / 3600.0)
                       * COALESCE(rs.fee_per_hour, 0)
                   ), 0)
            FROM external_bookings eb
            JOIN resource_shares rs ON rs.id = eb.resource_share_id
            JOIN resources r ON r.id = rs.resource_id
            WHERE eb.status = 'APPROVED'
              AND eb.start_time >= :fromTime
              AND eb.start_time < :toTime
            """ + scope;

        Query totalsQuery =
                entityManager.createNativeQuery(totalsSql);

        totalsQuery.setParameter("fromTime", from);
        totalsQuery.setParameter("toTime", to);

        if (!systemWide) {
            totalsQuery.setParameter(
                    "scopeId",
                    departmentScope ? departmentId : institutionId
            );
        }

        Object[] totals = (Object[]) totalsQuery.getSingleResult();

        long bookingCount =
                ((Number) totals[0]).longValue();

        double hours =
                ((Number) totals[1]).doubleValue();

        BigDecimal fees =
                new BigDecimal(totals[2].toString());

        String partnerSql = """
            SELECT partner.id,
                   partner.name,
                   COUNT(eb.id),
                   COALESCE(SUM(
                       EXTRACT(EPOCH FROM (eb.end_time - eb.start_time))
                       / 3600.0
                   ), 0),
                   COALESCE(SUM(
                       (EXTRACT(EPOCH FROM (eb.end_time - eb.start_time))
                       / 3600.0)
                       * COALESCE(rs.fee_per_hour, 0)
                   ), 0)
            FROM external_bookings eb
            JOIN resource_shares rs ON rs.id = eb.resource_share_id
            JOIN resources r ON r.id = rs.resource_id
            JOIN institutions partner ON partner.id =
                CASE
                    WHEN rs.shared_by_institution_id = :institutionId
                    THEN rs.shared_with_institution_id
                    ELSE rs.shared_by_institution_id
                END
            WHERE eb.status = 'APPROVED'
              AND eb.start_time >= :fromTime
              AND eb.start_time < :toTime
            """ + (systemWide ? """
            GROUP BY partner.id, partner.name
            ORDER BY 4 DESC
            """ : departmentScope ? """
            AND r.department_id = :scopeId
            GROUP BY partner.id, partner.name
            ORDER BY 4 DESC
            """ : """
            AND (
                rs.shared_by_institution_id = :institutionId
                OR rs.shared_with_institution_id = :institutionId
            )
            GROUP BY partner.id, partner.name
            ORDER BY 4 DESC
            """);

        Query partnerQuery =
                entityManager.createNativeQuery(partnerSql);

        partnerQuery.setParameter(
                "institutionId",
                institutionId == null ? -1L : institutionId
        );

        partnerQuery.setParameter("fromTime", from);
        partnerQuery.setParameter("toTime", to);

        if (departmentScope) {
            partnerQuery.setParameter("scopeId", departmentId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = partnerQuery.getResultList();

        List<SharingAnalyticsResponse.PartnerMetric> partners =
                new ArrayList<>();

        for (Object[] row : rows) {

            partners.add(
                    new SharingAnalyticsResponse.PartnerMetric(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).doubleValue(),
                            new BigDecimal(row[4].toString())
                    )
            );
        }

        return new SharingAnalyticsResponse(
                bookingCount,
                hours,
                fees,
                partners.size(),
                partners
        );
    }
}