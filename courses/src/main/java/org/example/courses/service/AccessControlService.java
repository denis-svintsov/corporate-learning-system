package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.model.Course;
import org.example.courses.users.UsersServiceClient;
import org.example.courses.util.CsvUtil;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UsersServiceClient usersServiceClient;

    public boolean canAccessCourse(String userId, Course course) {
        Set<String> allowedRoles = CsvUtil.splitToSet(course.getAllowedRolesCsv());
        Set<String> allowedDepartments = CsvUtil.splitToSet(course.getAllowedDepartmentIdsCsv());
        return canAccessCourse(userId, allowedRoles, allowedDepartments);
    }

    public boolean canAccessCourse(String userId, Set<String> allowedRoles, Set<String> allowedDepartments) {
        var userContext = usersServiceClient.getUserContext(userId);
        if (userContext == null) return false;

        Set<String> userRoles = userContext.roles().stream()
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        // Администраторы/HR всегда имеют доступ к просмотру курса.
        if (userRoles.contains("ADMIN") || userRoles.contains("HR")) {
            return true;
        }

        // Нет ограничений — доступно всем
        if (allowedRoles.isEmpty() && allowedDepartments.isEmpty()) {
            return true;
        }

        if (!allowedDepartments.isEmpty()) {
            String deptId = userContext.departmentId();
            if (deptId == null || !allowedDepartments.contains(deptId)) {
                return false;
            }
        }

        if (!allowedRoles.isEmpty()) {
            // allowedRoles тоже нормализуем к верхнему регистру при сравнении
            Set<String> allowedNorm = allowedRoles.stream()
                    .map(s -> s.trim().toUpperCase())
                    .collect(Collectors.toSet());
            boolean ok = userRoles.stream().anyMatch(allowedNorm::contains);
            if (!ok) return false;
        }

        return true;
    }
}
