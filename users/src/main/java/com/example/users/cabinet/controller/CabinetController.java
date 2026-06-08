package com.example.users.cabinet.controller;

import com.example.users.cabinet.dto.LearningHistoryDto;
import com.example.users.cabinet.dto.ProgressSummaryDto;
import com.example.users.cabinet.dto.UserCabinetDto;
import com.example.users.cabinet.service.CabinetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/cabinet")
public class CabinetController {

    private final CabinetService cabinetService;

    public CabinetController(CabinetService cabinetService) {
        this.cabinetService = cabinetService;
    }

    @GetMapping
    public UserCabinetDto myCabinet(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "50") int historyLimit
    ) {
        return cabinetService.getMyCabinet(userId, historyLimit);
    }

    @GetMapping("/progress")
    public ProgressSummaryDto myProgress(@RequestHeader("X-User-Id") String userId) {
        return cabinetService.getMyProgress(userId);
    }

    @GetMapping("/history")
    public List<LearningHistoryDto> myHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return cabinetService.getMyHistory(userId, limit);
    }
}
