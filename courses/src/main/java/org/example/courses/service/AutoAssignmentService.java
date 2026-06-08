package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.CourseAssignmentRequest;
import org.example.courses.model.CourseStatus;
import org.example.courses.repository.CourseAssignmentRepository;
import org.example.courses.repository.CourseRepository;
import org.example.courses.users.UsersServiceClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Автоматическое назначение 3 курсов в год каждому сотруднику.
 * Упрощённо: раз в сутки добивает до 3 назначений в текущем году.
 */
@Service
@RequiredArgsConstructor
public class AutoAssignmentService {

    private final UsersServiceClient usersServiceClient;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final AssignmentService assignmentService;

    @Scheduled(cron = "0 10 3 * * *") // каждый день в 03:10
    public void ensureThreeCoursesPerYear() {
        var users = usersServiceClient.getActiveUsers();
        var activeCourses = courseRepository.findByStatus(CourseStatus.ACTIVE);
        if (activeCourses.isEmpty()) return;

        OffsetDateTime startOfYear = OffsetDateTime.now(ZoneOffset.UTC).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime startNextYear = startOfYear.plusYears(1);

        for (var user : users) {
            List<String> assignedThisYear = courseAssignmentRepository
                    .findByUserIdAndCreatedAtBetween(user.id(), startOfYear, startNextYear)
                    .stream()
                    .map(a -> a.getCourse().getId())
                    .toList();

            int need = 3 - assignedThisYear.size();
            if (need <= 0) continue;

            Set<String> assignedSet = Set.copyOf(assignedThisYear);
            List<String> candidates = activeCourses.stream()
                    .map(c -> c.getId())
                    .filter(id -> !assignedSet.contains(id))
                    .collect(Collectors.toList());
            Collections.shuffle(candidates);

            for (int i = 0; i < Math.min(need, candidates.size()); i++) {
                String courseId = candidates.get(i);
                assignmentService.assign(new CourseAssignmentRequest(user.id(), courseId, "SYSTEM", null));
            }
        }
    }
}
