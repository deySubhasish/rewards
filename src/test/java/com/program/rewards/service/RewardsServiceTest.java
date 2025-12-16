package com.program.rewards.service;

import com.program.rewards.dto.RewardsResponse;
import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import com.program.rewards.exception.CustomerNotFoundException;
import com.program.rewards.repository.CustomerRepository;
import com.program.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private RewardsService rewardsService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Setup test customer
        testCustomer = new Customer("John Doe", "john.doe@example.com",
                LocalDateTime.now().toLocalDate(), "123-456-7890", "123 Main St");
        testCustomer.setId(1L);

        // Setup test transactions

    }

    @Test
    void getCustomerById_ShouldReturnCustomer_WhenCustomerExists() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = rewardsService.getCustomerById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
    }

    @Test
    void getCustomerById_ShouldThrowException_WhenCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> {
            rewardsService.getCustomerById(999L);
        });
    }

    private Transaction createTestTransaction(Long id, double amount, int daysAgo) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAmount(amount);
        transaction.setStatus("COMPLETED");
        transaction.setTransactionDate(LocalDateTime.now().minusDays(daysAgo));
        return transaction;
    }

    @Test
    void getRewardEligibleTransactions_ShouldReturnAllCompletedTransactions_WhenNoDateRangeProvided() {
        // Arrange
        Transaction t1 = createTestTransaction(1L, 120.0, 10);
        Transaction t2 = createTestTransaction(2L, 80.0, 5);
        Transaction t3 = createTestTransaction(3L, 310.0, 0);

        when(transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThan(
                eq(1L), eq("COMPLETED"), eq(50.0)))
                .thenReturn(Arrays.asList(t1, t2, t3));

        // Act
        List<Transaction> result = rewardsService.getRewardEligibleTransactions(1L, null, null);

        // Assert
        assertEquals(3, result.size());
        verify(transactionRepository).findByCustomerIdAndStatusAndAmountGreaterThan(1L, "COMPLETED", 50.0);
    }

    @Test
    void getRewardEligibleTransactions_ShouldFilterByDateRange_WhenBothDatesProvided() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(7);
        LocalDateTime endDate = now.minusDays(2);
        
        Transaction t1 = createTestTransaction(1L, 120.0, 5); // Within range

        when(transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween(
                eq(1L), eq("COMPLETED"), eq(50.0), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(t1));

        // Act
        List<Transaction> result = rewardsService.getRewardEligibleTransactions(1L, startDate, endDate);

        // Assert
        assertEquals(1, result.size());
        verify(transactionRepository).findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween(
                1L, "COMPLETED", 50.0, startDate, endDate);
    }

    @Test
    void getRewardEligibleTransactions_ShouldFilterByStartDate_WhenOnlyStartDateProvided() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        
        Transaction t2 = createTestTransaction(2L, 80.0, 5);  // After start date
        Transaction t3 = createTestTransaction(3L, 310.0, 0); // After start date

        when(transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter(
                eq(1L), eq("COMPLETED"), eq(50.0), eq(startDate)))
                .thenReturn(Arrays.asList(t2, t3));

        // Act
        List<Transaction> result = rewardsService.getRewardEligibleTransactions(1L, startDate, null);

        // Assert
        assertEquals(2, result.size());
        verify(transactionRepository).findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter(
                1L, "COMPLETED", 50.0, startDate);
    }

    @Test
    void getRewardEligibleTransactions_ShouldFilterByEndDate_WhenOnlyEndDateProvided() {
        // Arrange
        LocalDateTime endDate = LocalDateTime.now().minusDays(2);
        
        Transaction t1 = createTestTransaction(1L, 120.0, 10); // Before end date
        Transaction t2 = createTestTransaction(2L, 80.0, 5);   // Before end date

        when(transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore(
                eq(1L), eq("COMPLETED"), eq(50.0), eq(endDate)))
                .thenReturn(Arrays.asList(t1, t2));

        // Act
        List<Transaction> result = rewardsService.getRewardEligibleTransactions(1L, null, endDate);

        // Assert
        assertEquals(2, result.size());
        verify(transactionRepository).findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore(
                1L, "COMPLETED", 50.0, endDate);
    }

    @Test
    void calculatePoints_ShouldCalculateCorrectly() throws Exception {
        // Get the private method using reflection
        Method method = RewardsService.class.getDeclaredMethod("calculatePoints", Double.class);
        method.setAccessible(true);

        // Test case 2: Amount between 50 and 100
        assertEquals(10, (int) method.invoke(rewardsService, 60.0));  // 60 - 50 = 10 points

        // Test case 3: Amount greater than 100
        assertEquals(90, (int) method.invoke(rewardsService, 120.0));  // 2*(120-100) + 50 = 90 points
    }

    @Test
    void calculateMonthlyRewards_ShouldReturnCorrectRewards() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        Transaction t1 = new Transaction();
        t1.setId(1L);
        t1.setAmount(120.0);
        t1.setStatus("COMPLETED");
        t1.setTransactionDate(LocalDateTime.now().minusDays(10));

        Transaction t2 = new Transaction();
        t2.setId(2L);
        t2.setAmount(80.0);
        t2.setStatus("COMPLETED");
        t2.setTransactionDate(LocalDateTime.now().minusDays(5));

        List<Transaction> testTransactions = Arrays.asList(t1, t2);
        when(transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThan(anyLong(), anyString(), anyDouble()))
                .thenReturn(testTransactions);

        // Act
        RewardsResponse response = rewardsService.calculateMonthlyRewards(1L, null, null, false);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getCustomer().getId());
        assertFalse(response.getMonthlyPoints().isEmpty());
        assertTrue(response.getTotalPoints() > 0);
    }

    @Test
    void clearRewardsCache_ShouldBeCallable() {
        // This test just verifies the method can be called without errors
        assertDoesNotThrow(() -> rewardsService.clearRewardsCache());
    }
}