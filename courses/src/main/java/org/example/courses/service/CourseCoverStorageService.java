package org.example.courses.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseCoverStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MinioClient minioClient;

    @Value("${minio.bucket.course-covers}")
    private String courseCoversBucket;

    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cover file is required");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Cover must be JPEG, PNG or WEBP image");
        }

        String objectKey = "course-cover-" + UUID.randomUUID() + extension(contentType);
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(courseCoversBucket)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, file.getSize(), -1)
                    .build());
            return objectKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload course cover to object storage", ex);
        }
    }

    public StoredObject downloadCover(String objectKey) {
        try {
            var response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(courseCoversBucket)
                    .object(objectKey)
                    .build());
            String contentType = response.headers().get("Content-Type");
            return new StoredObject(response, contentType == null ? "application/octet-stream" : contentType);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to download course cover from object storage", ex);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(courseCoversBucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(courseCoversBucket)
                    .build());
        }
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public record StoredObject(InputStream inputStream, String contentType) {
    }
}
