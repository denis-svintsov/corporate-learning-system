package org.example.courses.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

    public UserProfileDto getUserProfile(String userId) {
        return restTemplate.getForObject(
                usersServiceUrl + "/users/" + userId,
                UserProfileDto.class
        );
    }

    public List<InternalUserContextDto> getActiveUsers() {
        ResponseEntity<List<InternalUserContextDto>> response = restTemplate.exchange(
                usersServiceUrl + "/internal/users/active",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }
}
