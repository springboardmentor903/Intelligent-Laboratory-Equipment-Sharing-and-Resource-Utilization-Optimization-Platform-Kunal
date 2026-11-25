package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.AssignTechnicianRequest;
import com.labresource.platform.maintenance.dto.CreateWorkOrderRequest;
import com.labresource.platform.maintenance.dto.MaintenanceStartPreviewResponse;
import com.labresource.platform.maintenance.dto.UpdateWorkOrderRequest;
import com.labresource.platform.maintenance.dto.WorkOrderResponse;
import com.labresource.platform.maintenance.dto.WorkOrderNoteResponse;
import com.labresource.platform.maintenance.dto.WorkOrderServiceLogResponse;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    // CREATE WORK ORDER

    @PostMapping
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<WorkOrderResponse> createWorkOrder(
            @Valid @RequestBody CreateWorkOrderRequest request,
            Authentication authentication
    ) {
        WorkOrderResponse response =
                workOrderService.createWorkOrder(
                        request,
                        authentication.getName()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET INSTITUTION WORK ORDERS

    @GetMapping
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<Page<WorkOrderResponse>> getInstitutionWorkOrders(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderType type,
            @RequestParam(required = false) Long resourceId,
            Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.getInstitutionWorkOrders(
                        authentication.getName(),
                        status,
                        type,
                        resourceId,
                        pageable
                )
        );
    }

    // GET MY ASSIGNED WORK ORDERS

    @GetMapping("/my")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<Page<WorkOrderResponse>> getMyAssignedWorkOrders(
            Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.getMyAssignedWorkOrders(
                        authentication.getName(),
                        pageable
                )
        );
    }

    // GET WORK ORDER DETAILS

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> getWorkOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.getWorkOrder(
                        id,
                        authentication.getName()
                )
        );
    }

    // ASSIGN / REASSIGN TECHNICIAN

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<WorkOrderResponse> assignTechnician(
            @PathVariable Long id,
            @Valid @RequestBody AssignTechnicianRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.assignTechnician(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // PREVIEW AFFECTED BOOKINGS BEFORE MAINTENANCE START

    @GetMapping("/{id}/maintenance-preview")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<MaintenanceStartPreviewResponse>
    previewMaintenanceStart(
            @PathVariable Long id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime expectedEnd,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.previewMaintenanceStart(
                        id,
                        expectedEnd,
                        authentication.getName()
                )
        );
    }

    // START WORK ORDER

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> startWorkOrder(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateWorkOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.startWorkOrder(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // COMPLETE WORK ORDER

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateWorkOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.completeWorkOrder(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // CANCEL WORK ORDER

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<WorkOrderResponse> cancelWorkOrder(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateWorkOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.cancelWorkOrder(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // EXTEND EXPECTED END TIME

    @PatchMapping("/{id}/extend")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<WorkOrderResponse> extendWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.extendWorkOrder(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // ADD WORK ORDER NOTE

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<WorkOrderNoteResponse> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String content = request == null
                ? null
                : request.get("content");

        WorkOrderNoteResponse note = workOrderService.addNote(
                id,
                content,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    // GET WORK ORDER NOTES

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<List<WorkOrderNoteResponse>> getNotes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.getNotes(
                        id,
                        authentication.getName()
                )
        );
    }

    // GET WORK ORDER SERVICE LOGS

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<List<WorkOrderServiceLogResponse>> getServiceLogs(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                workOrderService.getServiceLogs(
                        id,
                        authentication.getName()
                )
        );
    }
}