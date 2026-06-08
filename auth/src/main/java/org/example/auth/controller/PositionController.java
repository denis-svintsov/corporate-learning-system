package org.example.auth.controller;

import org.example.auth.users.UsersServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/positions", "/api/auth/positions"})
@CrossOrigin(origins = "*")
public class PositionController {
    private final UsersServiceClient usersServiceClient;

    public PositionController(UsersServiceClient usersServiceClient) {
        this.usersServiceClient = usersServiceClient;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPositions() {
        return ResponseEntity.ok(usersServiceClient.getPositions());
    }
}
