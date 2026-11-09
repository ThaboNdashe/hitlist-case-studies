package com.portfolio.hitlist.quota;

import java.time.LocalDateTime;

/**
 * Encapsulated singleton state tracking circuit-breaker status, auto-resume timestamp,
 * deep-pagination nextPageToken, and stateless modulo permutation counter.
 */
public class AutomationState {

    public static final long SINGLETON_ID = 1L;

    private Long id = SINGLETON_ID;
    private boolean pipelineEnabled = true;
    private String disabledReason;
    private LocalDateTime disabledAt;
    private LocalDateTime autoResumeAt;
    private String nextPageToken;
    private int globalSearchIteration;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isPipelineEnabled() { return pipelineEnabled; }
    public void setPipelineEnabled(boolean pipelineEnabled) { this.pipelineEnabled = pipelineEnabled; }

    public String getDisabledReason() { return disabledReason; }
    public void setDisabledReason(String disabledReason) { this.disabledReason = disabledReason; }

    public LocalDateTime getDisabledAt() { return disabledAt; }
    public void setDisabledAt(LocalDateTime disabledAt) { this.disabledAt = disabledAt; }

    public LocalDateTime getAutoResumeAt() { return autoResumeAt; }
    public void setAutoResumeAt(LocalDateTime autoResumeAt) { this.autoResumeAt = autoResumeAt; }

    public String getNextPageToken() { return nextPageToken; }
    public void setNextPageToken(String nextPageToken) { this.nextPageToken = nextPageToken; }

    public int getGlobalSearchIteration() { return globalSearchIteration; }
    public void setGlobalSearchIteration(int globalSearchIteration) {
        this.globalSearchIteration = Math.max(0, globalSearchIteration);
    }
}
