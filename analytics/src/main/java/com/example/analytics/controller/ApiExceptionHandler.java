package com.example.analytics.controller;

import com.example.analytics.service.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(AccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        detail.setTitle("Access denied");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail dependency(RestClientException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detail.setTitle("Analytics dependency unavailable");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail generic(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("Analytics error");
        detail.setDetail(ex.getMessage());
        return detail;
    }
}
