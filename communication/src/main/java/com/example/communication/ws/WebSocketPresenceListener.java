package com.example.communication.ws;

import com.example.communication.service.OnlineStatusService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceListener {

    private final OnlineStatusService onlineStatusService;

    public WebSocketPresenceListener(OnlineStatusService onlineStatusService) {
        this.onlineStatusService = onlineStatusService;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getSessionAttributes() != null
                ? (String) accessor.getSessionAttributes().get("userId")
                : null;
        onlineStatusService.markOnline(userId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getSessionAttributes() != null
                ? (String) accessor.getSessionAttributes().get("userId")
                : null;
        onlineStatusService.markOffline(userId);
    }
}
