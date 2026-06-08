package org.example.courses.repository;

import org.example.courses.model.AssignmentPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentPolicyRepository extends JpaRepository<AssignmentPolicy, Integer> {
}
