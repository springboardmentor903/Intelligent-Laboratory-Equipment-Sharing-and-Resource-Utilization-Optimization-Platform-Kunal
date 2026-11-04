
package com.labresource.platform.department.controller;

import com.labresource.platform.department.entity.Department;
import com.labresource.platform.department.repository.DepartmentRepository;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.institution.repository.InstitutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final InstitutionRepository institutionRepository;

    public DepartmentController(
            DepartmentRepository departmentRepository,
            InstitutionRepository institutionRepository) {

        this.departmentRepository = departmentRepository;
        this.institutionRepository = institutionRepository;
    }

    @GetMapping
    public List<Department> getAllDepartments(
            @RequestParam(required = false) Long institutionId) {

        if (institutionId != null) {
            return departmentRepository.findByInstitution_Id(institutionId);
        }

        return departmentRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department createDepartment(
            @RequestBody Map<String, Object> request) {

        Object nameValue = request.get("name");
        Object codeValue = request.get("code");
        Object institutionIdValue = request.get("institutionId");

        // Also support the existing frontend payload:
        // { institution: { id: 1 } }
        if (institutionIdValue == null
                && request.get("institution") instanceof Map<?, ?> institutionMap) {
            institutionIdValue = institutionMap.get("id");
        }

        if (nameValue == null || codeValue == null
                || institutionIdValue == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Department name, code and institutionId are required."
            );
        }

        Long institutionId;

        try {
            institutionId = Long.valueOf(institutionIdValue.toString());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid institutionId."
            );
        }

        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Institution not found."
                ));

        Department department = new Department();
        department.setName(nameValue.toString().trim());
        department.setCode(codeValue.toString().trim());
        department.setInstitution(institution);

        department.setIsActive(true);

        return departmentRepository.save(department);
    }
}
