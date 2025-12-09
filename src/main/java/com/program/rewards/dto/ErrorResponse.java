package com.program.rewards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response format that matches the GlobalExceptionHandler's response structure.
 * This is used for consistent error responses across the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response format for API errors")
public class ErrorResponse {
    
    @Schema(description = "Timestamp when the error occurred", example = "2023-12-09T11:23:45.123")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @Schema(description = "HTTP status code", example = "404")
    private int status;
    
    @Schema(description = "Error message or type", example = "Customer not found")
    private String error;
    
    @Schema(description = "Request path that caused the error", example = "/api/customers/999/rewards")
    private String path;
    
    @Schema(description = "Additional error details or validation errors")
    private Map<String, Object> details;
    
    @ArraySchema(schema = @Schema(description = "List of validation errors"))
    private List<String> errors;
}
