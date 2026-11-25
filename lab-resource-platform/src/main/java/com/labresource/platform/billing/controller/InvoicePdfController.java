
package com.labresource.platform.billing.controller;

import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.billing.service.InvoicePdfService;
import com.labresource.platform.billing.service.InvoiceService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/billing/invoices")
public class InvoicePdfController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;
    private final UserRepository userRepository;

    public InvoicePdfController(
            InvoiceService invoiceService,
            InvoicePdfService pdfService,
            UserRepository userRepository
    ) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN','DEPARTMENT_HEAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long invoiceId,
            Authentication authentication
    ) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid invoice ID"
            );
        }

        User user = userRepository
                .findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        Invoice invoice = invoiceService.getInvoiceDetails(invoiceId);

        validateAccess(user, invoice);

        byte[] pdf = pdfService.generate(invoice);

        String invoiceNumber = invoice.getInvoiceNumber() == null
                ? String.valueOf(invoice.getId())
                : invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9_-]", "_");

        String filename = "invoice-" + invoiceNumber + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"
                )
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(pdf);
    }

    private void validateAccess(User user, Invoice invoice) {
        if (user.getRole() == null
                || user.getRole().getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User role is not configured"
            );
        }

        String role = user.getRole().getName()
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("ROLE_", "");

        if ("SYSTEM_ADMIN".equals(role)) {
            return;
        }

        if ("INSTITUTION_ADMIN".equals(role)) {
            if (user.getInstitution() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Institution is not assigned"
                );
            }

            Long institutionId = user.getInstitution().getId();

            boolean issuerMatch = institutionId != null
                    && institutionId.equals(invoice.getIssuerInstitutionId());

            boolean recipientMatch = institutionId != null
                    && institutionId.equals(invoice.getRecipientInstitutionId());

            if (issuerMatch || recipientMatch) {
                return;
            }
        }

        if ("DEPARTMENT_HEAD".equals(role)) {
            if (user.getDepartment() == null
                    || user.getDepartment().getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Department is not assigned"
                );
            }

            if (user.getDepartment().getId().equals(
                    invoice.getRecipientDepartmentId()
            )) {
                return;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have access to this invoice"
        );
    }
}
