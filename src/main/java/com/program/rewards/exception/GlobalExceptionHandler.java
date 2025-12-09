package com.program.rewards.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TIMESTAMP = "timestamp";

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Object> handleCustomerNotFoundException(
            CustomerNotFoundException ex, WebRequest request) {
        log.error("Customer not found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ((ServletWebRequest) request).getRequest().getRequestURI()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());

        List<String> errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);

        ResponseEntity<Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                ((ServletWebRequest) request).getRequest().getRequestURI()
        );

        // Add errors to the response body
        if (response.getBody() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            responseBody.putAll(body);
        }

        return response;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String error = String.format("Invalid value '%s' for parameter '%s'. %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() == null ? "" : ("Expected type: " + ex.getRequiredType().getSimpleName()));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                error,
                ((ServletWebRequest) request).getRequest().getRequestURI()
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "An unexpected error occurred");

        ResponseEntity<Object> response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                request.getDescription(false).replace("uri=", "")
        );

        // Add message to the response body
        if (response.getBody() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            responseBody.putAll(body);
        }

        return response;
    }

    /**
     * Builds a standardized error response with the given parameters
     *
     * @param status  HTTP status code
     * @param message General error message
     * @param path    Request path that caused the error
     * @return ResponseEntity containing the error details
     */
    private ResponseEntity<Object> buildErrorResponse(
            HttpStatus status,
            String message,
            String path) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        body.put("path", path);

        return new ResponseEntity<>(body, status);
    }
}