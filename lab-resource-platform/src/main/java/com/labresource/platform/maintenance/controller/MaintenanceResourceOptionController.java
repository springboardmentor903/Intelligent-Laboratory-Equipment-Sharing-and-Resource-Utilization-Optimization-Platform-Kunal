
package com.labresource.platform.resource.controller;

import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class MaintenanceResourceOptionController {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public MaintenanceResourceOptionController(
            ResourceRepository resourceRepository,
            UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public record ResourceOption(
            Long id,
            String name,
            String category,
            String location
    ) {}

    @GetMapping("/maintenance-options")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    @Transactional(readOnly = true)
    public List<ResourceOption> getMaintenanceResourceOptions(
            Authentication authentication) {

        User manager = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found"));

        if (manager.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Institution is not assigned to this manager");
        }

        Long institutionId = manager.getInstitution().getId();

        return resourceRepository.findAll()
                .stream()
                .filter(resource ->
                        Boolean.TRUE.equals(resource.getIsActive()))
                .filter(resource ->
                        resource.getInstitution() != null
                                && institutionId.equals(
                                resource.getInstitution().getId()))
                .map(resource -> new ResourceOption(
                        resource.getId(),
                        resource.getName(),
                        resource.getCategory(),
                        resource.getLocation()
                ))
                .toList();
    }
}
