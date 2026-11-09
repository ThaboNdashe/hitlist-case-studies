package com.portfolio.hitlist.quota;

import java.time.LocalDate;

/**
 * Encapsulated domain entity representing a single day's API usage and micro-dollar ledger.
 */
public class DailyUsageLedger {

    private LocalDate usageDate;
    private long textSearchCalls;
    private long enterpriseSearchCalls;
    private long placeDetailsCalls;
    private long estimatedCostMicros;

    public DailyUsageLedger() {}

    public DailyUsageLedger(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public void increment(GooglePlacesSku sku, long costMicros) {
        if (costMicros < 0) {
            throw new IllegalArgumentException("costMicros must be non-negative");
        }
        switch (sku) {
            case TEXT_SEARCH -> this.textSearchCalls++;
            case TEXT_SEARCH_ENTERPRISE -> this.enterpriseSearchCalls++;
            case PLACE_DETAILS_CONTACT -> this.placeDetailsCalls++;
        }
        this.estimatedCostMicros += costMicros;
    }

    public long getRequestCount(GooglePlacesSku sku) {
        return switch (sku) {
            case TEXT_SEARCH -> this.textSearchCalls;
            case TEXT_SEARCH_ENTERPRISE -> this.enterpriseSearchCalls;
            case PLACE_DETAILS_CONTACT -> this.placeDetailsCalls;
        };
    }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public long getTextSearchCalls() { return textSearchCalls; }
    public long getEnterpriseSearchCalls() { return enterpriseSearchCalls; }
    public long getPlaceDetailsCalls() { return placeDetailsCalls; }
    public long getEstimatedCostMicros() { return estimatedCostMicros; }
}
