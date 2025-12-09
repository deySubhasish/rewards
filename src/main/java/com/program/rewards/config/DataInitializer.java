package com.program.rewards.config;

import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import com.program.rewards.repository.CustomerRepository;
import com.program.rewards.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!test") // Exclude from tests unless specifically needed
public class DataInitializer {

    @Bean
    public CommandLineRunner loadData(TransactionRepository transactionRepository,CustomerRepository customerRepository) {
        return args -> {
            // Initialize customers first
            initializeCustomers(customerRepository);
            // Only insert if database is empty
            if (transactionRepository.count() == 0) {
                List<Transaction> transactions = loadTransactionsFromCsv();
                transactionRepository.saveAll(transactions);
                System.out.println("Loaded " + transactions.size() + " transactions from CSV file.");
            }
        };
    }
    
    private List<Transaction> loadTransactionsFromCsv() {
        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        try (InputStream inputStream = new ClassPathResource("data/transactions.csv").getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            
            // Read and process the first line (header)
            String line = reader.readLine();
            if (line == null) {
                throw new RuntimeException("CSV file is empty");
            }
            
            // Remove BOM if present
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            
            // Check header
            if (!line.trim().toLowerCase().startsWith("amount")) {
                throw new RuntimeException("Invalid CSV format: missing or invalid header. Expected: amount,status,transaction_date,customer_id. Found: " + line);
            }
            
            // Process data lines
            int lineNumber = 1; // We've already read the first line
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                // Skip empty lines and comments
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (values.length < 4) {
                        System.err.println("Warning: Line " + lineNumber + " has insufficient columns: " + line);
                        continue;
                    }
                    
                    Transaction transaction = new Transaction();
                    transaction.setAmount(Double.parseDouble(values[0].trim()));
                    transaction.setStatus(values[1].trim());
                    transaction.setTransactionDate(LocalDateTime.parse(values[2].trim(), formatter));
                    transaction.setCustomerId(Long.parseLong(values[3].trim()));
                    transactions.add(transaction);
                    
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + line);
                    System.err.println("Error details: " + e.getMessage());
                    // Continue processing next lines even if one line fails
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    private void initializeCustomers(CustomerRepository customerRepository) {
        String[] customerNames = {
                "John Doe", "Jane Smith", "Robert Johnson", "Emily Davis", "Michael Brown"
        };
        String[] customerEmails = {
                "john.doe@example.com", "jane.smith@example.com", "robert.j@example.com",
                "emily.d@example.com", "michael.b@example.com"
        };
        String[] phoneNumbers = {
                "+1-555-0101", "+1-555-0102", "+1-555-0103", "+1-555-0104", "+1-555-0105"
        };
        String[] addresses = {
                "123 Main St, Anytown, USA",
                "456 Oak Ave, Somewhere, USA",
                "789 Pine Rd, Nowhere, USA",
                "321 Elm St, Anywhere, USA",
                "654 Maple Dr, Everywhere, USA"
        };

        List<Customer> customers = new ArrayList<>();
        if (customerRepository.count() == 0) {
            for (int i = 0; i < customerNames.length; i++) {
                Customer customer = new Customer(
                        customerNames[i],
                        customerEmails[i],
                        LocalDate.now().minusMonths(i),
                        phoneNumbers[i],
                        addresses[i]
                );
                customers.add(customerRepository.save(customer));
            }
            System.out.println("Initialized " + customers.size() + " sample customers.");
        } else {
            customers = customerRepository.findAll();
        }
    }
}
