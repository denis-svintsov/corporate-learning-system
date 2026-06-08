package com.example.notifications.service;

import com.example.notifications.dto.CreateNotificationRequest;
import com.example.notifications.dto.NotificationDeliveryStatDto;
import com.example.notifications.dto.NotificationDto;
import com.example.notifications.model.Notification;
import com.example.notifications.model.NotificationChannel;
import com.example.notifications.model.NotificationDeliveryStat;
import com.example.notifications.model.NotificationStatus;
import com.example.notifications.model.NotificationType;
import com.example.notifications.repository.NotificationDeliveryStatRepository;
import com.example.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryStatRepository deliveryStatRepository;

    public List<NotificationDto> findMy(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public long unreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationDto create(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.userId())
                .type(request.type() == null ? NotificationType.SYSTEM : request.type())
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .title(request.title())
                .message(request.message())
                .sourceService(request.sourceService())
                .sourceId(request.sourceId())
                .createdAt(OffsetDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        incrementSent(saved.getType(), saved.getChannel());
        return toDto(saved);
    }

    @Transactional
    public NotificationDto markRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to notification: " + notificationId);
        }
        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(OffsetDateTime.now());
            incrementRead(notification.getType(), notification.getChannel());
        }
        return toDto(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(String userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(notification -> notification.getStatus() == NotificationStatus.UNREAD)
                .forEach(notification -> {
                    notification.setStatus(NotificationStatus.READ);
                    notification.setReadAt(OffsetDateTime.now());
                    incrementRead(notification.getType(), notification.getChannel());
                });
    }

    public List<NotificationDeliveryStatDto> deliveryStats() {
        return deliveryStatRepository.findAll().stream()
                .map(stat -> new NotificationDeliveryStatDto(
                        stat.getType(),
                        stat.getChannel(),
                        stat.getSentCount(),
                        stat.getReadCount(),
                        percent(stat.getReadCount(), stat.getSentCount())
                ))
                .toList();
    }

    private void incrementSent(NotificationType type, NotificationChannel channel) {
        NotificationDeliveryStat stat = deliveryStatRepository.findByTypeAndChannel(type, channel)
                .orElseGet(() -> NotificationDeliveryStat.builder()
                        .type(type)
                        .channel(channel)
                        .sentCount(0)
                        .readCount(0)
                        .build());
        stat.setSentCount(stat.getSentCount() + 1);
        deliveryStatRepository.save(stat);
    }

    private void incrementRead(NotificationType type, NotificationChannel channel) {
        NotificationDeliveryStat stat = deliveryStatRepository.findByTypeAndChannel(type, channel)
                .orElseGet(() -> NotificationDeliveryStat.builder()
                        .type(type)
                        .channel(channel)
                        .sentCount(0)
                        .readCount(0)
                        .build());
        stat.setReadCount(stat.getReadCount() + 1);
        deliveryStatRepository.save(stat);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSourceService(),
                notification.getSourceId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private double percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((part * 1000.0) / total) / 10.0;
    }
}
