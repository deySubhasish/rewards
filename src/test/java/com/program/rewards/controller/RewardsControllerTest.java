package com.program.rewards.controller;

import com.program.rewards.dto.RewardsResponse;
import com.program.rewards.entity.Customer;
import com.program.rewards.service.RewardsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RewardsControllerTest {

    @Mock
    private RewardsService rewardsService;

    @InjectMocks
    private RewardsController rewardsController;

    @Autowired
    private MockMvc mockMvc;
    private RewardsResponse testResponse;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rewardsController)
                .setControllerAdvice(new com.program.rewards.exception.GlobalExceptionHandler())
                .build();

        Customer testCustomer = new Customer("John Doe", "john.doe@example.com", LocalDate.now(), "123-456-7890", "123 Main St");
        testCustomer.setId(1L);
        
        Map<String, Integer> monthlyPoints = new HashMap<>();
        monthlyPoints.put("2023-01", 120);
        monthlyPoints.put("2023-02", 150);
        
        testResponse = new RewardsResponse(testCustomer, 270, monthlyPoints,null);
    }

    @Test
    void getMonthlyRewards_WithValidCustomerIdAndDays_ShouldReturnOk() throws Exception {
        when(rewardsService.calculateMonthlyRewards(anyLong(), any(), any(),anyBoolean()))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/customers/1/rewards?days=30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("John Doe"))
                .andExpect(jsonPath("$.totalPoints").value(270))
                .andExpect(jsonPath("$.monthlyPoints.2023-01").value(120))
                .andExpect(jsonPath("$.monthlyPoints.2023-02").value(150));
    }

    @Test
    void getMonthlyRewards_WithValidCustomerIdAndMonths_ShouldReturnOk() throws Exception {
        when(rewardsService.calculateMonthlyRewards(anyLong(), any(), any(),anyBoolean()))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/customers/1/rewards?months=6")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void getMonthlyRewards_WithValidDateRange_ShouldReturnOk() throws Exception {
        when(rewardsService.calculateMonthlyRewards(anyLong(), any(), any(),anyBoolean()))
                .thenReturn(testResponse);

        String startDate = LocalDateTime.now().minusMonths(1).format(formatter);
        String endDate = LocalDateTime.now().format(formatter);

        mockMvc.perform(get("/api/customers/1/rewards")
                .param("startDate", startDate)
                .param("endDate", endDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void getMonthlyRewards_WithInvalidDateRange_ShouldReturnBadRequest() throws Exception {
        String invalidDate = "invalid-date";
        
        mockMvc.perform(get("/api/customers/1/rewards")
                .param("startDate", invalidDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getMonthlyRewards_WithNoParameters_ShouldUseDefaultDateRange() throws Exception {
        when(rewardsService.calculateMonthlyRewards(anyLong(), any(), any(),anyBoolean()))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/customers/1/rewards")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void getMonthlyRewards_WithBothDaysAndMonths_ShouldPrioritizeDays() throws Exception {
        when(rewardsService.calculateMonthlyRewards(anyLong(), any(), any(),anyBoolean()))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/customers/1/rewards?days=30&months=6")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void getMonthlyRewards_WithInvalidBoolean_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/customers/1/rewards?showTransactions=notABoolean")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid value 'notABoolean' for parameter 'showTransactions'. Expected type: boolean")));
    }

    @Test
    void getMonthlyRewards_WithInvalidDateFormat_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/customers/1/rewards")
                        .param("startDate", "2023/01/01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid value '2023/01/01' for parameter 'startDate'. Expected type: LocalDateTime")));
    }


}
