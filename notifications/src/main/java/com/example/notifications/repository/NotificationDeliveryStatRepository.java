package com.example.notifications.repository;

import com.example.notifications.model.NotificationChannel;
import com.example.notifications.model.NotificationDeliveryStat;
import com.example.notifications.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationDeliveryStatRepository extends JpaRepository<NotificationDeliveryStat, String> {

    Optional<NotificationDeliveryStat> findByTypeAndChannel(NotificationType type, NotificationChannel channel);
}
