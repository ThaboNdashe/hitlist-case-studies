# Production Operations, Cost Guardrails & Troubleshooting Baseline

> **Auto-Synced & Sanitized from `hitlist@3a2129f` (`2026-09-01 04:09:39 +0300`)**

---

## Part 1: Google Places FinOps Cost Guardrails
*Source: `docs\guides\backend\google-places-cost-guardrails.md`*

# Google Places Cost Guardrails (SME Hitlist)

## Objective
Keep Google Places usage inside a predictable daily spend envelope while preventing runaway automation when quota or billing issues occur.

## Current guardrail behavior

### 1) Strict Place Details fields (allowlist enforced at startup)
- Place Details fields are restricted to this minimal allowlist only:
  - `website`
  - `formatted_phone_number`
  - `rating`
  - `user_ratings_total`
- Any non-allowlisted field causes startup failure in `GooglePlacesService`.
- Runtime request fields are normalized and sorted from the configured set before use.

### 2) DB-backed daily quota ledger (`google_api_daily_usage`)
- Daily usage is persisted in `google_api_daily_usage` (`GoogleApiDailyUsage`) keyed by `usage_date`.
- Tracked counters:
  - `text_search_calls`
  - `place_details_calls`
  - `estimated_cost_micros`
  - `updated_at`
- Quota is reserved **before every outbound Google HTTP attempt** (including retries), so the ledger reflects true attempt volume/cost pressure.

### 3) Automation lock state (`google_automation_state`)
- Automation state is stored in singleton row `id=1` of `google_automation_state` (`GoogleAutomationState`).
- Key fields:
  - `pipeline_enabled`
  - `disabled_reason`
  - `disabled_at`
  - `auto_resume_at`
- If disabled, API calls throw `GooglePlacesAutomationDisabledException` and automation stays blocked until resume conditions are met.

### 4) Scheduler + manual pre-run gate
- `CronJobService` checks `GoogleApiQuotaService.evaluateAutomationGate()` **before** starting:
  - scheduled runs
  - manual trigger runs
- If blocked, it:
  - skips pipeline start
  - writes a paused cron log with the gate reason
  - logs `Free Tier Exhausted for Today` when that is the block reason

### 5) Billing lock behavior
- Billing/quota failure signals trigger immediate lockdown through `markBillingLockdown(...)`, including:
  - Google status `OVER_QUERY_LIMIT`
  - `REQUEST_DENIED` with billing-related message
  - billing/quota-like HTTP failures (for example `402`, `403`, `429`, or billing-like response body)
- Lockdown sets `pipeline_enabled=false`, records reason/timestamps, and schedules next auto-resume window.

### 6) Daily auto re-enable window (02:00 in configured quota zone)
- Auto re-enable job runs at:
  - `daily-auto-reenable-cron` (default `0 0 2 * * *`)
  - `quota-reset-zone` (default `Europe/Istanbul`)
- Default resume window is `02:00` from:
  - `auto-reenable-hour` (default `2`)
  - `auto-reenable-minute` (default `0`)
- Resume can also happen opportunistically when gate/call checks run and `auto_resume_at` is due.

## Effective budget math
- Daily budget (micros USD):
  - `((monthly_credit_usd * budget_safety_ratio) / billing_cycle_days) * 1_000_000`
- Per-request cost (micros USD):
  - `(price_per_1000_usd / 1000) * 1_000_000` (rounded up conservatively per request)
- Calls are blocked when:
  - `current_daily_cost + next_call_cost > daily_budget`

## Google request shape (current implementation, legacy endpoints)

The client uses Google Places legacy REST paths (`/textsearch/json`, `/details/json`), not Places API v1 field-mask style endpoints.

### Text Search
`GET https://maps.googleapis.com/maps/api/place/textsearch/json?query={category}%20{city}%20Turkey&language=tr&region=tr&key={API_KEY}`

### Place Details (strict minimal fields)
`GET https://maps.googleapis.com/maps/api/place/details/json?place_id={PLACE_ID}&language=tr&fields=formatted_phone_number,rating,user_ratings_total,website&key={API_KEY}`

## Leads table impact
- `leads` table/schema is unchanged by these guardrails.
- New persistence is isolated to:
  - `google_api_daily_usage`
  - `google_automation_state`

## Operator guidance

### Quick health checks
1. Check latest cron logs: `GET /api/cron-logs`
2. Confirm current gate behavior:
   - free-tier lock reason: `Free Tier Exhausted for Today`
   - billing lock reason: `Google billing rejection detected...`
3. Inspect DB rows for today and singleton lock state:
   - `google_api_daily_usage` for today’s `estimated_cost_micros`
   - `google_automation_state` for `pipeline_enabled`, `disabled_reason`, `auto_resume_at`

### Typical response actions
- **Free tier exhausted**: wait until next daily auto re-enable window in `quota-reset-zone`.
- **Billing lock**: fix Google billing/quota/account issue first; then wait for auto re-enable window (or perform approved manual DB intervention).
- **Frequent pauses**: tune `LEAD_GEN_*` volume settings and/or Google pricing/budget envs to match expected run volume.

## Config surface
- `GOOGLE_PLACES_DETAILS_FIELDS`
- `GOOGLE_PLACES_MONTHLY_CREDIT_USD`
- `GOOGLE_PLACES_BUDGET_SAFETY_RATIO`
- `GOOGLE_PLACES_BILLING_CYCLE_DAYS`
- `GOOGLE_PLACES_TEXT_SEARCH_PRICE_PER_THOUSAND_USD`
- `GOOGLE_PLACES_DETAILS_CONTACT_PRICE_PER_THOUSAND_USD`
- `GOOGLE_PLACES_QUOTA_RESET_ZONE`
- `GOOGLE_PLACES_AUTO_REENABLE_HOUR`
- `GOOGLE_PLACES_AUTO_REENABLE_MINUTE`
- `GOOGLE_PLACES_DAILY_AUTO_REENABLE_CRON`

## Related docs
- [Backend overview](README.md)
- [Operations runbook](../operations/README.md)
- [Troubleshooting guide](../troubleshooting/README.md)
- [Production readiness](../production/README.md)

---

## Part 2: Bare-Metal CI/CD & LGTM Observability Troubleshooting
*Source: `docs\setup\getting-started\troubleshooting-architecture.md`*

# Troubleshooting Architecture and Lessons Learned

This document serves to permanently record critical edge cases, server configurations, and bugs encountered to prevent future developers from repeating the same mistakes.

## 1. GitHub Actions CI/CD Pipeline

- **SCP Wildcard Bugs:** The `appleboy/scp-action` action silently fails or parses incorrectly when using wildcards (e.g., `source: "frontend/dist/*"`). The robust solution is to use `source: "./*"` to transfer the entire workspace to a temporary directory (`/home/ubuntu/temp_hitlist`), and then use SSH to `rsync` it into production.
- **Non-Interactive SSH Sudo:** The `appleboy/ssh-action` runs in a non-interactive shell and cannot prompt for passwords. Running `systemctl restart` requires granting the `ubuntu` user NOPASSWD access via:
  ```bash
  echo "ubuntu ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/99-github-actions
  ```

## 2. Observability Stack (Loki & Tempo)

- **Loki v11 Schema & Structured Metadata:** The legacy Loki v11 schema strictly rejects structured metadata, causing HTTP 400 Bad Request errors. `logback-spring.xml` must configure the Loki4j appender with `<noStructuredMetadata>true</noStructuredMetadata>` as a direct child of the `<appender>` tag (NOT inside `<format>`).
- **Tempo OTLP Port & Protocol Mismatch:** To avoid collisions with Loki (9095), Tempo's OTLP gRPC receiver is custom-mapped to `4319`. Because the standard OpenTelemetry Java Agent defaults to `http/protobuf`, developers MUST explicitly set `OTEL_EXPORTER_OTLP_PROTOCOL=grpc` in their `.env` / `application.yml`. Failing to do so causes `Connection reset by peer` errors.

## 3. Spring Boot Resiliency

- **Brittle Startup Validations:** Throwing fatal exceptions (like `IllegalStateException`) during `@PostConstruct` or service initialization based on environment variables (e.g., the `GooglePlacesService` strict field validation) is dangerous. Production environments often inject unexpected variables, which will cause crash loops. Use warnings instead.

## Learning Resources & Crash Courses

To help the team master the concepts discussed above, here are high-quality, practical learning recommendations:

1. **Spring Boot Observability:**
   - *Documentation:* [Spring Boot Reference Guide: Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
   - *YouTube:* "Spring Boot 3 Observability with Micrometer and OpenTelemetry" (search for Dan Vega or Josh Long's talks).
   - *Concept Focus:* Learn how Micrometer acts as a facade (like SLF4J for metrics/tracing) and how OpenTelemetry instruments Java applications for distributed tracing.

2. **The Grafana LGTM Stack:**
   - *YouTube Channel:* [Grafana Labs Official Channel](https://www.youtube.com/c/grafana) - check out their "LGTM Stack" overviews.
   - *Tutorial Focus:* Understand how **L**oki handles logs, **G**rafana provides the dashboards, **T**empo stores traces, and **M**imir stores metrics. Pay special attention to configuring Tempo's gRPC receivers and Loki's LogQL.

3. **GitHub Actions:**
   - *Documentation:* [GitHub Actions Documentation](https://docs.github.com/en/actions)
   - *YouTube:* "GitHub Actions CI/CD pipeline tutorial" by TechWorld with Nana.
   - *Concept Focus:* Mastering workflow triggers, job dependencies (`needs`), securely managing secrets, and effectively using SSH/SCP actions for bare-metal deployments (like `appleboy/ssh-action`).

4. **Docker Compose:**
   - *Documentation:* [Docker Compose Overview](https://docs.docker.com/compose/)
   - *YouTube:* "Docker Compose Tutorial" by NetworkChuck or freeCodeCamp.
   - *Concept Focus:* Defining multi-container environments, networking between containers (how services resolve each other by name), and using named volumes for persistent data (vital for databases and Loki/Tempo storage).

---

## Part 3: Infrastructure Security & Hardening Checklist
*Source: `docs\guides\developer-guide\best-practices-review.md`*

# Best Practices Checklist Review

Based on the review of the existing repository assets (Nginx configs, deployment scripts, and operations/production runbooks), the following critical best practices must be added to the developer guide to bridge existing security, performance, and reliability gaps:

## 1. Nginx Hardening
Currently, Nginx has basic security headers but lacks performance and attack mitigation configurations.
- [ ] **Rate Limiting:** Implement limit_req_zone and limit_req to protect the API (/api/) and auth endpoints from brute-force and DDoS attacks.
- [ ] **Buffer Overflow Protection:** Define limits like client_body_buffer_size, client_header_buffer_size, and large_client_header_buffers to prevent buffer overflow attacks.
- [ ] **Compression:** Enable gzip with optimal compression levels and types to improve frontend delivery performance.
- [ ] **Server Tokens:** Add server_tokens off; to hide the Nginx version.

## 2. systemd Security (Sandboxing)
Beyond the currently documented NoNewPrivileges, ProtectSystem, and ProtectHome, enforce strict systemd sandboxing:
- [ ] **Private Directories:** Add PrivateTmp=true and PrivateDevices=true.
- [ ] **Kernel & Control Groups:** Use ProtectKernelTunables=true and ProtectControlGroups=true.
- [ ] **Network Restrictions:** Restrict socket creation with RestrictAddressFamilies=AF_INET AF_INET6 AF_UNIX.

## 3. PostgreSQL + PostGIS Backup Automation Gaps
While pg_dump via cron is mentioned, the automation strategy is incomplete for production.
- [ ] **Point-in-Time Recovery (PITR):** Enable WAL archiving (Write-Ahead Logging) for PITR instead of relying solely on daily dumps.
- [ ] **Automated Testing:** Implement a pipeline to periodically auto-restore the backup to a staging environment to verify integrity.
- [ ] **Monitoring & Alerting:** Add cron monitoring (e.g., Healthchecks.io or Cronitor) to alert if the daily backup script fails.
- [ ] **Encryption:** Ensure backups are encrypted at rest before being uploaded to offsite storage (e.g., AWS S3).

## 4. Redis Security
Redis is part of the stack but lacks documented security baselines.
- [ ] **Network Binding:** Strictly bind Redis to localhost (ind 127.0.0.1) to prevent external access.
- [ ] **Authentication:** Enforce a strong password via equirepass in edis.conf and inject it into the Spring Boot environment.
- [ ] **Disable Dangerous Commands:** Rename or disable commands like FLUSHALL, FLUSHDB, CONFIG, and KEYS to prevent accidental or malicious data wipes.

## 5. CI/CD Pipeline Security
- [ ] **Secret Scanning:** Integrate tools like Gitleaks or TruffleHog to actively block commits containing hardcoded secrets.
- [ ] **SAST/DAST Integration:** Expand beyond CodeQL by including dedicated SAST tools for both Java (e.g., SpotBugs/SonarQube) and React, plus container scanning (e.g., Trivy).
- [ ] **SBOM Generation:** Automatically generate and publish a Software Bill of Materials (SBOM) during the build phase.

## 6. Logging & Monitoring Gaps
- [ ] **Centralized Logging:** Move away from raw text files in /var/log/nginx/. Implement a log forwarder (Promtail, Filebeat) to aggregate logs into a centralized system (Loki, ELK).
- [ ] **Application Performance Monitoring (APM):** Integrate OpenTelemetry or an APM agent for tracing request latency across Nginx, Spring Boot, and PostgreSQL.
- [ ] **Log Rotation:** Ensure logrotate is explicitly configured for all custom application logs to prevent disk exhaustion.

## 7. SSL/TLS Best Practices
- [ ] **HTTP Strict Transport Security (HSTS):** Add dd_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always; to Nginx.
- [ ] **Modern Protocols & Ciphers:** Disable TLS 1.0 and 1.1. Restrict Nginx to TLS 1.2 and 1.3 with a strong cipher suite.
- [ ] **OCSP Stapling:** Enable OCSP stapling (ssl_stapling on;, ssl_stapling_verify on;) to improve TLS handshake performance and privacy.

