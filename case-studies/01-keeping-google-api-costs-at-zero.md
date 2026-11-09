# 01: Keeping Google Places API Costs at $0 (While Rotating Across Dozens of Cities)

**Runnable Code in This Repo:** [`ApiQuotaGuardrailService.java`](../code-proofs/src/main/java/com/portfolio/hitlist/quota/ApiQuotaGuardrailService.java) · [`CampaignPermutationEngine.java`](../code-proofs/src/main/java/com/portfolio/hitlist/permutation/CampaignPermutationEngine.java)

## What I Was Trying to Build
Hitlist automatically searches Google Places for local businesses in different cities and categories (like *"Plumber in Istanbul"* or *"Dentist in London"*) to find businesses that don't have a website yet.

When Google updated its Maps billing, requesting phone numbers and website URLs fell under the **Enterprise SKU**, which strictly caps free usage at **1,000 calls per month**. I needed the background job to rotate across dozens of city/category combinations 24/7 without ever crossing 1,000 calls/month.

## What Went Wrong First (The 15-Minute Infinite Loop)
My first attempt used simple per-run counters in memory (`max-google-details-calls-per-run`). When a run hit that limit, the pipeline returned `paused = true`.

The problem? Our `CronJobService` treated `paused = true` as a temporary rate limit and scheduled a retry 15 minutes later. When it woke up 15 minutes later:
1. Its in-memory counter reset to `0`.
2. It searched the exact same first page of Google Places results, hit the limit on the same candidates, returned `paused = true`, and scheduled *another* retry.
3. **Result:** It burned Google search calls every 15 minutes in an infinite loop while saving **zero new leads**.

## How I Fixed It

```mermaid
flowchart LR
    CRON["Hourly Cron Job\n(1 Call/Hr = 720/Mo)"] --> GATE{"Quota Ledger OK?\n(Postgres Micros)"}
    GATE -->|"No / 429"| LOCK["Lock Pipeline Until\n02:00 Europe/Istanbul"]
    GATE -->|"Yes"| SEARCH["Search Target:\niteration % total"]
    SEARCH --> TOKEN{"Has nextPageToken?"}
    TOKEN -->|"Yes"| HOLD["Save Token\n(Keep Same Iteration)"]
    TOKEN -->|"No"| NEXT["Clear Token\n(Iteration + 1)"]
```

1. **Pre-Flight Micro-Dollar Ledger in Postgres (`GoogleApiQuotaService`):**
   Instead of counting calls in memory, I created a `google_api_daily_usage` table that tracks spend in micro-dollars (`$1 = 1,000,000 micros`) before any HTTP call leaves the server. If a call would exceed the daily budget—or if Google returns `OVER_QUERY_LIMIT`—the service flips `pipeline_enabled = false` in Postgres and automatically schedules a resume for `02:00 Europe/Istanbul` the next day.
2. **1 Call Per Hour + Deep Pagination (`nextPageToken`):**
   I set the cron job to make **strictly 1 call per hour** (~720 calls/month, safely below the 1,000 cap). If a query has more than 20 results, Google returns a `nextPageToken`. We save that token in Postgres so the next hour's run pulls Page 2 of the *same* search instead of moving on too early.
3. **Stateless Modulo Rotation (`globalSearchIteration % totalPermutations`):**
   Once all pages for a city/category are exhausted (`nextPageToken == null`), we increment `globalSearchIteration` and pick the next target using `iteration % totalPermutations`. That way, users can add or remove cities in the UI at any time without breaking a cursor table.
4. **Soft Caps Instead of Hard Pauses:**
   For manual batch runs, hitting the Place Details limit no longer pauses the job. Instead, it degrades gracefully—saving the basic business lead without the extra `$0.025` details call and finishing the run cleanly (`paused = false`).

## Trade-Offs I Made
* **Capped Speed for $0 Predictability:** Limiting background runs to 1 call/hour means max 20 leads/hour (480/day), which is plenty for an agency team and guarantees a $0 Google bill.
