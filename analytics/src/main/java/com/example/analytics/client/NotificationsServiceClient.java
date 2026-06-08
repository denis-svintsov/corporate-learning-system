package com.example.analytics.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class NotificationsServiceClient {

    private final RestClient restClient;

    public NotificationsServiceClient(RestClient.Builder builder, @Value("${notifications.service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<NotificationDeliveryStatDto> deliveryStats() {
        List<NotificationDeliveryStatDto> result = restClient.get()
                .uri("/notifications/delivery-stats")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result == null ? List.of() : result;
    }

    public record NotificationDeliveryStatDto(
            String type,
            String channel,
            long sentCount,
            long readCount,
            double deliveryRate
    ) {
    }
}
