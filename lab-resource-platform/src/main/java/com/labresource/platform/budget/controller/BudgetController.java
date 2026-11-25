
package com.labresource.platform.budget.controller;

import com.labresource.platform.budget.dto.BudgetRequest;
import com.labresource.platform.budget.dto.BudgetResponse;
import com.labresource.platform.budget.service.BudgetService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    public BudgetController(
            BudgetService budgetService,
            UserRepository userRepository) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
    }

    // Institution Admin: create or update department budget
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public BudgetResponse createBudget(
            Authentication authentication,
            @RequestBody BudgetRequest request) {

        User user = getCurrentUser(authentication);

        return budgetService.createOrUpdateBudget(
                getInstitutionId(user),
                request
        );
    }

    // Institution Admin: update existing budget
    @PutMapping
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public BudgetResponse updateBudget(
            Authentication authentication,
            @RequestBody BudgetRequest request) {

        User user = getCurrentUser(authentication);

        return budgetService.createOrUpdateBudget(
                getInstitutionId(user),
                request
        );
    }

    // Institution Admin: view all department budgets
    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public List<BudgetResponse> getAllBudgets(
            Authentication authentication,
            @RequestParam Integer fiscalYear) {

        User user = getCurrentUser(authentication);

        return budgetService.getDepartmentBudgets(
                getInstitutionId(user),
                fiscalYear
        );
    }

    // Department Head: view only their own department budget
    @GetMapping("/my-department")
    @PreAuthorize("hasRole('DEPARTMENT_HEAD')")
    public BudgetResponse getMyDepartmentBudget(
            Authentication authentication,
            @RequestParam Integer fiscalYear) {

        User user = getCurrentUser(authentication);

        if (user.getInstitution() == null || user.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is not linked to a department"
            );
        }

        return budgetService.getDepartmentBudgets(
                        user.getInstitution().getId(),
                        fiscalYear
                )
                .stream()
                .filter(b -> b.getDepartmentId()
                        .equals(user.getDepartment().getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department budget not found"
                ));
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private Long getInstitutionId(User user) {
        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not linked to an institution"
            );
        }

        return user.getInstitution().getId();
    }
}
