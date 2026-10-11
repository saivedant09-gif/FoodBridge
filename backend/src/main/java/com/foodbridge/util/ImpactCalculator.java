package com.foodbridge.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility methods for computing social-impact metrics.
 * All metrics are calculated from actual database records.
 * These are platform coordination metrics, not verified redistribution outcomes.
 */
public class ImpactCalculator {
    
    public static double fulfillmentRate(long fulfilled, long total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(fulfilled)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
