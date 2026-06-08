package com.example.communication.controller;

import com.example.communication.dto.CreateMessageRequest;
import com.example.communication.dto.MessageDto;
import com.example.communication.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable String roomId,
            CreateMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userId = headerAccessor.getSessionAttributes() != null
                ? (String) headerAccessor.getSessionAttributes().get("userId")
                : null;
        if (userId == null || userId.isBlank()) {
            return;
        }
        MessageDto saved = chatService.createMessage(userId, roomId, request);
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, saved);
    }
}
