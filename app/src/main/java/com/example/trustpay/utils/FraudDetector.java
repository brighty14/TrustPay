package com.example.trustpay.utils;

import java.util.List;

public class FraudDetector {

    public static boolean isTransactionSuspicious(List<Double> amounts, double currentAmount) {

        if (amounts == null || amounts.size() < 3) {
            return false; // not enough data
        }

        double sum = 0;
        for (double amt : amounts) {
            sum += amt;
        }


        double mean = sum / amounts.size();

        double variance = 0;
        for (double amt : amounts) {
            variance += Math.pow(amt - mean, 2);
        }

        variance /= amounts.size();
        double stdDev = Math.sqrt(variance);

        // 🔥 3-sigma rule
        double upperLimit = mean + (3 * stdDev);

        return currentAmount > upperLimit;
    }
}