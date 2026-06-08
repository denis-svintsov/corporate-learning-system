package org.example.courses.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.example.courses.dto.CreateCourseRequest;
import org.example.courses.dto.CourseDto;
import org.example.courses.dto.UpdateCourseRequest;
import org.example.courses.model.*;
import org.example.courses.repository.CategoryRepository;
import org.example.courses.repository.CourseRepository;
import org.example.courses.repository.TagRepository;
import org.example.courses.util.CsvUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseMapper courseMapper;

    public Page<Course> catalog(String q,
                               String categoryId,
                               DifficultyLevel difficulty,
                               CourseStatus status,
                               String tagId,
                               String positionId,
                               Pageable pageable) {
        Specification<Course> spec = Specification.where(null);

        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("title")), like),
                            cb.like(cb.lower(root.get("description")), like)
                    )
            );
        }

        if (categoryId != null && !categoryId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        if (difficulty != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (tagId != null && !tagId.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Course, Tag> tags = root.join("tags", JoinType.LEFT);
                return cb.equal(tags.get("id"), tagId);
            });
        }

        if (positionId != null && !positionId.isBlank()) {
            String normalized = positionId.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> {
                var csvExpr = cb.lower(cb.coalesce(root.get("specializationsCsv"), ""));
                var wrapped = cb.concat(cb.concat(",", csvExpr), ",");
                return cb.like(wrapped, "%," + normalized + ",%");
            });
        }

        return courseRepository.findAll(spec, pageable);
    }

    @Cacheable(
            cacheNames = "courseCatalog",
            key = "T(String).format('%s|%s|%s|%s|%s|%s|%s|%s', #q, #categoryId, #difficulty, #status, #tagId, #positionId, #pageable.pageNumber, #pageable.pageSize)"
    )
    public Page<CourseDto> catalogDto(String q,
                                      String categoryId,
                                      DifficultyLevel difficulty,
                                      CourseStatus status,
                                      String tagId,
                                      String positionId,
                                      Pageable pageable) {
        return catalog(q, categoryId, difficulty, status, tagId, positionId, pageable)
                .map(courseMapper::toDto);
    }

    public Course getById(String id) {
        return courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    @Cacheable(cacheNames = "courseById", key = "#id")
    public CourseDto getDtoById(String id) {
        return courseMapper.toDto(getById(id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"courseCatalog", "courseById"}, allEntries = true)
    public Course create(CreateCourseRequest req) {
        Category category = null;
        if (req.categoryId() != null && !req.categoryId().isBlank()) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
        }

        Set<Tag> tags = new HashSet<>();
        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            tags.addAll(tagRepository.findAllById(req.tagIds()));
        }

        Course course = Course.builder()
                .title(req.title())
                .description(req.description())
                .category(category)
                .difficulty(req.difficulty())
                .durationMinutes(req.durationMinutes())
                .status(CourseStatus.DRAFT)
                .tags(tags)
                .allowedRolesCsv(CsvUtil.join(req.allowedRoles()))
                .allowedDepartmentIdsCsv(CsvUtil.join(req.allowedDepartmentIds()))
                .specializationsCsv(CsvUtil.join(req.specializations()))
                .instructions(req.instructions())
                .aggregatorUrl(req.aggregatorUrl())
                .coverUrl(req.coverUrl())
                .companyCost(req.companyCost())
                .partnerName(req.partnerName())
                .partnerLocation(req.partnerLocation())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();

        return courseRepository.save(course);
    }

    @Transactional
    @CacheEvict(cacheNames = {"courseCatalog", "courseById"}, allEntries = true)
    public Course update(String id, UpdateCourseRequest req) {
        Course course = getById(id);

        Category category = null;
        if (req.categoryId() != null && !req.categoryId().isBlank()) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
        }

        Set<Tag> tags = new HashSet<>();
        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            tags.addAll(tagRepository.findAllById(req.tagIds()));
        }

        course.setTitle(req.title());
        course.setDescription(req.description());
        course.setCategory(category);
        course.setDifficulty(req.difficulty());
        course.setDurationMinutes(req.durationMinutes());
        course.setStatus(req.status());
        course.setTags(tags);
        course.setAllowedRolesCsv(CsvUtil.join(req.allowedRoles()));
        course.setAllowedDepartmentIdsCsv(CsvUtil.join(req.allowedDepartmentIds()));
        course.setSpecializationsCsv(CsvUtil.join(req.specializations()));
        course.setInstructions(req.instructions());
        course.setAggregatorUrl(req.aggregatorUrl());
        course.setCoverUrl(req.coverUrl());
        course.setCompanyCost(req.companyCost());
        course.setPartnerName(req.partnerName());
        course.setPartnerLocation(req.partnerLocation());
        course.setStartDate(req.startDate());
        course.setEndDate(req.endDate());

        return courseRepository.save(course);
    }

}
