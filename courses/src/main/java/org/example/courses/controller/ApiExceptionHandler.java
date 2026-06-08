package org.example.courses.controller;

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
        String message = ex.getMessage() == null ? "Некорректный запрос" : ex.getMessage();
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
        String message = ex.getMessage() == null ? "Конфликт данных" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", toUserMessage(message)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() == null || ex.getReason().isBlank()
                ? "Ошибка запроса"
                : ex.getReason();
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
        String lower = message.toLowerCase();
        if (lower.contains("access denied to course")) return "У вас нет доступа к этому курсу.";
        if (lower.contains("access denied")) return "У вас нет прав для этого действия.";
        if (lower.contains("course not found")) return "Курс не найден.";
        if (lower.contains("request not found")) return "Заявка не найдена.";
        if (lower.contains("certificate not found")) return "Сертификат не найден.";
        if (lower.contains("quarter limit reached")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("up to\\s+(\\d+)\\s+courses", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(message);
            if (matcher.find()) {
                return "Лимит на квартал исчерпан. Можно выбрать не более " + matcher.group(1) + " курсов.";
            }
            return "Лимит на квартал исчерпан. Достигнуто максимальное количество курсов.";
        }
        if (lower.contains("already assigned")) return "Курс уже назначен этому пользователю.";
        if (lower.contains("already pending")) return "Заявка на этот курс уже ожидает модерации.";
        if (lower.contains("missing x-user-id")) return "Требуется авторизация.";
        return message;
    }
}
