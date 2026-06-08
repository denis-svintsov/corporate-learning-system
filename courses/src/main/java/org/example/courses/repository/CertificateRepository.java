package org.example.courses.repository;

import org.example.courses.dto.CertificateDto;
import org.example.courses.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, String> {

    List<Certificate> findByUserId(String userId);

    @Query("""
            select new org.example.courses.dto.CertificateDto(
                c.id,
                c.course.id,
                c.issueDate,
                c.certificateUrl,
                c.hash
            )
            from Certificate c
            where c.userId = :userId
            order by c.issueDate desc
            """)
    List<CertificateDto> findDtosByUserId(@Param("userId") String userId);
}
