# ADR 002: Search Campaign Permutations & Free-Tier Deep Exhaustion

## Context
The system needed a way to endlessly generate unique leads by rotating through multiple active "Search Campaigns" (combinations of Country × City × Category).
Simultaneously, the Google Maps Platform transitioned to a per-SKU limit, strictly capping the Enterprise SKU (triggered by requesting phone numbers and websites) to **1,000 free events per month**.
We needed an algorithm that extracts the maximum possible unique leads from every combination without ever exceeding 1,000 requests per month.

## Decision
We implemented a **Deep Exhaustion + Mathematical Permutation** strategy inside the hourly Cron job.

1. **Strictly 1 Request Per Hour:** The `@Scheduled` cron job is hardcoded to execute exactly one Google Places API call per hour. Over a 30-day month, this translates to 720 calls, perfectly protecting the user from exceeding the 1,000 free event limit.
2. **Deep Exhaustion:** If a query yields more than 20 results, Google provides a `nextPageToken`. The backend stores this token in `GoogleAutomationState`. The next hourly run will use that token to fetch page 2 instead of starting a new query.
3. **Mathematical Permutation:** Once all pages for a combination are exhausted (the token is null), the system mathematically calculates the next target: `globalSearchIteration % total_permutations`. This allows for seamless rotation across multiple campaigns, cities, and categories without needing a complex state-tracking table.
4. **Multi-Campaign UI:** The frontend was updated to allow toggling multiple active campaigns concurrently.

## Consequences
* **Pros:** 
  * 100% guarantee of staying inside the free tier.
  * Stateless permutation makes it resilient to adding/removing cities mid-cycle.
  * Maximum extraction of leads per geographic area.
* **Cons:** 
  * Lead generation is strictly bounded to max 20 leads per hour (or 480 per day). For higher volume, the user will need to explicitly override the free tier protections or manually use the "Run Now" button.

