package com.example.analytics.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class UsersServiceClient {

    private final RestClient restClient;

    public UsersServiceClient(RestClient.Builder builder, @Value("${users.service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<DepartmentDto> getDepartments() {
        List<DepartmentDto> result = restClient.get()
                .uri("/departments")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result == null ? List.of() : result;
    }

    public List<UserProfileDto> getDepartmentUsers(String departmentId) {
        List<UserProfileDto> result = restClient.get()
                .uri("/departments/{id}/users", departmentId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result == null ? List.of() : result;
    }

    public record DepartmentDto(
            String departmentId,
            String name,
            String description,
            String managerId,
            String parentDepartmentId
    ) {
    }

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
        public String displayName() {
            String name = String.join(" ",
                    lastName == null ? "" : lastName,
                    firstName == null ? "" : firstName).trim();
            return name.isBlank() ? email : name;
        }
    }
}
