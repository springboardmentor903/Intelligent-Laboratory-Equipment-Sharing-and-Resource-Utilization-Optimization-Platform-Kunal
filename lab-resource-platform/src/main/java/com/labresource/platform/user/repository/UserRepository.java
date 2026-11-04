
package com.labresource.platform.user.repository;

import com.labresource.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Login ke liye: Role, Institution aur Department ko saath mein fetch karega
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithRole(@Param("email") String email);

    // User profile ke liye
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.department
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    // Email se normal user search
    Optional<User> findByEmail(String email);

    // Find active institution administrators
    Optional<User> findFirstByInstitution_IdAndRole_NameAndIsActiveTrueOrderByIdAsc(
            Long institutionId,
            String roleName
    );

    // Registration mein duplicate email check
    boolean existsByEmail(String email);

    // Google OAuth mein Google ID check
    boolean existsByGoogleId(String googleId);
}
