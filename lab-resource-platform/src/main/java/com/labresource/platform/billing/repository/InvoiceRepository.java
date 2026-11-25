
package com.labresource.platform.billing.repository;

import com.labresource.platform.billing.entity.Invoice;
import com.labresource.platform.billing.enums.InvoiceStatus;
import com.labresource.platform.billing.enums.InvoiceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Generate unique invoice number
    long countByInvoiceNumberStartingWith(String prefix);

    // Check whether a usage charge is already linked to an invoice
    boolean existsByCharges_Id(Long chargeId);

    // Check duplicate external invoice for same recipient and period
    boolean existsByIssuerInstitutionIdAndInvoiceTypeAndRecipientInstitutionIdAndPeriodStartAndPeriodEnd(
            Long issuerInstitutionId,
            InvoiceType invoiceType,
            Long recipientInstitutionId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    // Check duplicate department chargeback invoice
    boolean existsByIssuerInstitutionIdAndInvoiceTypeAndRecipientDepartmentIdAndPeriodStartAndPeriodEnd(
            Long issuerInstitutionId,
            InvoiceType invoiceType,
            Long recipientDepartmentId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    // Issued invoices with pagination
    Page<Invoice> findByIssuerInstitutionId(
            Long institutionId,
            Pageable pageable
    );

    // Received invoices with pagination
    Page<Invoice> findByRecipientInstitutionId(
            Long institutionId,
            Pageable pageable
    );

    // Department invoices with pagination
    Page<Invoice> findByRecipientDepartmentId(
            Long departmentId,
            Pageable pageable
    );

    // All issued invoices
    List<Invoice> findByIssuerInstitutionId(Long institutionId);

    // All received invoices
    List<Invoice> findByRecipientInstitutionId(Long institutionId);

    // Issued invoices filtered by status
    Page<Invoice> findByIssuerInstitutionIdAndStatus(
            Long institutionId,
            InvoiceStatus status,
            Pageable pageable
    );

    // Received invoices filtered by status
    Page<Invoice> findByRecipientInstitutionIdAndStatus(
            Long institutionId,
            InvoiceStatus status,
            Pageable pageable
    );

    // Fetch invoice with linked charges and their resource/booking
    @Query("""
        SELECT DISTINCT i
        FROM Invoice i
        LEFT JOIN FETCH i.charges c
        LEFT JOIN FETCH c.resource
        LEFT JOIN FETCH c.booking
        WHERE i.id = :invoiceId
    """)
    Optional<Invoice> findDetailedById(
            @Param("invoiceId") Long invoiceId
    );
}
