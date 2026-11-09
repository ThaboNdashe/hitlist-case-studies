package com.portfolio.hitlist.quota;

/**
 * Domain exception raised when a pre-flight reservation exceeds the daily micro-dollar budget
 * or the Enterprise SKU daily request cap.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
