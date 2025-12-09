package com.program.rewards.repository;

import com.program.rewards.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findById_ShouldReturnCustomer_WhenCustomerExists() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com",
                LocalDate.now(), "123-456-7890", "123 Main St");
        Customer savedCustomer = customerRepository.save(customer);

        // Act
        Optional<Customer> foundCustomer = customerRepository.findById(savedCustomer.getId());

        // Assert
        assertTrue(foundCustomer.isPresent());
        assertEquals("John Doe", foundCustomer.get().getName());
        assertEquals("john@example.com", foundCustomer.get().getEmail());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenCustomerNotExists() {
        // Act
        Optional<Customer> foundCustomer = customerRepository.findById(999L);

        // Assert
        assertFalse(foundCustomer.isPresent());
    }

    @Test
    void save_ShouldPersistCustomer() {
        // Arrange
        Customer customer = new Customer("Jane Smith", "jane@example.com",
                LocalDate.now(), "987-654-3210", "456 Oak St");

        // Act
        Customer savedCustomer = customerRepository.save(customer);

        // Assert
        assertNotNull(savedCustomer.getId());
        assertEquals("Jane Smith", savedCustomer.getName());
        assertEquals("jane@example.com", savedCustomer.getEmail());

        // Verify can be retrieved
        Optional<Customer> retrieved = customerRepository.findById(savedCustomer.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(savedCustomer.getId(), retrieved.get().getId());
    }
}