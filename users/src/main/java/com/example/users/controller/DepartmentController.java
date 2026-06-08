package com.example.users.controller;

import com.example.users.dto.DepartmentDto;
import com.example.users.dto.UserProfileDto;
import com.example.users.service.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentDto> all() {
        return departmentService.findAll();
    }

    @GetMapping("/{id}/users")
    public List<UserProfileDto> usersByDepartment(@PathVariable String id) {
        return departmentService.usersByDepartment(id);
    }
}
