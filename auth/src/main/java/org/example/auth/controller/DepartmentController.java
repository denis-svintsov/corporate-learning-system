package org.example.auth.controller;

import org.example.auth.users.UsersServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/departments", "/api/auth/departments"})
@CrossOrigin(origins = "*")
public class DepartmentController {
    private final UsersServiceClient usersServiceClient;

    public DepartmentController(UsersServiceClient usersServiceClient) {
        this.usersServiceClient = usersServiceClient;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDepartments() {
        return ResponseEntity.ok(usersServiceClient.getDepartments());
    }
}
