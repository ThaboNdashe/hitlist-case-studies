package com.portfolio.hitlist.quota;

/**
 * Configuration properties governing daily micro-dollar spend limits, Enterprise SKU caps,
 * and timezone-aware auto-resume windows.
 */
public class QuotaProperties {

    public static final String DEFAULT_QUOTA_RESET_ZONE = "Europe/Istanbul";
    public static final int DEFAULT_AUTO_REENABLE_HOUR = 2;
    public static final int DEFAULT_AUTO_REENABLE_MINUTE = 0;
    public static final double DEFAULT_MONTHLY_CREDIT_USD = 200.0;
    public static final double DEFAULT_BUDGET_SAFETY_RATIO = 0.85;
    public static final int DEFAULT_BILLING_CYCLE_DAYS = 30;
    public static final double DEFAULT_TEXT_SEARCH_PRICE_PER_THOUSAND_USD = 32.0;
    public static final double DEFAULT_DETAILS_PRICE_PER_THOUSAND_USD = 17.0;
    public static final int DEFAULT_MAX_ENTERPRISE_REQUESTS_PER_DAY = 24;

    private String quotaResetZone = DEFAULT_QUOTA_RESET_ZONE;
    private int autoReenableHour = DEFAULT_AUTO_REENABLE_HOUR;
    private int autoReenableMinute = DEFAULT_AUTO_REENABLE_MINUTE;
    private double monthlyCreditUsd = DEFAULT_MONTHLY_CREDIT_USD;
    private double budgetSafetyRatio = DEFAULT_BUDGET_SAFETY_RATIO;
    private int billingCycleDays = DEFAULT_BILLING_CYCLE_DAYS;
    private double textSearchPricePerThousandUsd = DEFAULT_TEXT_SEARCH_PRICE_PER_THOUSAND_USD;
    private double detailsContactPricePerThousandUsd = DEFAULT_DETAILS_PRICE_PER_THOUSAND_USD;
    private int maxRequestsPerDay = DEFAULT_MAX_ENTERPRISE_REQUESTS_PER_DAY;

    public String getQuotaResetZone() { return quotaResetZone; }
    public void setQuotaResetZone(String quotaResetZone) { this.quotaResetZone = quotaResetZone; }

    public int getAutoReenableHour() { return autoReenableHour; }
    public void setAutoReenableHour(int autoReenableHour) { this.autoReenableHour = autoReenableHour; }

    public int getAutoReenableMinute() { return autoReenableMinute; }
    public void setAutoReenableMinute(int autoReenableMinute) { this.autoReenableMinute = autoReenableMinute; }

    public double getMonthlyCreditUsd() { return monthlyCreditUsd; }
    public void setMonthlyCreditUsd(double monthlyCreditUsd) { this.monthlyCreditUsd = monthlyCreditUsd; }

    public double getBudgetSafetyRatio() { return budgetSafetyRatio; }
    public void setBudgetSafetyRatio(double budgetSafetyRatio) { this.budgetSafetyRatio = budgetSafetyRatio; }

    public int getBillingCycleDays() { return billingCycleDays; }
    public void setBillingCycleDays(int billingCycleDays) { this.billingCycleDays = billingCycleDays; }

    public double getTextSearchPricePerThousandUsd() { return textSearchPricePerThousandUsd; }
    public void setTextSearchPricePerThousandUsd(double price) { this.textSearchPricePerThousandUsd = price; }

    public double getDetailsContactPricePerThousandUsd() { return detailsContactPricePerThousandUsd; }
    public void setDetailsContactPricePerThousandUsd(double price) { this.detailsContactPricePerThousandUsd = price; }

    public int getMaxRequestsPerDay() { return maxRequestsPerDay; }
    public void setMaxRequestsPerDay(int maxRequestsPerDay) { this.maxRequestsPerDay = maxRequestsPerDay; }
}
