
package com.labresource.platform.user.controller;

import com.labresource.platform.user.entity.UserRole;
import com.labresource.platform.user.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

    private final UserRoleRepository userRoleRepository;

    public UserRoleController(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @GetMapping("/user/{userId}")
    public List<UserRole> getRolesByUser(@PathVariable Long userId) {
        return userRoleRepository.findByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRole assignRole(@RequestBody UserRole userRole) {
        return userRoleRepository.save(userRole);
    }
}
