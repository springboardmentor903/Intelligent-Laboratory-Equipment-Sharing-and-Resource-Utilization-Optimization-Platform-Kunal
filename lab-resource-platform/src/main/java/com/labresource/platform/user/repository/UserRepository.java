
package com.labresource.platform.user.repository;

import com.labresource.platform.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    Optional<User> findByEmail(String email);

    Optional<User> findFirstByInstitution_IdAndRole_NameAndIsActiveTrueOrderByIdAsc(
            Long institutionId,
            String roleName
    );

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    @Query("""
        SELECT u FROM User u
        JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE u.institution.id = :institutionId
          AND u.role.name = 'LAB_TECHNICIAN'
          AND u.isActive = true
        ORDER BY u.fullName ASC
    """)
    List<User> findActiveTechniciansByInstitutionId(
            @Param("institutionId") Long institutionId
    );

    @Query("""
        SELECT u FROM User u
        JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE u.institution.id = :institutionId
          AND u.role.name = 'LAB_TECHNICIAN'
          AND u.isActive = true
        ORDER BY u.fullName ASC
    """)
    List<User> findActiveTechniciansByInstitution(
            @Param("institutionId") Long institutionId
    );
}
