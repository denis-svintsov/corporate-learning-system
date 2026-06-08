package com.example.users.controller;

import com.example.users.model.Position;
import com.example.users.repository.PositionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/positions")
public class PositionController {

    private final PositionRepository positionRepository;

    public PositionController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @GetMapping
    public List<Position> all() {
        return positionRepository.findAll();
    }
}
