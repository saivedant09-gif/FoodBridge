package com.foodbridge.util;

import java.time.LocalDateTime;

public class ValidationUtil {
    
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    public static boolean isFutureDateTime(LocalDateTime dt) {
        return dt != null && dt.isAfter(LocalDateTime.now());
    }
    
    public static boolean isPositive(java.math.BigDecimal value) {
        return value != null && value.compareTo(java.math.BigDecimal.ZERO) > 0;
    }
}
