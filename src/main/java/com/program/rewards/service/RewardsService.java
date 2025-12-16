package com.program.rewards.service;

import com.program.rewards.dto.RewardsResponse;
import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import com.program.rewards.exception.CustomerNotFoundException;
import com.program.rewards.repository.CustomerRepository;
import com.program.rewards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardsService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    private static final String REWARDS_CACHE = "rewards";
    public static final String COMPLETED_STATUS = "COMPLETED";
    public static final Double MIN_AMOUNT_FOR_REWARDS = 50.0;

    public Customer getCustomerById(Long id) {
        log.debug("Looking up customer with id: {}", id);
        try {
            return customerRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Customer not found with id: {}", id);
                        return new CustomerNotFoundException(id);
                    });
        } catch (Exception e) {
            log.error("Error occurred while fetching customer with id: {}", id, e);
            throw e;
        }
    }

    public List<Transaction> getRewardEligibleTransactions(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching reward-eligible transactions for customer: {}, startDate: {}, endDate: {}",
                customerId, startDate, endDate);
        List<Transaction> transactions;
        try {
            if (startDate != null && endDate != null) {
                transactions = transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween(
                        customerId, COMPLETED_STATUS, MIN_AMOUNT_FOR_REWARDS, startDate, endDate);
            } else if (startDate != null) {
                transactions = transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter(
                        customerId, COMPLETED_STATUS, MIN_AMOUNT_FOR_REWARDS, startDate);
            } else if (endDate != null) {
                transactions = transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore(
                        customerId, COMPLETED_STATUS, MIN_AMOUNT_FOR_REWARDS, endDate);
            } else {
                transactions = transactionRepository.findByCustomerIdAndStatusAndAmountGreaterThan(
                        customerId, COMPLETED_STATUS, MIN_AMOUNT_FOR_REWARDS);
            }
            log.debug("Found {} eligible transactions for customer: {}", transactions.size(), customerId);
            return transactions.stream()
                    .filter(t -> t.getTransactionDate() != null)  // Filter out null transaction dates
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching reward-eligible transactions for customer: {}", customerId, e);
            throw e;
        }
    }

    @Cacheable(
            value = REWARDS_CACHE,
            key = "{#customerId, #startDate?.toLocalDate(), #endDate?.toLocalDate(), #showTransactions}",
            unless = "#result == null || #result.getTotalPoints() < 0"
    )
    @Transactional(readOnly = true)
    public RewardsResponse calculateMonthlyRewards(Long customerId, LocalDateTime startDate, LocalDateTime endDate, boolean showTransactions) {
        log.info("Calculating rewards for customer: {} between {} and {}. Include transactions: {}",
                customerId, startDate, endDate, showTransactions);
        try {
            Customer customer = getCustomerById(customerId);
            List<Transaction> transactions = getRewardEligibleTransactions(customerId, startDate, endDate);
            RewardsResponse response = calculateMonthlyBreakdown(customer, transactions, showTransactions);


            log.info("Successfully calculated rewards for customer: {}. Total points: {}. Transactions included: {}",
                    customerId, response.getTotalPoints(), showTransactions);
            return response;
        } catch (Exception e) {
            log.error("Error calculating rewards for customer: {}", customerId, e);
            throw e;
        }
    }

    @Value("${rewards.cache.clear-cache-fixed-rate:360000}")
    private long cacheClearIntervalMs;

    @CacheEvict(value = REWARDS_CACHE, allEntries = true)
    @Scheduled(fixedRateString = "${rewards.cache.clear-cache-fixed-rate:360000}")
    public void clearRewardsCache() {
        log.info("Clearing rewards cache. Next clear in {} ms", cacheClearIntervalMs);
    }


    private RewardsResponse calculateMonthlyBreakdown(Customer customer, List<Transaction> transactions, boolean listTransactions) {
        log.debug("Calculating monthly breakdown for customer: {} with {} transactions",
                customer.getId(), transactions.size());

        // Create a formatter for the month-year display
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        
        // Create a TreeMap to sort by YearMonth in descending order
        Map<YearMonth, Integer> sortedMonthlyPoints = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate()),
                        TreeMap::new,  // Use TreeMap to sort by YearMonth
                        Collectors.summingInt(t -> calculatePoints(t.getAmount()))
                ))
                .descendingMap();  // Sort in descending order (newest first)

        // Convert to the final map with formatted month-year strings
        Map<String, Integer> monthlyPoints = new LinkedHashMap<>();
        sortedMonthlyPoints.forEach((yearMonth, points) -> {
            String monthYear = yearMonth.format(monthYearFormatter);
            monthlyPoints.put(monthYear, points);
        });

        int totalPoints = monthlyPoints.values().stream().mapToInt(Integer::intValue).sum();

        log.debug("Monthly points breakdown for customer {}: {}", customer.getId(), monthlyPoints);
        log.debug("Total points calculated: {}", totalPoints);
        return new RewardsResponse(customer, totalPoints, monthlyPoints, listTransactions ? transactions.stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .toList() : null);
    }

    private int calculatePoints(Double amount) {
        log.trace("Calculating points for amount: {}", amount);
        BigDecimal amountBD = BigDecimal.valueOf(amount);
        BigDecimal minAmount = BigDecimal.valueOf(MIN_AMOUNT_FOR_REWARDS);
        int points = 0;

        // 2 points for every dollar over $100
        if (amount > 100) {
            points = amountBD.subtract(BigDecimal.valueOf(100))
                    .multiply(BigDecimal.valueOf(2)).intValue();
            log.trace("Added {} points for amount over $100", points);
            amountBD = BigDecimal.valueOf(100);
        }

        // 1 point for every dollar between $50 and $100
        if (amount > MIN_AMOUNT_FOR_REWARDS) {
            int between50and100 = amountBD.subtract(minAmount).intValue();
            points += between50and100;
            log.trace("Added {} points for amount between $50 and $100: {}", between50and100, between50and100);
        }

        log.trace("Total points calculated: {}", points);
        return points;
    }

}
