package org.example.courses.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class CertificateStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.certificates}")
    private String certificatesBucket;

    public String uploadCertificate(String objectKey, byte[] pdfBytes) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(certificatesBucket)
                    .object(objectKey)
                    .contentType("application/pdf")
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .build());
            return objectKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload certificate to object storage", ex);
        }
    }

    public byte[] downloadCertificate(String objectKey) {
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(certificatesBucket)
                .object(objectKey)
                .build())) {
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to download certificate from object storage", ex);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(certificatesBucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(certificatesBucket)
                    .build());
        }
    }
}
