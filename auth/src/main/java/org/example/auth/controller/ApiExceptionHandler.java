package org.example.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() == null || ex.getReason().isBlank() ? "Ошибка запроса" : ex.getReason();
        return ResponseEntity.status(status).body(Map.of("error", toUserMessage(reason)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Некорректный запрос" : ex.getMessage();
        HttpStatus status = message.toLowerCase().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", toUserMessage(message)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "Конфликт данных" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", toUserMessage(message)));
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
        if (low.contains("invalid username or password")) return "Неверный логин или пароль.";
        if (low.contains("token is invalid or expired")) return "Сессия истекла. Войдите снова.";
        if (low.contains("missing or invalid authorization header")) return "Требуется авторизация.";
        if (low.contains("registration failed")) return "Не удалось зарегистрироваться. Проверьте введенные данные.";
        if (low.contains("user not found")) return "Пользователь не найден.";
        return message;
    }
}

