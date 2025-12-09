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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    public static final double MIN_AMOUNT_FOR_REWARDS = 50.0;

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
            return transactions;
        } catch (Exception e) {
            log.error("Error fetching reward-eligible transactions for customer: {}", customerId, e);
            throw e;
        }
    }

    @Cacheable(
            value = REWARDS_CACHE,
            key = "{#customerId, #startDate, #endDate}",
            unless = "#result == null || #result.getTotalPoints() < 0"
    )
    @Transactional(readOnly = true)
    public RewardsResponse calculateMonthlyRewards(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating rewards for customer: {} between {} and {}", customerId, startDate, endDate);
        try {
            Customer customer = getCustomerById(customerId);
            List<Transaction> transactions = getRewardEligibleTransactions(customerId, startDate, endDate);
            RewardsResponse response = calculateMonthlyBreakdown(customer, transactions);
            log.info("Successfully calculated rewards for customer: {}. Total points: {}",
                    customerId, response.getTotalPoints());
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


    private RewardsResponse calculateMonthlyBreakdown(Customer customer, List<Transaction> transactions) {
        log.debug("Calculating monthly breakdown for customer: {} with {} transactions",
                customer.getId(), transactions.size());

        Map<String, Integer> monthlyPoints = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate()).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.summingInt(t -> calculatePoints(t.getAmount()))
                ));

        int totalPoints = monthlyPoints.values().stream().mapToInt(Integer::intValue).sum();

        log.debug("Monthly points breakdown for customer {}: {}", customer.getId(), monthlyPoints);
        log.debug("Total points calculated: {}", totalPoints);

        return new RewardsResponse(customer, totalPoints, monthlyPoints);
    }

    private int calculatePoints(Double amount) {
        log.trace("Calculating points for amount: {}", amount);
        int points = 0;

        // 2 points for every dollar over $100
        if (amount > 100) {
            double over100 = amount - 100;
            points += (int) (over100 * 2);
            log.trace("Added {} points for amount over $100: {}", points, over100);
            amount = 100.0; // Remaining amount for next tier
        }

        // 1 point for every dollar between $50 and $100
        if (amount > MIN_AMOUNT_FOR_REWARDS) {
            int between50and100 = (int) (amount - MIN_AMOUNT_FOR_REWARDS);
            points += between50and100;
            log.trace("Added {} points for amount between $50 and $100: {}", between50and100, between50and100);
        }

        log.trace("Total points calculated: {}", points);
        return points;
    }

}
