package com.portfolio.hitlist.fakes;

import java.util.function.Function;

import com.portfolio.hitlist.interfaces.AutomationStateRepository;
import com.portfolio.hitlist.quota.AutomationState;

/**
 * Zero-bytecode in-memory fake for AutomationStateRepository.
 */
public class FakeAutomationStateRepository extends InMemoryRepository<AutomationState, Long>
        implements AutomationStateRepository {

    public FakeAutomationStateRepository() {
        super(AutomationState::getId, AutomationState::setId, Function.identity());
    }
}
