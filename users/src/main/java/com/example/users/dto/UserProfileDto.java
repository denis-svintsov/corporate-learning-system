package com.example.users.dto;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String positionId,
        String positionTitle,
        String departmentId,
        String departmentName,
        LocalDate hireDate,
        String status,
        Set<String> roles
) {
}
