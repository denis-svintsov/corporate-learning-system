package com.example.users.service;

import com.example.users.dto.DepartmentDto;
import com.example.users.dto.UserProfileDto;
import com.example.users.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserService userService;

    public DepartmentService(DepartmentRepository departmentRepository, UserService userService) {
        this.departmentRepository = departmentRepository;
        this.userService = userService;
    }

    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDto(
                        d.getDepartmentId(),
                        d.getName(),
                        d.getDescription(),
                        d.getManagerId(),
                        d.getParentDepartmentId()
                ))
                .toList();
    }

    public List<UserProfileDto> usersByDepartment(String departmentId) {
        return userService.findByDepartment(departmentId);
    }
}
