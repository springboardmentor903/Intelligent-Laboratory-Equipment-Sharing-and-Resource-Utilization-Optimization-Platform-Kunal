
package com.labresource.platform.billing.controller;

import com.labresource.platform.billing.dto.InvoiceDraftRequest;
import com.labresource.platform.billing.dto.InvoiceResponse;
import com.labresource.platform.billing.service.InvoiceService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserRepository userRepository;

    public InvoiceController(
            InvoiceService invoiceService,
            UserRepository userRepository
    ) {
        this.invoiceService = invoiceService;
        this.userRepository = userRepository;
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional
    public List<InvoiceResponse> generateDrafts(
            @RequestBody InvoiceDraftRequest request,
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        return invoiceService.generateDrafts(
                        user.getInstitution().getId(),
                        user.getId(),
                        request
                )
                .stream()
                .map(InvoiceResponse::new)
                .toList();
    }

    @GetMapping("/issued")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> issued(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = currentUser(authentication);

        return invoiceService.getIssuedInvoices(
                        user.getInstitution().getId(),
                        pageRequest(page, size)
                )
                .map(InvoiceResponse::new);
    }

    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> received(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = currentUser(authentication);

        return invoiceService.getReceivedInvoices(
                        user.getInstitution().getId(),
                        pageRequest(page, size)
                )
                .map(InvoiceResponse::new);
    }

    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD','INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> departmentInvoices(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = currentUser(authentication);

        if (user.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User has no department"
            );
        }

        return invoiceService.getDepartmentInvoices(
                        user.getDepartment().getId(),
                        pageRequest(page, size)
                )
                .map(InvoiceResponse::new);
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN','DEPARTMENT_HEAD')")
    @Transactional(readOnly = true)
    public InvoiceResponse details(
            @PathVariable Long invoiceId
    ) {
        return new InvoiceResponse(
                invoiceService.getInvoiceDetails(invoiceId)
        );
    }

    @PostMapping("/{invoiceId}/issue")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional
    public InvoiceResponse issue(
            @PathVariable Long invoiceId
    ) {
        return new InvoiceResponse(
                invoiceService.issueInvoice(invoiceId)
        );
    }

    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional
    public InvoiceResponse pay(
            @PathVariable Long invoiceId
    ) {
        return new InvoiceResponse(
                invoiceService.markPaid(invoiceId)
        );
    }

    @PostMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    @Transactional
    public InvoiceResponse cancel(
            @PathVariable Long invoiceId
    ) {
        return new InvoiceResponse(
                invoiceService.cancelInvoice(invoiceId)
        );
    }

    private User currentUser(Authentication authentication) {
        return userRepository
                .findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination values"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
