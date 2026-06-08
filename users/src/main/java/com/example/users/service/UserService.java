package com.example.users.service;

import com.example.users.dto.UpdateUserRequest;
import com.example.users.dto.UpdateUserRoleRequest;
import com.example.users.dto.InternalUserContextDto;
import com.example.users.dto.ProvisionUserRequest;
import com.example.users.dto.UserProfileDto;
import com.example.users.model.User;
import com.example.users.repository.DepartmentRepository;
import com.example.users.repository.PositionRepository;
import com.example.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "MANAGER", "HR", "TECHNOLOG", "EXPERT", "ADMIN");

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PositionRepository positionRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
    }

    public UserProfileDto getById(String userId) {
        return toProfileDto(findUser(userId));
    }

    public List<UserProfileDto> findByDepartment(String departmentId) {
        return userRepository.findByDepartmentId(departmentId).stream()
                .map(this::toProfileDto)
                .toList();
    }

    public List<UserProfileDto> searchByName(String name) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name).stream()
                .map(this::toProfileDto)
                .toList();
    }

    public InternalUserContextDto getInternalContextById(String userId) {
        User user = findUser(userId);
        return toInternalContextDto(user);
    }

    public List<InternalUserContextDto> findActiveInternalUsers() {
        return userRepository.findByStatusIgnoreCase("active").stream()
                .map(this::toInternalContextDto)
                .toList();
    }

    @Transactional
    public UserProfileDto update(String userId, UpdateUserRequest request) {
        User user = findUser(userId);
        if (request.positionId() != null && !request.positionId().isBlank()) {
            positionRepository.findById(request.positionId())
                    .orElseThrow(() -> new IllegalArgumentException("Position not found: " + request.positionId()));
            user.setPositionId(request.positionId());
        } else {
            user.setPositionId(null);
        }
        if (request.departmentId() != null && !request.departmentId().isBlank()) {
            departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.departmentId()));
            user.setDepartmentId(request.departmentId());
        } else {
            user.setDepartmentId(null);
        }

        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setHireDate(request.hireDate());
        user.setStatus(request.status());

        return toProfileDto(userRepository.save(user));
    }

    @Transactional
    public UserProfileDto updateRole(String userId, UpdateUserRoleRequest request) {
        User user = findUser(userId);
        String normalizedRole = request.role().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Unsupported role: " + request.role());
        }
        user.setRoles(Set.of(normalizedRole));
        return toProfileDto(userRepository.save(user));
    }

    @Transactional
    public InternalUserContextDto provisionFromAuth(String userId, ProvisionUserRequest request) {
        User user = userRepository.findById(userId).orElseGet(User::new);
        user.setId(userId);

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setHireDate(request.hireDate());
        user.setStatus(request.status() == null || request.status().isBlank() ? "active" : request.status());

        if (request.positionId() != null && !request.positionId().isBlank()) {
            positionRepository.findById(request.positionId())
                    .orElseThrow(() -> new IllegalArgumentException("Position not found: " + request.positionId()));
            user.setPositionId(request.positionId());
        } else {
            user.setPositionId(null);
        }

        if (request.departmentId() != null && !request.departmentId().isBlank()) {
            departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.departmentId()));
            user.setDepartmentId(request.departmentId());
        } else {
            user.setDepartmentId(null);
        }

        Set<String> roles = request.roles() == null || request.roles().isEmpty()
                ? Set.of("USER")
                : request.roles().stream()
                .map(r -> r == null ? "" : r.trim().toUpperCase(Locale.ROOT))
                .filter(ALLOWED_ROLES::contains)
                .collect(java.util.stream.Collectors.toSet());
        user.setRoles(roles.isEmpty() ? Set.of("USER") : roles);

        return toInternalContextDto(userRepository.save(user));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private UserProfileDto toProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPositionId(),
                user.getPosition() != null ? user.getPosition().getTitle() : null,
                user.getDepartmentId(),
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                user.getHireDate(),
                user.getStatus(),
                user.getRoles() == null ? Set.of() : user.getRoles()
        );
    }

    private InternalUserContextDto toInternalContextDto(User user) {
        return new InternalUserContextDto(
                user.getId(),
                user.getDepartmentId(),
                user.getPositionId(),
                user.getStatus(),
                user.getRoles() == null ? Set.of() : user.getRoles()
        );
    }
}
