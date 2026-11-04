
package com.labresource.platform.resource.controller;

import com.labresource.platform.department.entity.Department;
import com.labresource.platform.department.repository.DepartmentRepository;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.institution.repository.InstitutionRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;

    public ResourceController(
            ResourceRepository resourceRepository,
            InstitutionRepository institutionRepository,
            DepartmentRepository departmentRepository) {
        this.resourceRepository = resourceRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Resource> getAllResources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long departmentId) {

        return resourceRepository.findAll()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> category == null || category.isBlank()
                        || equalsIgnoreCase(r.getCategory(), category))
                .filter(r -> location == null || location.isBlank()
                        || containsIgnoreCase(r.getLocation(), location))
                .filter(r -> status == null || status.isBlank()
                        || equalsIgnoreCase(r.getStatus(), status))
                .filter(r -> search == null || search.isBlank()
                        || containsIgnoreCase(r.getName(), search)
                        || containsIgnoreCase(r.getDescription(), search)
                        || containsIgnoreCase(r.getSpecifications(), search))
                .filter(r -> tag == null || tag.isBlank()
                        || (r.getTags() != null && r.getTags().stream()
                        .anyMatch(t -> equalsIgnoreCase(t, tag))))
                .filter(r -> institutionId == null
                        || (r.getInstitution() != null
                        && institutionId.equals(r.getInstitution().getId())))
                .filter(r -> departmentId == null
                        || (r.getDepartment() != null
                        && departmentId.equals(r.getDepartment().getId())))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Resource getResourceById(@PathVariable Long id) {
        Resource resource = getExistingResource(id);

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Resource is inactive");
        }

        return resource;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Resource createResource(@RequestBody Resource resource) {

        validateResource(resource);
        applyInstitutionDepartment(resource,
                getInstitutionId(resource),
                getDepartmentId(resource));

        resource.setAvailableQuantity(resource.getQuantity());
        resource.setStatus("AVAILABLE");
        resource.setIsActive(true);

        return resourceRepository.save(resource);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Resource updateResource(
            @PathVariable Long id,
            @RequestBody Resource details) {

        Resource existing = getExistingResource(id);

        if (!Boolean.TRUE.equals(existing.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Resource is inactive");
        }

        validateResource(details);

        int oldQuantity = existing.getQuantity();
        int oldAvailable = existing.getAvailableQuantity() == null
                ? oldQuantity : existing.getAvailableQuantity();

        int usedQuantity = oldQuantity - oldAvailable;

        if (details.getQuantity() < usedQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity cannot be less than currently allocated quantity");
        }

        applyInstitutionDepartment(existing,
                getInstitutionId(details),
                getDepartmentId(details));

        existing.setName(details.getName());
        existing.setCategory(details.getCategory());
        existing.setDescription(details.getDescription());
        existing.setSpecifications(details.getSpecifications());
        existing.setDocumentationUrl(details.getDocumentationUrl());
        existing.setTags(details.getTags() == null
                ? Set.of() : details.getTags());
        existing.setLocation(details.getLocation());
        existing.setQuantity(details.getQuantity());
        existing.setAvailableQuantity(details.getQuantity() - usedQuantity);

        return resourceRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public void deactivateResource(@PathVariable Long id) {

        Resource resource = getExistingResource(id);

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Resource is already inactive");
        }

        resource.setIsActive(false);
        resource.setStatus("INACTIVE");

        resourceRepository.save(resource);
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    @Transactional(readOnly = true)
    public List<Resource> getInactiveResources() {
        return resourceRepository.findAll()
                .stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsActive()))
                .toList();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Resource reactivateResource(@PathVariable Long id) {

        Resource resource = getExistingResource(id);

        if (Boolean.TRUE.equals(resource.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Resource is already active");
        }

        resource.setIsActive(true);
        resource.setStatus("AVAILABLE");

        return resourceRepository.save(resource);
    }

    private void applyInstitutionDepartment(
            Resource resource,
            Long institutionId,
            Long departmentId) {

        if (institutionId == null) {
            if (departmentId != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Institution is required when department is provided");
            }

            resource.setInstitution(null);
            resource.setDepartment(null);
            return;
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Institution not found"));

        if (Boolean.FALSE.equals(institution.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Institution is inactive");
        }

        resource.setInstitution(institution);

        if (departmentId == null) {
            resource.setDepartment(null);
            return;
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"));

        if (department.getInstitution() == null
                || !institutionId.equals(department.getInstitution().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Department does not belong to the selected institution");
        }

        if (Boolean.FALSE.equals(department.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Department is inactive");
        }

        resource.setDepartment(department);
    }

    private Long getInstitutionId(Resource resource) {
        return resource.getInstitution() == null
                ? null : resource.getInstitution().getId();
    }

    private Long getDepartmentId(Resource resource) {
        return resource.getDepartment() == null
                ? null : resource.getDepartment().getId();
    }

    private Resource getExistingResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Resource not found"));
    }

    private void validateResource(Resource resource) {

        if (resource == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Resource data is required");
        }

        if (resource.getName() == null || resource.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Resource name is required");
        }

        if (resource.getCategory() == null || resource.getCategory().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Category is required");
        }

        if (resource.getLocation() == null || resource.getLocation().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Location is required");
        }

        if (resource.getQuantity() == null || resource.getQuantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero");
        }
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null && first.equalsIgnoreCase(second.trim());
    }

    private boolean containsIgnoreCase(String source, String search) {
        return source != null
                && source.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Resource updateResourceStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        Resource resource = getExistingResource(id);

        Set<String> allowedStatuses = Set.of(
                "AVAILABLE",
                "BOOKED",
                "UNDER_MAINTENANCE",
                "OUT_OF_SERVICE",
                "RETIRED"
        );

        String normalizedStatus = status.trim().toUpperCase();

        if (!allowedStatuses.contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid resource status"
            );
        }

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive resource status cannot be changed"
            );
        }

        resource.setStatus(normalizedStatus);

        return resourceRepository.save(resource);
    }
}
