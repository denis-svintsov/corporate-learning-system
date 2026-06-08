package org.example.courses.controller;

import lombok.RequiredArgsConstructor;
import org.example.courses.dto.CertificateDto;
import org.example.courses.model.Certificate;
import org.example.courses.service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/certificates")
public class CertificatesController {

    private final CertificateService certificateService;

    @GetMapping("/my")
    public List<CertificateDto> my(@RequestHeader("X-User-Id") String userId) {
        return certificateService.myCertificateDtos(userId);
    }

    /**
     * По ТЗ: GET /certificates/{id} — скачать сертификат.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id,
                                           @RequestHeader("X-User-Id") String userId) {
        Certificate cert = certificateService.get(id);
        if (!cert.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to certificate: " + id);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(certificateService.downloadPdf(cert));
    }
}
