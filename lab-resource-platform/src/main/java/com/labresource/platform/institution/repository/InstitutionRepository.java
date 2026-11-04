
package com.labresource.platform.institution.repository;

import com.labresource.platform.institution.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}
