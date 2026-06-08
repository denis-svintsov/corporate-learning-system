package com.example.users.dto;

import java.time.LocalDate;
import java.util.Set;

public record ProvisionUserRequest(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String positionId,
        String departmentId,
        LocalDate hireDate,
        String status,
        Set<String> roles
) {
}
