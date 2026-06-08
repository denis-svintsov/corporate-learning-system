package org.example.courses.users;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileDto(
        String id,
        String email,
        String firstName,
        String middleName,
        String lastName,
        String positionId,
        String positionTitle,
        String departmentId,
        String departmentName,
        LocalDate hireDate,
        String status,
        Set<String> roles
) {
    public String displayName() {
        String name = String.join(" ",
                lastName == null ? "" : lastName.trim(),
                firstName == null ? "" : firstName.trim(),
                middleName == null ? "" : middleName.trim()
        ).trim();
        if (!name.isBlank()) {
            return name;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        return id;
    }
}
