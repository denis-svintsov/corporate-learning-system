package org.example.auth.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class UsersServiceClient {

    private final RestTemplate restTemplate;
    private final String usersServiceUrl;

    public UsersServiceClient(
            RestTemplate restTemplate,
            @Value("${users.service.url:http://localhost:8080}") String usersServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.usersServiceUrl = usersServiceUrl;
    }

    public InternalUserContextDto getUserContext(String userId) {
        return restTemplate.getForObject(
                usersServiceUrl + "/internal/users/" + userId + "/context",
                InternalUserContextDto.class
        );
    }

    public InternalUserContextDto provisionUser(String userId, ProvisionUserRequest request) {
        return restTemplate.exchange(
                usersServiceUrl + "/internal/users/" + userId + "/provision",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                InternalUserContextDto.class
        ).getBody();
    }

    public List<Map<String, Object>> getDepartments() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usersServiceUrl + "/departments",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    public List<Map<String, Object>> getPositions() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                usersServiceUrl + "/positions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }
}
