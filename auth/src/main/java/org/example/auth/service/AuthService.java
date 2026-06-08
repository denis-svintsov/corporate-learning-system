package org.example.auth.service;

import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.model.Role;
import org.example.auth.model.User;
import org.example.auth.repository.UserRepository;
import org.example.auth.users.UsersServiceClient;
import org.example.auth.users.ProvisionUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityPolicyService securityPolicyService;
    private final UsersServiceClient usersServiceClient;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SessionService sessionService,
            LoginAttemptService loginAttemptService,
            SecurityPolicyService securityPolicyService,
            UsersServiceClient usersServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.loginAttemptService = loginAttemptService;
        this.securityPolicyService = securityPolicyService;
        this.usersServiceClient = usersServiceClient;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        // Проверка минимальной длины пароля из security_policy
        if (!securityPolicyService.isPasswordValid(request.getPassword())) {
            int minLength = securityPolicyService.getPasswordMinLength();
            throw new RuntimeException("Password must be at least " + minLength + " characters long");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Проверка блокировки IP
        if (securityPolicyService.isIpBlocked(ipAddress)) {
            throw new RuntimeException("Too many failed attempts from this IP. Please try again later.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPositionId(request.getPositionId());
        user.setDepartmentId(request.getDepartmentId());
        user.setHireDate(request.getHireDate());
        user.setStatus("active");
        
        user = userRepository.save(user);

        usersServiceClient.provisionUser(user.getId(), new ProvisionUserRequest(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPositionId(),
                user.getDepartmentId(),
                user.getHireDate(),
                user.getStatus(),
                Set.of(Role.USER.name())
        ));

        var userContext = usersServiceClient.getUserContext(user.getId());
        Set<String> roleNames = userContext == null || userContext.roles() == null
                ? Set.of(Role.USER.name())
                : userContext.roles().stream().map(String::toUpperCase).collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getUsername(), roleNames);

        // Создание сессии
        sessionService.createSession(user.getId(), token, ipAddress, userAgent);

        // Запись успешной попытки регистрации
        // Пользователь еще не закоммичен в основной транзакции, поэтому в REQUIRES_NEW
        // нельзя ссылаться на user_id из users.users (получим FK violation).
        loginAttemptService.recordLoginAttempt(null, user.getEmail(), ipAddress, true);

        return new AuthResponse(
                user.getId(),
                token,
                "Bearer",
                user.getUsername(),
                user.getEmail(),
                userContext != null ? userContext.positionId() : user.getPositionId(),
                roleNames
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Проверка блокировки IP адреса
        if (securityPolicyService.isIpBlocked(ipAddress)) {
            // Записываем попытку даже при блокировке IP
            loginAttemptService.recordLoginAttempt(null, request.getUsername(), ipAddress, false);
            throw new RuntimeException("Too many failed attempts from this IP. Please try again later.");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        // Проверка блокировки аккаунта по email
        if (user != null && securityPolicyService.isAccountLocked(user.getEmail())) {
            // Записываем попытку даже при блокировке аккаунта
            loginAttemptService.recordLoginAttempt(user.getId(), user.getEmail(), ipAddress, false);
            int remainingMinutes = 15; // Время блокировки
            throw new RuntimeException("Account is temporarily locked due to too many failed login attempts. Please try again in " + remainingMinutes + " minutes.");
        }

        boolean success = user != null && passwordEncoder.matches(request.getPassword(), user.getPassword());

        // Запись попытки входа ДО проверки успешности, чтобы гарантировать сохранение
        // Используется REQUIRES_NEW, поэтому запись сохранится даже если будет исключение
        if (user != null) {
            loginAttemptService.recordLoginAttempt(user.getId(), user.getEmail(), ipAddress, success);
        } else {
            loginAttemptService.recordLoginAttempt(null, request.getUsername(), ipAddress, false);
        }

        if (!success) {
            // Получаем количество оставшихся попыток
            int remainingAttempts = user != null 
                ? securityPolicyService.getRemainingAttempts(user.getEmail())
                : securityPolicyService.getMaxLoginAttempts();
            
            String errorMessage = "Invalid username or password";
            if (remainingAttempts > 0 && remainingAttempts < securityPolicyService.getMaxLoginAttempts()) {
                errorMessage += ". " + remainingAttempts + " attempt(s) remaining";
            }
            
            throw new RuntimeException(errorMessage);
        }

        var userContext = usersServiceClient.getUserContext(user.getId());
        Set<String> roleNames = userContext == null || userContext.roles() == null
                ? Set.of(Role.USER.name())
                : userContext.roles().stream().map(String::toUpperCase).collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getUsername(), roleNames);

        // Создание сессии
        sessionService.createSession(user.getId(), token, ipAddress, userAgent);

        return new AuthResponse(
                user.getId(),
                token,
                "Bearer",
                user.getUsername(),
                user.getEmail(),
                userContext != null ? userContext.positionId() : user.getPositionId(),
                roleNames
        );
    }

    @Transactional
    public void logout(String token) {
        sessionService.invalidateSessionByToken(token);
    }

}
