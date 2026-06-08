package com.example.users.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(@NotBlank String role) {
}
