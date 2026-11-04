
package com.labresource.platform.user.config;

import com.labresource.platform.user.entity.Role;
import com.labresource.platform.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RoleDataInitializer {

    @Bean
    CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.saveAll(List.of(
                        new Role("RESEARCHER_STUDENT", "Student or researcher"),
                        new Role("LAB_TECHNICIAN", "Laboratory technician"),
                        new Role("LAB_MANAGER", "Laboratory manager"),
                        new Role("DEPARTMENT_HEAD", "Department head"),
                        new Role("INSTITUTION_ADMIN", "Institution administrator"),
                        new Role("SYSTEM_ADMIN", "System administrator")
                ));

                System.out.println("Default roles inserted successfully!");
            }
        };
    }
}
