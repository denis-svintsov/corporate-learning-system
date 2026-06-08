package com.example.users.cabinet.service;

import com.example.users.cabinet.dto.LearningHistoryDto;
import com.example.users.cabinet.dto.ProgressSummaryDto;
import com.example.users.cabinet.dto.UserCabinetDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class CabinetService {

    private final RestClient restClient;

    public CabinetService(
            @Value("${courses.service.url}") String coursesServiceUrl
    ) {
        this.restClient = RestClient.builder().baseUrl(coursesServiceUrl).build();
    }

    public ProgressSummaryDto getMyProgress(String userId) {
        ProgressSummaryDto response = restClient.get()
                .uri("/progress/my")
                .header("X-User-Id", userId)
                .retrieve()
                .body(ProgressSummaryDto.class);
        if (response == null) {
            throw new IllegalStateException("Empty progress response from courses service");
        }
        return response;
    }

    public List<LearningHistoryDto> getMyHistory(String userId, int limit) {
        List<LearningHistoryDto> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/progress/history/my")
                        .queryParam("limit", limit)
                        .build())
                .header("X-User-Id", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response == null ? List.of() : response;
    }

    public UserCabinetDto getMyCabinet(String userId, int historyLimit) {
        return new UserCabinetDto(
                getMyProgress(userId),
                getMyHistory(userId, historyLimit)
        );
    }
}
