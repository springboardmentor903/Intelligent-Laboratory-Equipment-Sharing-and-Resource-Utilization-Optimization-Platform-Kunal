package com.labresource.platform.usage.controller;

import com.labresource.platform.usage.dto.UsageChargePageResponse;
import com.labresource.platform.usage.dto.UsageChargeResponse;
import com.labresource.platform.usage.entity.UsageCharge;
import com.labresource.platform.usage.repository.UsageChargeRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/usage-charges")
public class UsageChargeController {

    private final UsageChargeRepository chargeRepository;
    private final UserRepository userRepository;

    public UsageChargeController(
            UsageChargeRepository chargeRepository,
            UserRepository userRepository) {
        this.chargeRepository = chargeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public UsageChargePageResponse getMyCharges(
            Authentication authentication,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String chargeType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getUser(authentication);

        return buildResponse(
                chargeRepository.findUserCharges(
                        user.getId(),
                        startOfDay(fromDate),
                        endExclusive(toDate),
                        parseType(chargeType),
                        blankToNull(status),
                        pageRequest(page, size)
                ),
                chargeRepository.sumUserCharges(
                        user.getId(),
                        startOfDay(fromDate),
                        endExclusive(toDate),
                        parseType(chargeType),
                        blankToNull(status)
                )
        );
    }

    @GetMapping("/institution")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public UsageChargePageResponse getInstitutionCharges(
            Authentication authentication,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String chargeType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getUser(authentication);

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is not linked to an institution"
            );
        }

        Long institutionId = user.getInstitution().getId();

        return buildResponse(
                chargeRepository.findInstitutionCharges(
                        institutionId,
                        startOfDay(fromDate),
                        endExclusive(toDate),
                        parseType(chargeType),
                        blankToNull(status),
                        pageRequest(page, size)
                ),
                chargeRepository.sumInstitutionCharges(
                        institutionId,
                        startOfDay(fromDate),
                        endExclusive(toDate),
                        parseType(chargeType),
                        blankToNull(status)
                )
        );
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private UsageChargePageResponse buildResponse(
            Page<UsageCharge> result,
            BigDecimal totalAmount) {

        return new UsageChargePageResponse(
                result.getContent().stream()
                        .map(UsageChargeResponse::new)
                        .toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                totalAmount == null ? BigDecimal.ZERO : totalAmount
        );
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination values"
            );
        }

        return PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null :
                date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant endExclusive(LocalDate date) {
        return date == null ? null :
                date.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private UsageCharge.ChargeType parseType(String type) {
        if (type == null || type.isBlank()) return null;

        try {
            return UsageCharge.ChargeType.valueOf(
                    type.trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid chargeType. Use INTERNAL, EXTERNAL or UNASSIGNED"
            );
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}