package com.portfolio.hitlist.interfaces;

import com.portfolio.hitlist.quota.AutomationState;

/**
 * Requires interface contract for persisting the singleton automation lock and permutation cursor state.
 */
public interface AutomationStateRepository extends CrudRepositoryContract<AutomationState, Long> {
}
