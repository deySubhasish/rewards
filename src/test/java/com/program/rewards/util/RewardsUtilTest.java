package com.program.rewards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardsUtilTest {

    @Test
    void calculatePoints_ShouldCalculateCorrectly() {
        // Test case 1: Amount between 50 and 100
        assertEquals(10, RewardsUtil.calculatePoints(60.0), "60 - 50 = 10 points");

        // Test case 2: Amount greater than 100
        assertEquals(90, RewardsUtil.calculatePoints(120.0), "2*(120-100) + 50 = 90 points");

        // Test case 3: Amount between 50 and 100
        assertEquals(25, RewardsUtil.calculatePoints(75.0), "75 - 50 = 25 points");

        // Test case 4: Amount exactly 100
        assertEquals(50, RewardsUtil.calculatePoints(100.0), "50 points for first $50 + 0 points for next $50");

        // Test case 5: Large amount
        assertEquals(250, RewardsUtil.calculatePoints(200.0), "2*(200-100) + 50 = 250 points");
    }
}
