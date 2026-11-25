
package com.labresource.platform.billing.controller;

import com.labresource.platform.billing.dto.FinancialSummaryResponse;
import com.labresource.platform.billing.service.FinancialSummaryService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/financial-summary")
public class FinancialSummaryController {

    private final FinancialSummaryService financialSummaryService;
    private final UserRepository userRepository;

    public FinancialSummaryController(
            FinancialSummaryService financialSummaryService,
            UserRepository userRepository
    ) {
        this.financialSummaryService = financialSummaryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public FinancialSummaryResponse getSummary(
            Authentication authentication,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        User user = userRepository
                .findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "User has no institution");
        }

        return financialSummaryService.getSummary(
                user.getInstitution().getId(),
                startDate,
                endDate
        );
    }
}
