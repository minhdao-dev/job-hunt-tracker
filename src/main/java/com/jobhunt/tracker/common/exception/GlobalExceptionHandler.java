package com.jobhunt.tracker.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_URL = "https://jobhunt.io/errors";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(
            AppException ex,
            HttpServletRequest request) {

        if (ex instanceof AppException.InternalServerException e) {
            log.error("[{}] {} -> {}",
                    e.getErrorCode(), request.getRequestURI(), e.getMessage());
        } else {
            log.warn("[{}] {} -> {}",
                    ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
        }

        return ResponseEntity
                .status(ex.getStatus())
                .body(buildProblemDetail(
                        ex.getStatus(),
                        ex.getMessage(),
                        ex.getErrorCode(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        log.warn("[VALIDATION_ERROR] {} → {}", request.getRequestURI(), errors);

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "VALIDATION_ERROR",
                request.getRequestURI()
        );
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("[UNEXPECTED_ERROR] {} → {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildProblemDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        "INTERNAL_ERROR",
                        request.getRequestURI()
                ));
    }

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String detail,
            String errorCode,
            String path) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(BASE_ERROR_URL + "/"
                + errorCode.toLowerCase().replace("_", "-")));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(path));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }
}