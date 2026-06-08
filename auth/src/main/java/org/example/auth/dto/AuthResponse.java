package org.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String userId;
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private String positionId;
    private java.util.Set<String> roles;
}
