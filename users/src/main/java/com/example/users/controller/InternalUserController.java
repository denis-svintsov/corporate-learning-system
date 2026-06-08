package com.example.users.controller;

import com.example.users.dto.InternalUserContextDto;
import com.example.users.dto.ProvisionUserRequest;
import com.example.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/context")
    public InternalUserContextDto context(@PathVariable String id) {
        return userService.getInternalContextById(id);
    }

    @GetMapping("/active")
    public List<InternalUserContextDto> activeUsers() {
        return userService.findActiveInternalUsers();
    }

    @PutMapping("/{id}/provision")
    public InternalUserContextDto provision(
            @PathVariable String id,
            @Valid @RequestBody ProvisionUserRequest request
    ) {
        return userService.provisionFromAuth(id, request);
    }
}
