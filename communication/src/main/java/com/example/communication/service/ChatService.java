package com.example.communication.service;

import com.example.communication.courses.CoursesServiceClient;
import com.example.communication.dto.ChatParticipantDto;
import com.example.communication.dto.ChatRoomDto;
import com.example.communication.dto.CreateChatRoomRequest;
import com.example.communication.dto.CreateMessageRequest;
import com.example.communication.dto.MessageDto;
import com.example.communication.kafka.EventPublisher;
import com.example.communication.kafka.NewMessageEvent;
import com.example.communication.model.ChatMessage;
import com.example.communication.model.ChatParticipant;
import com.example.communication.model.ChatParticipantRole;
import com.example.communication.model.ChatRoom;
import com.example.communication.model.ChatRoomType;
import com.example.communication.model.MessageType;
import com.example.communication.repository.ChatMessageRepository;
import com.example.communication.repository.ChatParticipantRepository;
import com.example.communication.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {
    private static final String ATTACHMENTS_SEPARATOR = "\u001F";
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CoursesServiceClient coursesServiceClient;
    private final EventPublisher eventPublisher;
    private final OnlineStatusService onlineStatusService;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatParticipantRepository chatParticipantRepository,
            ChatMessageRepository chatMessageRepository,
            CoursesServiceClient coursesServiceClient,
            EventPublisher eventPublisher,
            OnlineStatusService onlineStatusService
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.coursesServiceClient = coursesServiceClient;
        this.eventPublisher = eventPublisher;
        this.onlineStatusService = onlineStatusService;
    }

    public List<ChatRoomDto> listUserRooms(String userId) {
        List<String> roomIds = chatParticipantRepository.findByUserId(userId).stream()
                .map(ChatParticipant::getRoomId)
                .toList();
        if (roomIds.isEmpty()) return List.of();
        return chatRoomRepository.findByIdInOrderByUpdatedAtDesc(roomIds).stream()
                .map(this::toRoomDto)
                .toList();
    }

    public List<MessageDto> getRoomMessages(String userId, String roomId, Integer limit, OffsetDateTime before) {
        ensureParticipant(roomId, userId);
        int safeLimit = Math.max(1, Math.min(limit == null ? 50 : limit, 200));
        List<ChatMessage> desc = before == null
                ? chatMessageRepository.findByRoomIdOrderByTimestampDesc(roomId, PageRequest.of(0, safeLimit))
                : chatMessageRepository.findByRoomIdAndTimestampLessThanOrderByTimestampDesc(roomId, before, PageRequest.of(0, safeLimit));
        List<ChatMessage> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc.stream().map(this::toMessageDto).toList();
    }

    @Transactional
    public MessageDto createMessage(String userId, String roomId, CreateMessageRequest request) {
        ensureParticipant(roomId, userId);
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(request.content().trim());
        message.setMessageType(request.messageType() == null ? MessageType.TEXT : request.messageType());
        message.setAttachmentsJson(writeAttachments(request.attachments()));
        ChatMessage saved = chatMessageRepository.save(message);

        room.setUpdatedAt(OffsetDateTime.now());
        chatRoomRepository.save(room);

        MessageDto dto = toMessageDto(saved);
        try {
            eventPublisher.publishNewMessage(new NewMessageEvent(
                    dto.id(),
                    dto.roomId(),
                    dto.userId(),
                    dto.content(),
                    dto.messageType(),
                    dto.timestamp()
            ));
        } catch (KafkaException ex) {
            log.warn("Failed to publish chat.new-message for messageId={} roomId={}", dto.id(), dto.roomId(), ex);
        } catch (RuntimeException ex) {
            log.warn("Unexpected error during chat.new-message publishing for messageId={} roomId={}", dto.id(), dto.roomId(), ex);
        }
        return dto;
    }

    @Transactional
    public ChatRoomDto createRoom(String creatorUserId, CreateChatRoomRequest request) {
        if (request.type() == ChatRoomType.COURSE && request.courseId() != null && !request.courseId().isBlank()) {
            ChatRoom existing = chatRoomRepository.findByCourseIdAndType(request.courseId(), ChatRoomType.COURSE).orElse(null);
            if (existing != null) {
                addParticipantIfAbsent(existing.getId(), creatorUserId, ChatParticipantRole.CURATOR);
                if (request.participants() != null) {
                    request.participants().forEach(p -> addParticipantIfAbsent(existing.getId(), p, ChatParticipantRole.PARTICIPANT));
                }
                return toRoomDto(existing);
            }
        }

        ChatRoom room = new ChatRoom();
        room.setCourseId(request.courseId());
        room.setName(request.name().trim());
        room.setType(request.type());
        ChatRoom saved = chatRoomRepository.save(room);

        addParticipantIfAbsent(saved.getId(), creatorUserId, ChatParticipantRole.CURATOR);
        if (request.participants() != null) {
            request.participants().forEach(p -> addParticipantIfAbsent(saved.getId(), p, ChatParticipantRole.PARTICIPANT));
        }
        return toRoomDto(saved);
    }

    public List<ChatParticipantDto> getParticipants(String userId, String roomId) {
        ensureParticipant(roomId, userId);
        return chatParticipantRepository.findByRoomId(roomId).stream()
                .map(p -> new ChatParticipantDto(
                        p.getUserId(),
                        p.getRoomId(),
                        p.getJoinedAt(),
                        p.getRole(),
                        onlineStatusService.isOnline(p.getUserId())
                ))
                .toList();
    }

    @Transactional
    public void ensureCourseRoomParticipant(String courseId, String participantUserId, String curatorUserId) {
        if (courseId == null || courseId.isBlank() || participantUserId == null || participantUserId.isBlank()) {
            return;
        }
        ChatRoom room = chatRoomRepository.findByCourseIdAndType(courseId, ChatRoomType.COURSE)
                .orElseGet(() -> {
                    ChatRoom r = new ChatRoom();
                    r.setCourseId(courseId);
                    r.setName("Чат курса " + courseId);
                    r.setType(ChatRoomType.COURSE);
                    return chatRoomRepository.save(r);
                });
        addParticipantIfAbsent(room.getId(), participantUserId, ChatParticipantRole.PARTICIPANT);
        if (curatorUserId != null && !curatorUserId.isBlank()) {
            addParticipantIfAbsent(room.getId(), curatorUserId, ChatParticipantRole.CURATOR);
        }
    }

    @Transactional
    public ChatRoomDto joinCourseRoom(String courseId, String userId, Set<String> roles) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        Set<String> normalizedRoles = normalizeRoles(roles);
        boolean instructor = normalizedRoles.contains("EXPERT") || normalizedRoles.contains("TECHNOLOG");
        boolean privileged = normalizedRoles.contains("ADMIN") || normalizedRoles.contains("HR") || instructor;
        if (!privileged && !coursesServiceClient.hasAssignment(userId, courseId)) {
            throw new ForbiddenOperationException("You are not allowed to join this course chat");
        }
        ChatRoom room = chatRoomRepository.findByCourseIdAndType(courseId, ChatRoomType.COURSE)
                .orElseGet(() -> {
                    ChatRoom r = new ChatRoom();
                    r.setCourseId(courseId);
                    r.setName("Чат курса " + courseId);
                    r.setType(ChatRoomType.COURSE);
                    return chatRoomRepository.save(r);
                });
        addParticipantIfAbsent(room.getId(), userId, instructor ? ChatParticipantRole.INSTRUCTOR : ChatParticipantRole.PARTICIPANT);
        return toRoomDto(room);
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();
        Set<String> normalized = new HashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) continue;
            normalized.add(role.trim().toUpperCase());
        }
        return normalized;
    }

    private void ensureParticipant(String roomId, String userId) {
        if (!chatParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new IllegalArgumentException("Access denied to room: " + roomId);
        }
    }

    private void addParticipantIfAbsent(String roomId, String userId, ChatParticipantRole role) {
        if (userId == null || userId.isBlank()) return;
        if (chatParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) return;
        ChatParticipant participant = new ChatParticipant();
        participant.setRoomId(roomId);
        participant.setUserId(userId);
        participant.setRole(role);
        chatParticipantRepository.save(participant);
    }

    private ChatRoomDto toRoomDto(ChatRoom room) {
        return new ChatRoomDto(
                room.getId(),
                room.getCourseId(),
                room.getName(),
                room.getType(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    private MessageDto toMessageDto(ChatMessage message) {
        return new MessageDto(
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                message.getContent(),
                message.getTimestamp(),
                message.getMessageType(),
                readAttachments(message.getAttachmentsJson())
        );
    }

    private String writeAttachments(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        return attachments.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .reduce((a, b) -> a + ATTACHMENTS_SEPARATOR + b)
                .orElse(null);
    }

    private List<String> readAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isBlank()) return List.of();
        return List.of(attachmentsJson.split(ATTACHMENTS_SEPARATOR));
    }
}
