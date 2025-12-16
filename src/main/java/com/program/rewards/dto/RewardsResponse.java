package com.program.rewards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.program.rewards.entity.Customer;
import com.program.rewards.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardsResponse {
    private Customer customer;
    private int totalPoints;
    private Map<String, Integer> monthlyPoints;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Transaction> transactions;

}
