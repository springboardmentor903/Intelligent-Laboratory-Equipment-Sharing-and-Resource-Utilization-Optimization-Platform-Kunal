
package com.labresource.platform.user.service;

import com.labresource.platform.department.entity.Department;
import com.labresource.platform.department.repository.DepartmentRepository;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.institution.repository.InstitutionRepository;
import com.labresource.platform.user.dto.LoginRequest;
import com.labresource.platform.user.dto.UserProfileUpdateRequest;
import com.labresource.platform.user.dto.UserRegistrationRequest;
import com.labresource.platform.user.entity.Role;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.RoleRepository;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            InstitutionRepository institutionRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return userRepository.existsByEmail(
                email.trim().toLowerCase(Locale.ROOT)
        );
    }

    @Transactional
    public User createUser(UserRegistrationRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists. Please use another email."
            );
        }

        if (request.getRoleId() == null) {
            throw new IllegalArgumentException("Please select a role.");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Selected role not found.")
                );

        if ("SYSTEM_ADMIN".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException(
                    "SYSTEM_ADMIN cannot be assigned through public registration."
            );
        }

        if (request.getInstitutionId() == null) {
            throw new IllegalArgumentException(
                    "Please select an institution."
            );
        }

        Institution institution = institutionRepository
                .findById(request.getInstitutionId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected institution not found."
                        )
                );

        if (request.getDepartmentId() == null) {
            throw new IllegalArgumentException(
                    "Please select a department."
            );
        }

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected department not found."
                        )
                );

        if (department.getInstitution() == null
                || department.getInstitution().getId() == null
                || !department.getInstitution().getId()
                .equals(institution.getId())) {

            throw new IllegalArgumentException(
                    "Selected department does not belong to the selected institution."
            );
        }

        if (Boolean.FALSE.equals(institution.getIsActive())) {
            throw new IllegalArgumentException(
                    "Selected institution is inactive."
            );
        }

        if (Boolean.FALSE.equals(department.getIsActive())) {
            throw new IllegalArgumentException(
                    "Selected department is inactive."
            );
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setPhone(request.getPhone());
        user.setAuthProvider("LOCAL");
        user.setEmailVerified(false);
        user.setIsActive(true);
        user.setRole(role);
        user.setInstitution(institution);
        user.setDepartment(department);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getMyProfile(String authenticatedEmail) {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated email is required."
            );
        }

        String email = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User profile not found.")
                );
    }

    // UPDATE MY PROFILE
    @Transactional
    public User updateMyProfile(
            String authenticatedEmail,
            UserProfileUpdateRequest request) {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated email is required."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Profile update data is required."
            );
        }

        String email = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User profile not found.")
                );

        // Update Full Name
        if (request.getFullName() == null
                || request.getFullName().isBlank()) {

            throw new IllegalArgumentException("Full name is required.");
        }

        String fullName = request.getFullName().trim();

        if (fullName.length() > 150) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 150 characters."
            );
        }

        user.setFullName(fullName);

        // Update Phone only when supplied
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();

            if (phone.length() > 30) {
                throw new IllegalArgumentException(
                        "Phone cannot exceed 30 characters."
                );
            }

            user.setPhone(phone.isBlank() ? null : phone);
        }

        // Update Institution and Department
        if (request.getInstitutionId() != null
                || request.getDepartmentId() != null) {

            if (request.getInstitutionId() == null
                    || request.getDepartmentId() == null) {

                throw new IllegalArgumentException(
                        "Both institution and department are required."
                );
            }

            Institution institution = institutionRepository
                    .findById(request.getInstitutionId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected institution not found."
                            )
                    );

            Department department = departmentRepository
                    .findById(request.getDepartmentId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected department not found."
                            )
                    );

            if (Boolean.FALSE.equals(institution.getIsActive())) {
                throw new IllegalArgumentException(
                        "Selected institution is inactive."
                );
            }

            if (Boolean.FALSE.equals(department.getIsActive())) {
                throw new IllegalArgumentException(
                        "Selected department is inactive."
                );
            }

            if (department.getInstitution() == null
                    || !institution.getId().equals(
                    department.getInstitution().getId())) {

                throw new IllegalArgumentException(
                        "Selected department does not belong to the selected institution."
                );
            }

            user.setInstitution(institution);
            user.setDepartment(department);
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isEmpty()) {

            throw new IllegalArgumentException(
                    "Email and password are required."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive");
        }

        return user;
    }
}
