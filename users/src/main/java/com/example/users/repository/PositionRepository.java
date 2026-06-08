package com.example.users.repository;

import com.example.users.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, String> {
}
