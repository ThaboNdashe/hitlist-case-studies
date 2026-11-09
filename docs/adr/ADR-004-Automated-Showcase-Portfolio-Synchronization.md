# ADR 004: Automated Architecture Showcase & 4-Layer Cross-Repository Synchronization

## Context
Engineering hiring managers, staff engineers, and technical recruiters evaluating a software engineer's problem-solving capability need immediate, high-signal visibility into architectural trade-offs, root-cause diagnostics, production resiliency, and code quality—without having to configure complex local infrastructure or sift through operational noise.

However, maintaining a separate public engineering portfolio repository (`C:\Dev\hitlist-case-studies`) introduces two significant risks:
1. **Documentation Drift (Lehman's Law of Continuing Change):** As the primary production codebase (`C:\Dev\hitlist`) evolves with new ADRs, post-mortems, schema migrations, and test suites, a manually maintained showcase repository quickly becomes stale.
2. **Credential & IP Leakage Risk:** Copying operational runbooks, troubleshooting guides, or configuration examples directly from a working repository risks exposing local database passwords, API keys (`AIzaSy...`, `sk-ant-...`), or private host addresses.

## Root Cause
Traditional portfolio repositories treat case studies as static, one-off snapshots disconnected from the active development lifecycle and Git commit pipeline of the primary codebase.

## Decision
We implemented a **4-Layer Idempotent, Fail-Closed Synchronization Architecture** linking `C:\Dev\hitlist` (Main Repository) to `C:\Dev\hitlist-case-studies` (Public Architecture & Problem-Solving Showcase):

1. **Layer 1: Sanitizing Sync Engine (`scripts/sync-showcase-repo.ps1` & `scripts/sync-showcase-repo.sh`)**
   - Automatically extracts live engineering telemetry from `C:\Dev\hitlist` (ADR count, backend Java source/test/fake counts, frontend component counts, SRS modules, and latest commit SHA/timestamp).
   - Updates bounded auto-sync blocks (`<!-- AUTO-SYNC:METRICS:START --> ... <!-- AUTO-SYNC:METRICS:END -->`) in `README.md` and `docs/EXECUTIVE_BRIEF.md` while preserving curated problem-solving narratives.
   - Consolidates and sanitizes all ADRs (`docs/architecture/ADR-*.md`) and implementation post-mortems (`docs/tasks/implementation/*.md`) into individual `docs/adr/ADR-xxx.md` files and a unified `docs/adr/FULL_ADR_CATALOG.md`.
   - Enforces a **fail-closed regex secret scrubber** that redacts sensitive patterns (`<REDACTED_SECRET>`) before writing to disk and aborts the commit if any unredacted credential pattern is detected.
2. **Layer 2: Version-Controlled Local Git Hooks (`.githooks/post-commit` & `.githooks/pre-push`)**
   - Configured via `git config core.hooksPath .githooks` so every local `git commit` and `git push` automatically synchronizes `C:\Dev\hitlist-case-studies`.
3. **Layer 3: GitHub Actions Cross-Repo Pipeline (`.github/workflows/sync-showcase.yml`)**
   - Executes `scripts/sync-showcase-repo.sh` on `main` branch pushes, verifying sanitization in CI and pushing updates to the remote showcase repository when `SHOWCASE_REPO_TOKEN` is configured.
4. **Layer 4: Continuous AI Agent Enforcement (`AGENTS.md`, `GEMINI.md`, & `scripts/verify-doc-sync.ps1`)**
   - Embeds `verify-doc-sync.ps1` into the mandatory Definition of Done for all collaborative AI agent sessions.

## Consequences
* **Pros:**
  * **Zero Drift:** Every new ADR, post-mortem, or commit in `C:\Dev\hitlist` automatically updates the showcase repository's metrics and ADR catalog.
  * **Defense-in-Depth Secret Protection:** Regex scrubbing + post-sync audit prevents accidental credential disclosure.
  * **Dual-Audience Optimization:** Pairs 60-second recruiter executive summaries with 15-minute staff-engineer deep dives and a zero-setup runnable Maven proof module (`code-proofs/`).
* **Cons:**
  * Adds a lightweight (~1–2 second) post-commit hook execution step to local commits in `C:\Dev\hitlist`.

