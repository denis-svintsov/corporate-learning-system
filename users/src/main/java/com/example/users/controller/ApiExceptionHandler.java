package com.example.users.controller;

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
        String low = message.toLowerCase();
        HttpStatus status;
        if (low.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (low.contains("access denied")) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Map.of("error", toUserMessage(message)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "Conflict" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", toUserMessage(message)));
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
        if (low.contains("user not found")) return "Пользователь не найден.";
        if (low.contains("position not found")) return "Должность не найдена.";
        if (low.contains("department not found")) return "Отдел не найден.";
        if (low.contains("hr/admin role required")) return "Недостаточно прав. Требуется роль HR или ADMIN.";
        if (low.contains("unsupported role")) return "Указана неподдерживаемая роль.";
        return message;
    }
}
