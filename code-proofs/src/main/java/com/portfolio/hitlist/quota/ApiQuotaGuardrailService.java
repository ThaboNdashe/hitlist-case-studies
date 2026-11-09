package com.portfolio.hitlist.quota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.portfolio.hitlist.interfaces.AutomationStateRepository;
import com.portfolio.hitlist.interfaces.DailyUsageRepository;

/**
 * Production proof of the FinOps API Cost Guardrail & Circuit Breaker Engine.
 * Enforces pre-flight micro-dollar budget reservations, Enterprise SKU hard caps,
 * billing lockdown transitions, and timezone-aware daily auto-resume windows.
 */
public class ApiQuotaGuardrailService {

    public static final String FREE_TIER_EXHAUSTED_MESSAGE = "Free Tier Exhausted for Today";
    public static final String BILLING_LOCKDOWN_PREFIX = "Google billing rejection detected";
    private static final long MICROS_PER_USD = 1_000_000L;
    private static final long REQUESTS_PER_THOUSAND = 1_000L;
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Europe/Istanbul");

    private final QuotaProperties properties;
    private final DailyUsageRepository usageRepository;
    private final AutomationStateRepository automationStateRepository;
    private final Clock clock;

    public ApiQuotaGuardrailService(
            QuotaProperties properties,
            DailyUsageRepository usageRepository,
            AutomationStateRepository automationStateRepository
    ) {
        this(properties, usageRepository, automationStateRepository, Clock.systemDefaultZone());
    }

    public ApiQuotaGuardrailService(
            QuotaProperties properties,
            DailyUsageRepository usageRepository,
            AutomationStateRepository automationStateRepository,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.usageRepository = Objects.requireNonNull(usageRepository, "usageRepository must not be null");
        this.automationStateRepository = Objects.requireNonNull(automationStateRepository, "automationStateRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Pre-flight reservation executed transactionally before any outbound Google Places HTTP request.
     */
    public void reserveCall(GooglePlacesSku sku) {
        Objects.requireNonNull(sku, "sku must not be null");

        AutomationState state = getOrCreateState();
        ZonedDateTime now = nowInQuotaZone();
        tryAutoResumeIfDue(state, now);
        if (!state.isPipelineEnabled()) {
            throw new AutomationDisabledException(blockReason(state));
        }

        LocalDate usageDate = now.toLocalDate();
        DailyUsageLedger usage = getOrCreateUsage(usageDate);

        // Guardrail 1: Daily Request Hard Cap for Enterprise SKU
        long currentRequests = usage.getRequestCount(sku);
        if (sku == GooglePlacesSku.TEXT_SEARCH_ENTERPRISE && currentRequests >= properties.getMaxRequestsPerDay()) {
            String msg = "Daily Request Limit Exhausted for Enterprise Search (" + properties.getMaxRequestsPerDay() + "/day)";
            disableUntilNextWindow(state, now, msg);
            automationStateRepository.save(state);
            throw new QuotaExceededException(msg);
        }

        // Guardrail 2: Micro-Dollar Budget Envelope Check
        long dailyBudgetMicros = computeDailyBudgetMicros();
        long requestCostMicros = computeRequestCostMicros(sku);
        long projectedMicros = usage.getEstimatedCostMicros() + requestCostMicros;
        if (projectedMicros > dailyBudgetMicros) {
            disableUntilNextWindow(state, now, FREE_TIER_EXHAUSTED_MESSAGE);
            automationStateRepository.save(state);
            throw new QuotaExceededException(FREE_TIER_EXHAUSTED_MESSAGE);
        }

        usage.increment(sku, requestCostMicros);
        usageRepository.save(usage);
    }

    /**
     * Evaluates whether scheduled or manual automation runs are permitted to start.
     */
    public AutomationGate evaluateAutomationGate() {
        AutomationState state = getOrCreateState();
        ZonedDateTime now = nowInQuotaZone();
        if (tryAutoResumeIfDue(state, now)) {
            automationStateRepository.save(state);
        }
        if (!state.isPipelineEnabled()) {
            return new AutomationGate(false, blockReason(state));
        }

        DailyUsageLedger usage = usageRepository.findById(now.toLocalDate()).orElse(null);
        long consumedMicros = usage == null ? 0L : usage.getEstimatedCostMicros();
        if (consumedMicros >= computeDailyBudgetMicros()) {
            disableUntilNextWindow(state, now, FREE_TIER_EXHAUSTED_MESSAGE);
            automationStateRepository.save(state);
            return new AutomationGate(false, FREE_TIER_EXHAUSTED_MESSAGE);
        }
        return new AutomationGate(true, null);
    }

    /**
     * Immediately trips the billing lockdown circuit breaker on upstream HTTP 402/403/429 or OVER_QUERY_LIMIT signals.
     */
    public void markBillingLockdown(String reason) {
        AutomationState state = getOrCreateState();
        ZonedDateTime now = nowInQuotaZone();
        String message = BILLING_LOCKDOWN_PREFIX;
        if (reason != null && !reason.isBlank()) {
            message = message + ": " + reason.trim();
        }
        disableUntilNextWindow(state, now, message);
        automationStateRepository.save(state);
    }

    public long computeDailyBudgetMicros() {
        double monthlyCreditUsd = Math.max(0.0, properties.getMonthlyCreditUsd());
        double safetyRatio = Math.min(1.0, Math.max(0.0, properties.getBudgetSafetyRatio()));
        int billingDays = Math.max(1, properties.getBillingCycleDays());

        BigDecimal credit = BigDecimal.valueOf(monthlyCreditUsd);
        BigDecimal ratio = BigDecimal.valueOf(safetyRatio);
        BigDecimal days = BigDecimal.valueOf(billingDays);

        BigDecimal dailyUsd = credit.multiply(ratio).divide(days, 8, RoundingMode.DOWN);
        return dailyUsd.multiply(BigDecimal.valueOf(MICROS_PER_USD))
            .setScale(0, RoundingMode.DOWN)
            .longValueExact();
    }

    public long computeRequestCostMicros(GooglePlacesSku sku) {
        double pricePerThousand = switch (sku) {
            case TEXT_SEARCH, TEXT_SEARCH_ENTERPRISE -> Math.max(0.0, properties.getTextSearchPricePerThousandUsd());
            case PLACE_DETAILS_CONTACT -> Math.max(0.0, properties.getDetailsContactPricePerThousandUsd());
        };
        BigDecimal price = BigDecimal.valueOf(pricePerThousand);
        return price.multiply(BigDecimal.valueOf(MICROS_PER_USD))
            .divide(BigDecimal.valueOf(REQUESTS_PER_THOUSAND), 0, RoundingMode.CEILING)
            .longValueExact();
    }

    private boolean tryAutoResumeIfDue(AutomationState state, ZonedDateTime now) {
        if (state.isPipelineEnabled()) {
            return false;
        }
        LocalDateTime autoResumeAt = state.getAutoResumeAt();
        if (autoResumeAt == null || !now.toLocalDateTime().isBefore(autoResumeAt)) {
            state.setPipelineEnabled(true);
            state.setDisabledReason(null);
            state.setDisabledAt(null);
            state.setAutoResumeAt(null);
            return true;
        }
        return false;
    }

    private void disableUntilNextWindow(AutomationState state, ZonedDateTime now, String reason) {
        state.setPipelineEnabled(false);
        state.setDisabledReason(reason);
        state.setDisabledAt(now.toLocalDateTime());
        state.setAutoResumeAt(nextAutoResumeTime(now));
    }

    private LocalDateTime nextAutoResumeTime(ZonedDateTime now) {
        int hour = Math.min(23, Math.max(0, properties.getAutoReenableHour()));
        int minute = Math.min(59, Math.max(0, properties.getAutoReenableMinute()));
        ZonedDateTime candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toLocalDateTime();
    }

    private ZonedDateTime nowInQuotaZone() {
        ZoneId zone = resolveZone();
        return ZonedDateTime.now(clock.withZone(zone));
    }

    private ZoneId resolveZone() {
        String configured = properties.getQuotaResetZone();
        if (configured == null || configured.isBlank()) {
            return FALLBACK_ZONE;
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (DateTimeException ex) {
            return FALLBACK_ZONE;
        }
    }

    private String blockReason(AutomationState state) {
        String reason = state.getDisabledReason();
        return (reason != null && !reason.isBlank()) ? reason : FREE_TIER_EXHAUSTED_MESSAGE;
    }

    private AutomationState getOrCreateState() {
        return automationStateRepository.findById(AutomationState.SINGLETON_ID)
            .orElseGet(() -> {
                AutomationState created = new AutomationState();
                created.setId(AutomationState.SINGLETON_ID);
                created.setPipelineEnabled(true);
                return automationStateRepository.save(created);
            });
    }

    private DailyUsageLedger getOrCreateUsage(LocalDate date) {
        return usageRepository.findById(date)
            .orElseGet(() -> usageRepository.save(new DailyUsageLedger(date)));
    }

    public record AutomationGate(boolean allowed, String reason) {
        public String reasonOrDefault() {
            return (reason != null && !reason.isBlank()) ? reason : FREE_TIER_EXHAUSTED_MESSAGE;
        }
    }
}
