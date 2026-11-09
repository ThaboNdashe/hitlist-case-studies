package com.portfolio.hitlist.interfaces;

import java.time.LocalDate;
import com.portfolio.hitlist.quota.DailyUsageLedger;

/**
 * Requires interface contract for persisting and querying daily API usage micro-dollar ledgers.
 */
public interface DailyUsageRepository extends CrudRepositoryContract<DailyUsageLedger, LocalDate> {
}
