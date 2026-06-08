package com.example.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateUserRequest(
        @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String positionId,
        String departmentId,
        LocalDate hireDate,
        @NotBlank String status
) {
}
