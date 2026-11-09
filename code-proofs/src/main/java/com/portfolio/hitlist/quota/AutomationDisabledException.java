package com.portfolio.hitlist.quota;

/**
 * Domain exception raised when an outbound API call is attempted while the automation circuit breaker is open.
 */
public class AutomationDisabledException extends RuntimeException {
    public AutomationDisabledException(String message) {
        super(message);
    }
}
