package com.program.rewards.repository;

import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private static Customer testCustomer1;
    private static Customer testCustomer2;
    private static CustomerRepository customerRepository;

    @BeforeAll
    static void setUp(@Autowired CustomerRepository customerRepo) {
        customerRepository = customerRepo;
        // First test customer
        testCustomer1 = new Customer("Test User 1", "test1@example.com",
                LocalDateTime.now().toLocalDate(), "555-1234", "123 Test St");
        testCustomer1 = customerRepository.save(testCustomer1);
        
        // Second test customer
        testCustomer2 = new Customer("Test User 2", "test2@example.com",
                LocalDateTime.now().toLocalDate(), "555-5678", "456 Another St");
        testCustomer2 = customerRepository.save(testCustomer2);
    }

    @Test
    void findByCustomerIdAndStatusAndAmountGreaterThan_ShouldReturnFilteredTransactions() {
        // Arrange - Customer 1 transactions
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId());
        createTestTransaction("COMPLETED", 40.0, testCustomer1.getId()); // Should be filtered out (amount <= 50)
        createTestTransaction("FAILED", 60.0, testCustomer1.getId());  // Should be filtered out (wrong status)
        
        // Customer 2 transactions - should not appear in results
        createTestTransaction("COMPLETED", 100.0, testCustomer2.getId());
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId());

        // Act
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndStatusAndAmountGreaterThan(
                        testCustomer1.getId(), "COMPLETED", 50.0
                );

        // Assert
        assertEquals(1, transactions.size());
        assertEquals(60.0, transactions.get(0).getAmount());
        assertEquals("COMPLETED", transactions.get(0).getStatus());
    }

    @Test
    void findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween_ShouldFilterByDateAndAmount() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        // Customer 1 transactions
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(15)); //Out of range
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(5));  // In range
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now);   // Out of range
        createTestTransaction("COMPLETED", 40.0, testCustomer1.getId(), now.minusDays(5));  // Amount too low
        
        // Customer 2 transactions - should not appear in results
        createTestTransaction("COMPLETED", 100.0, testCustomer2.getId(), now.minusDays(5));
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId(), now.minusDays(3));

        // Act
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween(
                        testCustomer1.getId(),
                        "COMPLETED",
                        50.0,
                        now.minusDays(10),
                        now.minusDays(4)
                );

        // Assert
        assertEquals(1, transactions.size());
        assertEquals(60.0, transactions.get(0).getAmount());
        assertTrue(transactions.get(0).getTransactionDate().isBefore(now));
    }

    @Test
    void findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter_ShouldFilterByStartDate() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        // Customer 1 transactions
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(15));  // Before start date
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(5));   // After start date
        
        // Customer 2 transactions - should not appear in results
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId(), now.minusDays(20));  // Before start date
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId(), now.minusDays(10));  // After start date

        // Act
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter(
                        testCustomer1.getId(),
                        "COMPLETED",
                        50.0,
                        now.minusDays(10)
                );

        // Assert
        assertEquals(1, transactions.size());
        assertTrue(transactions.get(0).getTransactionDate().isAfter(now.minusDays(10)));
    }

    @Test
    void findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore_ShouldFilterByEndDate() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        // Customer 1 transactions
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(15));  // Before end date
        createTestTransaction("COMPLETED", 60.0, testCustomer1.getId(), now.minusDays(5));   // After end date
        
        // Customer 2 transactions - should not appear in results
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId(), now.minusDays(12));  // Before end date
        createTestTransaction("COMPLETED", 60.0, testCustomer2.getId(), now.minusDays(8));   // After end date

        // Act
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore(
                        testCustomer1.getId(),
                        "COMPLETED",
                        50.0,
                        now.minusDays(10)
                );

        // Assert
        assertEquals(1, transactions.size());
        assertTrue(transactions.get(0).getTransactionDate().isBefore(now));
    }

    // Helper methods
    private Transaction createTestTransaction(String status, double amount, Long customerId) {
        return createTestTransaction(status, amount, customerId, LocalDateTime.now());
    }

    private Transaction createTestTransaction(String status, double amount, Long customerId, LocalDateTime date) {
        Transaction transaction = new Transaction();
        transaction.setCustomerId(customerId);
        transaction.setAmount(amount);
        transaction.setStatus(status);
        transaction.setTransactionDate(date);
        return transactionRepository.save(transaction);
    }
}