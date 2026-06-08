package com.example.users.dto;

public record DepartmentDto(
        String departmentId,
        String name,
        String description,
        String managerId,
        String parentDepartmentId
) {
}
