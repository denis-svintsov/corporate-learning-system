package com.example.users.controller;

import com.example.users.dto.UpdateUserRequest;
import com.example.users.dto.UpdateUserRoleRequest;
import com.example.users.dto.UpdateUserSettingsRequest;
import com.example.users.dto.UserProfileDto;
import com.example.users.dto.UserSettingsDto;
import com.example.users.service.UserService;
import com.example.users.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserSettingsService userSettingsService;

    public UserController(UserService userService, UserSettingsService userSettingsService) {
        this.userService = userService;
        this.userSettingsService = userSettingsService;
    }

    @GetMapping("/{id}")
    public UserProfileDto getById(@PathVariable String id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public UserProfileDto update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @GetMapping
    public List<UserProfileDto> byDepartment(@RequestParam("department") String departmentId) {
        return userService.findByDepartment(departmentId);
    }

    @GetMapping("/search")
    public List<UserProfileDto> search(@RequestParam("name") String name) {
        return userService.searchByName(name);
    }

    @PutMapping("/{id}/role")
    public UserProfileDto updateRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        if (!roles.contains("HR") && !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR/Admin role required");
        }
        return userService.updateRole(id, request);
    }

    @GetMapping("/{id}/settings")
    public UserSettingsDto getSettings(@PathVariable String id) {
        return userSettingsService.getByUserId(id);
    }

    @PutMapping("/{id}/settings")
    public UserSettingsDto updateSettings(@PathVariable String id, @RequestBody UpdateUserSettingsRequest request) {
        return userSettingsService.update(id, request);
    }

    private Set<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        return List.of(header.split(",")).stream()
                .map(v -> v == null ? "" : v.trim().toUpperCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .collect(Collectors.toSet());
    }
}
