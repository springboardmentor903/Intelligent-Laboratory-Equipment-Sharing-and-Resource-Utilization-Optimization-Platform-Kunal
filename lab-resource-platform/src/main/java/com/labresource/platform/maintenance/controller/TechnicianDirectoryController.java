
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.TechnicianOptionResponse;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class TechnicianDirectoryController {

    private final UserRepository userRepository;

    public TechnicianDirectoryController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TechnicianOptionResponse>> getInstitutionTechnicians(
            Authentication authentication) {

        User manager = userRepository.findByEmailWithProfile(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Manager account not found."));

        if (manager.getRole() == null
                || manager.getRole().getName() == null
                || !"LAB_MANAGER".equalsIgnoreCase(manager.getRole().getName())) {
            throw new IllegalArgumentException(
                    "Only a laboratory manager can view technicians.");
        }

        if (manager.getInstitution() == null
                || manager.getInstitution().getId() == null) {
            throw new IllegalArgumentException(
                    "Your manager account is not assigned to an institution.");
        }

        List<TechnicianOptionResponse> technicians =
                userRepository.findActiveTechniciansByInstitutionId(
                                manager.getInstitution().getId())
                        .stream()
                        .map(TechnicianOptionResponse::new)
                        .toList();

        return ResponseEntity.ok(technicians);
    }
}
