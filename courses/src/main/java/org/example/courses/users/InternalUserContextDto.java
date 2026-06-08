package org.example.courses.users;

import java.util.Set;

public record InternalUserContextDto(
        String id,
        String departmentId,
        String positionId,
        String status,
        Set<String> roles
) {
}
