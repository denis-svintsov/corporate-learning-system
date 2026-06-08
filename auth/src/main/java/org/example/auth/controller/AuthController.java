package org.example.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.AuthValidationResponse;
import org.example.auth.dto.ErrorResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.repository.UserRepository;
import org.example.auth.service.AuthService;
import org.example.auth.service.JwtService;
import org.example.auth.service.SessionService;
import org.example.auth.users.UsersServiceClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final UsersServiceClient usersServiceClient;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            SessionService sessionService,
            UserRepository userRepository,
            UsersServiceClient usersServiceClient) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.usersServiceClient = usersServiceClient;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String mapRegisterErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Registration failed";
        }
        if (message.contains("rollback-only")) {
            return "Registration failed. Please check input data and try again.";
        }
        return message;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse response = authService.register(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(mapRegisterErrorMessage(e), "BAD_REQUEST");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse response = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ErrorResponse error = new ErrorResponse(e.getMessage(), "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
            }
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error during logout"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            boolean tokenValid = jwtService.validateToken(token, username);
            boolean sessionValid = sessionService.isSessionValid(token);

            if (!tokenValid || !sessionValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token is invalid or expired"));
            }

            var user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            var roles = resolveRoles(user.getId(), token);
            return ResponseEntity.ok(new AuthValidationResponse(user.getId(), user.getUsername(), roles));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token validation failed"));
        }
    }

    private List<String> resolveRoles(String userId, String token) {
        Set<String> tokenRoles = jwtService.extractRoles(token);
        try {
            var userContext = usersServiceClient.getUserContext(userId);
            if (userContext != null && userContext.roles() != null && !userContext.roles().isEmpty()) {
                return userContext.roles().stream().map(String::toUpperCase).toList();
            }
        } catch (RestClientException ex) {
            return tokenRoles.stream().toList();
        }
        return tokenRoles.stream().toList();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }

}
