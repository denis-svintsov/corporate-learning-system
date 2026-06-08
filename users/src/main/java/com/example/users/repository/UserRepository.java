package com.example.users.repository;

import com.example.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByDepartmentId(String departmentId);

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    List<User> findByStatusIgnoreCase(String status);
}
