package com.example.communication.controller;

import com.example.communication.service.ForbiddenOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Invalid request" : ex.getMessage();
        HttpStatus status;
        if (message.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message.toLowerCase().contains("access denied")) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Map.of("error", toUserMessage(message)));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenOperationException ex) {
        String message = ex.getMessage() == null ? "Forbidden" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", toUserMessage(message)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() == null || ex.getReason().isBlank() ? "Request failed" : ex.getReason();
        return ResponseEntity.status(status).body(Map.of("error", toUserMessage(reason)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() == null ? "Проверьте корректность заполненных полей" : err.getDefaultMessage())
                .orElse("Проверьте корректность заполненных полей");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервиса. Попробуйте позже."));
    }

    private String toUserMessage(String message) {
        if (message == null || message.isBlank()) return "Ошибка запроса";
        String low = message.toLowerCase();
        if (low.contains("missing x-user-id")) return "Требуется авторизация.";
        if (low.contains("access denied to room")) return "У вас нет доступа к этой комнате.";
        if (low.contains("you are not allowed to join this course chat")) return "У вас нет прав на вход в чат этого курса.";
        if (low.contains("room not found")) return "Чат-комната не найдена.";
        if (low.contains("courseid is required")) return "Не указан курс для входа в чат.";
        return message;
    }
}
