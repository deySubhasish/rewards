package com.program.rewards.controller;

import com.program.rewards.dto.RewardsResponse;
import com.program.rewards.service.RewardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.program.rewards.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@Tag(name = "Rewards", description = "APIs for fetching customer rewards information")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class RewardsController {

    private final RewardsService rewardsService;

    @Operation(
        summary = "Get customer rewards",
        description = "Retrieves total rewards points & monthly rewards breakdown for a specific customer"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved rewards",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RewardsResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters (e.g., invalid date format, invalid customer ID format)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Customer ID",
                        value = """
                        {
                            "timestamp": "2023-12-09T11:23:45.123",
                            "status": 400,
                            "error": "Invalid value 'abc' for parameter 'customerId'. Expected type: Long",
                            "path": "/api/customers/abc/rewards"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Invalid date format",
                        value = """
                        {
                            "timestamp": "2023-12-09T11:23:45.123",
                            "status": 400,
                            "error": "Invalid date format. Please use yyyy-MM-dd'T'HH:mm:ss format",
                            "path": "/api/customers/123/rewards"
                        }
                        """
                    )
                })
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found with the specified ID",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2023-12-09T11:23:45.123",
                        "status": 404,
                        "error": "Customer not found",
                        "path": "/api/customers/999/rewards"
                    }
                    """
                ))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2023-12-09T11:23:45.123",
                        "status": 500,
                        "error": "An unexpected error occurred",
                        "path": "/api/customers/123/rewards"
                    }
                    """
                ))
        )
    })
    @GetMapping(value = "/customers/{customerId}/rewards", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RewardsResponse> getMonthlyRewards(
            @Parameter(description = "ID of the customer", required = true, example = "1")
            @Min(value = 1, message = "Customer ID must be a positive number")
            @PathVariable Long customerId,
            
            @Parameter(description = "Number of days from today for which to fetch transactions. For example, '30' will fetch transactions from the last 30 days. " +
                                   "This parameter is only used when startDate is not provided.", 
                       example = "30")
            @Min(value = 1, message = "Days must be a positive number")
            @Max(value = 1000, message = "Can only rewards details for last 1000 days")
            @RequestParam(required = false) 
            Integer days,
            
            @Parameter(description = "Number of months from today for which to fetch transactions. For example, '6' will fetch transactions from the last 6 months. " +
                                   "This parameter is only used when startDate and days are not provided.", 
                       example = "6")
            @Min(value = 1, message = "Months must be a positive number")
            @Max(value = 36, message = "Can only rewards details for last 36 months")
            @RequestParam(required = false) 
            Integer months,

            @Parameter(description = "Start date for filtering transactions (format: yyyy-MM-dd'T'HH:mm:ss). " +
                    "If not provided, 'days' or 'months' parameters will be used if available.",
                    example = "2023-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            @Parameter(description = "End date for filtering transactions (format: yyyy-MM-dd'T'HH:mm:ss). " +
                                   "If not provided, current date and time will be used.", 
                       example = "2023-12-31T23:59:59")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate) {
            
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now();
        LocalDateTime effectiveStartDate = startDate;
        
        // Only use days parameter if startDate is not provided
        if (effectiveStartDate == null) {
            if (days != null) {
                effectiveStartDate = effectiveEndDate.minusDays(days);
            } else if (months != null) {
                effectiveStartDate = effectiveEndDate.minusMonths(months);
            }
        }

        RewardsResponse response = rewardsService.calculateMonthlyRewards(
            customerId, 
            effectiveStartDate, 
            effectiveEndDate
        );
        return ResponseEntity.ok(response);
    }
}
