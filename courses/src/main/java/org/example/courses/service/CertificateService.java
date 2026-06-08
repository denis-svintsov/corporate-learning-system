package org.example.courses.service;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.CertificateDto;
import org.example.courses.model.Certificate;
import org.example.courses.model.Course;
import org.example.courses.repository.CertificateRepository;
import org.example.courses.repository.CourseRepository;
import org.example.courses.users.UsersServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final CertificateGenerator certificateGenerator;
    private final CertificateStorageService certificateStorageService;
    private final UsersServiceClient usersServiceClient;

    public List<Certificate> myCertificates(String userId) {
        return certificateRepository.findByUserId(userId);
    }

    public List<CertificateDto> myCertificateDtos(String userId) {
        return certificateRepository.findDtosByUserId(userId);
    }

    public Certificate get(String id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + id));
    }

    public byte[] downloadPdf(Certificate certificate) {
        if (certificate.getObjectKey() == null || certificate.getObjectKey().isBlank()) {
            throw new IllegalStateException("Certificate file is not uploaded to object storage");
        }
        return certificateStorageService.downloadCertificate(certificate.getObjectKey());
    }

    /**
     * Создаёт сертификат при завершении курса, если его ещё нет.
     * Упрощённо: допускаем несколько сертификатов на один курс (в бою лучше ввести unique).
     */
    @Transactional
    public Certificate issueIfNeeded(String userId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        var gen = certificateGenerator.generate(resolveRecipientName(userId), course.getTitle());
        Certificate cert = Certificate.builder()
                .userId(userId)
                .course(course)
                .issueDate(gen.issueDate())
                .hash(gen.hash())
                .certificateUrl("pending")
                .build();
        Certificate saved = certificateRepository.save(cert);
        String objectKey = "certificates/%s/%s.pdf".formatted(userId, saved.getId());
        certificateStorageService.uploadCertificate(objectKey, gen.pdfBytes());
        saved.setObjectKey(objectKey);
        saved.setCertificateUrl("/certificates/" + saved.getId());
        return certificateRepository.save(saved);
    }

    private String resolveRecipientName(String userId) {
        try {
            var profile = usersServiceClient.getUserProfile(userId);
            if (profile != null && profile.displayName() != null && !profile.displayName().isBlank()) {
                return profile.displayName();
            }
        } catch (RuntimeException ignored) {
            // Сертификат всё равно должен выдаваться, даже если users-service временно недоступен.
        }
        return userId;
    }
}
