package com.example.notifications.dto;

import com.example.notifications.model.NotificationChannel;
import com.example.notifications.model.NotificationType;

public record NotificationDeliveryStatDto(
        NotificationType type,
        NotificationChannel channel,
        long sentCount,
        long readCount,
        double deliveryRate
) {
}
