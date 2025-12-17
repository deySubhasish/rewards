package com.program.rewards.util;

import com.program.rewards.entity.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
public class RewardsUtil {

    public static final Double MIN_AMOUNT_FOR_REWARDS = 50.0;

    public static Map<String, Integer> getMonthlyPoints(List<Transaction> transactions, DateTimeFormatter monthYearFormatter) {
        Map<YearMonth, Integer> sortedMonthlyPoints = transactions.stream().filter(t -> t.getTransactionDate() != null && t.getAmount() > RewardsUtil.MIN_AMOUNT_FOR_REWARDS)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate()),
                        TreeMap::new,  // Use TreeMap to sort by YearMonth
                        Collectors.summingInt(t -> RewardsUtil.calculatePoints(t.getAmount()))
                ))
                .descendingMap();  // Sort in descending order (newest first)

        // Convert to the final map with formatted month-year strings
        Map<String, Integer> monthlyPoints = new LinkedHashMap<>();
        sortedMonthlyPoints.forEach((yearMonth, points) -> {
            String monthYear = yearMonth.format(monthYearFormatter);
            monthlyPoints.put(monthYear, points);
        });
        return monthlyPoints;
    }


    public static int calculatePoints(Double amount) {
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

        int between50and100 = amountBD.subtract(minAmount).intValue();
        points += between50and100;
        log.trace("Added {} points for amount between $50 and $100: {}", between50and100, between50and100);

        log.trace("Total points calculated: {}", points);
        return points;
    }
}
