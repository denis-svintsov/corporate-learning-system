package com.example.communication.controller;

import com.example.communication.dto.ChatParticipantDto;
import com.example.communication.dto.ChatRoomDto;
import com.example.communication.dto.CreateChatRoomRequest;
import com.example.communication.dto.CreateMessageRequest;
import com.example.communication.dto.MessageDto;
import com.example.communication.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/rooms")
    public List<ChatRoomDto> rooms(@RequestHeader(name = "X-User-Id", required = false) String userId) {
        return chatService.listUserRooms(requiredUserId(userId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<MessageDto> messages(
            @PathVariable String roomId,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before
    ) {
        OffsetDateTime beforeTs = null;
        if (before != null && !before.isBlank()) {
            beforeTs = OffsetDateTime.parse(before);
        }
        return chatService.getRoomMessages(requiredUserId(userId), roomId, limit, beforeTs);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public MessageDto postMessage(
            @PathVariable String roomId,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        MessageDto saved = chatService.createMessage(requiredUserId(userId), roomId, request);
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, saved);
        return saved;
    }

    @PostMapping("/rooms")
    public ChatRoomDto createRoom(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        return chatService.createRoom(requiredUserId(userId), request);
    }

    @PostMapping("/rooms/course/{courseId}/join")
    public ChatRoomDto joinCourseRoom(
            @PathVariable String courseId,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return chatService.joinCourseRoom(courseId, requiredUserId(userId), parseRoles(rolesHeader));
    }

    @GetMapping("/rooms/{roomId}/participants")
    public List<ChatParticipantDto> participants(
            @PathVariable String roomId,
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        return chatService.getParticipants(requiredUserId(userId), roomId);
    }

    private String requiredUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        return userId;
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
