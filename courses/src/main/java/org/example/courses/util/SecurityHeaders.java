package org.example.courses.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityHeaders {
    private SecurityHeaders() {}

    public static boolean hasAnyRole(Set<String> roles, String... allowedRoles) {
        if (roles == null || roles.isEmpty()) return false;
        for (String role : allowedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
