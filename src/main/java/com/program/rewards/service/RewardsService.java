package com.program.rewards.service;

import com.program.rewards.dto.RewardsResponse;
import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import com.program.rewards.repository.CustomerRepository;
import com.program.rewards.repository.TransactionRepository;
import com.program.rewards.util.RewardsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardsService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public static final String REWARDS_CACHE = "rewards";
    public static final String COMPLETED_STATUS = "COMPLETED";

    public Customer getCustomerById(Long id) {
        log.debug("Looking up customer with id: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Customer not found with id: {}", id);
                    return new NoSuchElementException("Customer not found with id: " + id);
                });
    }

    public List<Transaction> getRewardEligibleTransactions(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching reward-eligible transactions for customer: {}, startDate: {}, endDate: {}",
                customerId, startDate, endDate);
        try {
            List<Transaction> transactions = transactionRepository.findEligibleTransactions(
                    customerId,
                    COMPLETED_STATUS,
                    RewardsUtil.MIN_AMOUNT_FOR_REWARDS,
                    startDate,
                    endDate);

            log.debug("Found {} eligible transactions for customer: {}", transactions.size(), customerId);
            return transactions;
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
        Map<String, Integer> monthlyPoints = RewardsUtil.getMonthlyPoints(transactions, monthYearFormatter);

        int totalPoints = monthlyPoints.values().stream().mapToInt(Integer::intValue).sum();

        log.debug("Monthly points breakdown for customer {}: {}", customer.getId(), monthlyPoints);
        log.debug("Total points calculated: {}", totalPoints);
        return new RewardsResponse(customer, totalPoints, monthlyPoints, listTransactions ? transactions.stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .toList() : null);
    }




}
