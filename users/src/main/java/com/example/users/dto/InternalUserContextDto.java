package com.example.users.dto;

import java.util.Set;

public record InternalUserContextDto(
        String id,
        String departmentId,
        String positionId,
        String status,
        Set<String> roles
) {
}
