
package com.labresource.platform.usage.repository;

import com.labresource.platform.usage.entity.UsageCharge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageChargeRepository extends JpaRepository<UsageCharge, Long> {

    boolean existsByBookingId(Long bookingId);

    Optional<UsageCharge> findByBookingId(Long bookingId);

    // User's own usage charges
    @EntityGraph(attributePaths = {"resource", "booking"})
    @Query("""
        SELECT uc FROM UsageCharge uc
        WHERE uc.user.id = :userId
        AND uc.createdAt >= COALESCE(:fromDate, uc.createdAt)
        AND uc.createdAt <= COALESCE(:toDate, uc.createdAt)
        AND uc.chargeType = COALESCE(:chargeType, uc.chargeType)
        AND uc.chargeStatus = COALESCE(:status, uc.chargeStatus)
        ORDER BY uc.createdAt DESC
        """)
    Page<UsageCharge> findUserCharges(
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("chargeType") UsageCharge.ChargeType chargeType,
            @Param("status") String status,
            Pageable pageable
    );

    // Institution-wise usage charges
    @EntityGraph(attributePaths = {"resource", "booking"})
    @Query("""
        SELECT uc FROM UsageCharge uc
        WHERE uc.owningInstitutionId = :institutionId
        AND uc.createdAt >= COALESCE(:fromDate, uc.createdAt)
        AND uc.createdAt <= COALESCE(:toDate, uc.createdAt)
        AND uc.chargeType = COALESCE(:chargeType, uc.chargeType)
        AND uc.chargeStatus = COALESCE(:status, uc.chargeStatus)
        ORDER BY uc.createdAt DESC
        """)
    Page<UsageCharge> findInstitutionCharges(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("chargeType") UsageCharge.ChargeType chargeType,
            @Param("status") String status,
            Pageable pageable
    );

    // Total amount for user's charges
    @Query("""
        SELECT COALESCE(SUM(uc.amount), 0)
        FROM UsageCharge uc
        WHERE uc.user.id = :userId
        AND uc.createdAt >= COALESCE(:fromDate, uc.createdAt)
        AND uc.createdAt <= COALESCE(:toDate, uc.createdAt)
        AND uc.chargeType = COALESCE(:chargeType, uc.chargeType)
        AND uc.chargeStatus = COALESCE(:status, uc.chargeStatus)
        """)
    BigDecimal sumUserCharges(
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("chargeType") UsageCharge.ChargeType chargeType,
            @Param("status") String status
    );

    // Total amount for institution's charges
    @Query("""
        SELECT COALESCE(SUM(uc.amount), 0)
        FROM UsageCharge uc
        WHERE uc.owningInstitutionId = :institutionId
        AND uc.createdAt >= COALESCE(:fromDate, uc.createdAt)
        AND uc.createdAt <= COALESCE(:toDate, uc.createdAt)
        AND uc.chargeType = COALESCE(:chargeType, uc.chargeType)
        AND uc.chargeStatus = COALESCE(:status, uc.chargeStatus)
        """)
    BigDecimal sumInstitutionCharges(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("chargeType") UsageCharge.ChargeType chargeType,
            @Param("status") String status
    );

    // Cost overview queries
    @EntityGraph(attributePaths = {"resource", "resource.institution"})
    List<UsageCharge>
    findByOwningInstitutionIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long institutionId,
            Instant fromDate,
            Instant toDate
    );

    @EntityGraph(attributePaths = {"resource", "resource.institution"})
    List<UsageCharge> findByOwningInstitutionIdAndCreatedAtLessThan(
            Long institutionId,
            Instant toDate
    );

    @EntityGraph(attributePaths = {"resource", "resource.institution"})
    List<UsageCharge> findByOwningInstitutionIdAndCreatedAtGreaterThanEqual(
            Long institutionId,
            Instant fromDate
    );

    @EntityGraph(attributePaths = {"resource", "resource.institution"})
    List<UsageCharge> findByOwningInstitutionId(Long institutionId);

    @Query("""
        SELECT COALESCE(SUM(uc.amount), 0)
        FROM UsageCharge uc
        WHERE uc.user.id = :userId
          AND uc.createdAt >= :fromDate
          AND uc.createdAt < :toDate
    """)
    BigDecimal sumUserUsageChargesBetween(
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}
