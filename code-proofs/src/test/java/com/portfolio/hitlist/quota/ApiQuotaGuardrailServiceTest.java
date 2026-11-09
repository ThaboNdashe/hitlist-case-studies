package com.portfolio.hitlist.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.portfolio.hitlist.fakes.FakeAutomationStateRepository;
import com.portfolio.hitlist.fakes.FakeDailyUsageRepository;

class ApiQuotaGuardrailServiceTest {

    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-09T10:00:00Z");

    private FakeDailyUsageRepository usageRepository;
    private FakeAutomationStateRepository automationStateRepository;
    private QuotaProperties properties;
    private ApiQuotaGuardrailService service;

    @BeforeEach
    void setUp() {
        usageRepository = new FakeDailyUsageRepository();
        automationStateRepository = new FakeAutomationStateRepository();

        properties = new QuotaProperties();
        properties.setQuotaResetZone("Europe/Istanbul");
        properties.setAutoReenableHour(2);
        properties.setAutoReenableMinute(0);

        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ISTANBUL_ZONE);
        service = new ApiQuotaGuardrailService(properties, usageRepository, automationStateRepository, fixedClock);
    }

    @Test
    void reserveCallDisablesAutomationWhenDailyMicroDollarBudgetExceeded() {
        // Daily budget: ($1.00 * 0.10) / 30 days = $0.003333 (3,333 micros)
        // Single TEXT_SEARCH call at $32/1000 = $0.032 (32,000 micros) -> Exceeds budget on first call
        properties.setMonthlyCreditUsd(1.0);
        properties.setBudgetSafetyRatio(0.1);
        properties.setBillingCycleDays(30);
        properties.setTextSearchPricePerThousandUsd(32.0);

        assertThatThrownBy(() -> service.reserveCall(GooglePlacesSku.TEXT_SEARCH))
            .isInstanceOf(QuotaExceededException.class)
            .hasMessage(ApiQuotaGuardrailService.FREE_TIER_EXHAUSTED_MESSAGE);

        AutomationState state = automationStateRepository.findById(AutomationState.SINGLETON_ID).orElseThrow();
        assertThat(state.isPipelineEnabled()).isFalse();
        assertThat(state.getDisabledReason()).isEqualTo(ApiQuotaGuardrailService.FREE_TIER_EXHAUSTED_MESSAGE);
        assertThat(state.getAutoResumeAt()).isEqualTo(LocalDateTime.of(2026, 11, 10, 2, 0));
    }

    @Test
    void reserveCallEnforcesDailyHardCapForEnterpriseSku() {
        properties.setMaxRequestsPerDay(2);
        properties.setMonthlyCreditUsd(500.0);

        service.reserveCall(GooglePlacesSku.TEXT_SEARCH_ENTERPRISE);
        service.reserveCall(GooglePlacesSku.TEXT_SEARCH_ENTERPRISE);

        assertThatThrownBy(() -> service.reserveCall(GooglePlacesSku.TEXT_SEARCH_ENTERPRISE))
            .isInstanceOf(QuotaExceededException.class)
            .hasMessageContaining("Daily Request Limit Exhausted for Enterprise Search (2/day)");

        DailyUsageLedger ledger = usageRepository.findById(LocalDate.of(2026, 11, 9)).orElseThrow();
        assertThat(ledger.getEnterpriseSearchCalls()).isEqualTo(2);
    }

    @Test
    void evaluateAutomationGateAutoReenablesAfterResumeWindowPasses() {
        AutomationState state = new AutomationState();
        state.setId(AutomationState.SINGLETON_ID);
        state.setPipelineEnabled(false);
        state.setDisabledReason("Google billing rejection detected: OVER_QUERY_LIMIT");
        state.setDisabledAt(LocalDateTime.of(2026, 11, 8, 18, 0));
        state.setAutoResumeAt(LocalDateTime.of(2026, 11, 9, 2, 0)); // In the past relative to 2026-11-09T13:00 Istanbul
        automationStateRepository.save(state);

        ApiQuotaGuardrailService.AutomationGate gate = service.evaluateAutomationGate();

        assertThat(gate.allowed()).isTrue();
        AutomationState updatedState = automationStateRepository.findById(AutomationState.SINGLETON_ID).orElseThrow();
        assertThat(updatedState.isPipelineEnabled()).isTrue();
        assertThat(updatedState.getDisabledReason()).isNull();
        assertThat(updatedState.getAutoResumeAt()).isNull();
    }

    @Test
    void markBillingLockdownTripsCircuitBreakerUntilNextDailyWindow() {
        service.markBillingLockdown("OVER_QUERY_LIMIT");

        AutomationState state = automationStateRepository.findById(AutomationState.SINGLETON_ID).orElseThrow();
        assertThat(state.isPipelineEnabled()).isFalse();
        assertThat(state.getDisabledReason()).isEqualTo("Google billing rejection detected: OVER_QUERY_LIMIT");
        assertThat(state.getAutoResumeAt()).isEqualTo(LocalDateTime.of(2026, 11, 10, 2, 0));

        assertThatThrownBy(() -> service.reserveCall(GooglePlacesSku.TEXT_SEARCH))
            .isInstanceOf(AutomationDisabledException.class)
            .hasMessageContaining("OVER_QUERY_LIMIT");
    }
}
