package com.program.rewards.dto;

import com.program.rewards.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardsResponse {
    private Customer customer;
    private int totalPoints;
    private Map<String, Integer> monthlyPoints;

}
