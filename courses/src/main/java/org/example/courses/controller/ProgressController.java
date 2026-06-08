package org.example.courses.controller;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.LearningHistoryDto;
import org.example.courses.dto.ProgressSummaryDto;
import org.example.courses.service.ProgressService;
import org.example.courses.util.SecurityHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Личный кабинет с прогрессом.
     * userId в идеале брать из токена, здесь — через заголовок.
     */
    @GetMapping("/my")
    public ProgressSummaryDto my(@RequestHeader("X-User-Id") String userId) {
        return progressService.getMyProgress(userId);
    }

    @GetMapping("/history/my")
    public List<LearningHistoryDto> myHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return progressService.getMyHistory(userId, limit);
    }

    /**
     * Прогресс пользователя (для HR).
     * Проверка ролей может быть добавлена через Security (как в auth), пока упрощённо.
     */
    @GetMapping("/users/{userId}")
    public ProgressSummaryDto user(@PathVariable String userId,
                                   @RequestHeader(name = "X-Roles", required = false) String rolesHeader) {
        Set<String> roles = SecurityHeaders.parseRoles(rolesHeader);
        if (!roles.contains("HR") && !roles.contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied (HR/Admin only)");
        }
        return progressService.getUserProgress(userId);
    }

    @PostMapping("/users/bulk")
    public List<ProgressSummaryDto> users(
            @RequestBody List<String> userIds,
            @RequestHeader(name = "X-Roles", required = false) String rolesHeader
    ) {
        Set<String> roles = SecurityHeaders.parseRoles(rolesHeader);
        if (!roles.contains("HR") && !roles.contains("ADMIN") && !roles.contains("MANAGER") && !roles.contains("TECHNOLOG")) {
            throw new IllegalArgumentException("Access denied (analytics roles only)");
        }
        return progressService.getUsersProgress(userIds);
    }
}
