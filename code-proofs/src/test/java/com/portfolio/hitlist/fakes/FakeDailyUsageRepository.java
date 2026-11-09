package com.portfolio.hitlist.fakes;

import java.time.LocalDate;

import com.portfolio.hitlist.interfaces.DailyUsageRepository;
import com.portfolio.hitlist.quota.DailyUsageLedger;

/**
 * Zero-bytecode in-memory fake for DailyUsageRepository.
 */
public class FakeDailyUsageRepository extends InMemoryRepository<DailyUsageLedger, LocalDate>
        implements DailyUsageRepository {

    public FakeDailyUsageRepository() {
        super(DailyUsageLedger::getUsageDate, DailyUsageLedger::setUsageDate, null);
    }
}
