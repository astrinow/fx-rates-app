# SOLUTION: AI-Assisted Implementation Report

## 1. Actually Used Tools and Models

- AI Models & Assistants: Codex / LLM for iterative code generation, refactoring, and test creation.
- IDE & Development: Android Studio for code navigation, inspections, and Git diff review.
- CLI & Diagnostics: `curl` for low-level HTTP/Cloudflare origin probing; `adb logcat` for capturing Ktor network exceptions and runtime lifecycle events.
- Build & Quality: Gradle CLI, Kotlin Multiplatform compiler, Kotest, Detekt.

## 2. How Project Context Was Provided

- Staged / Modular Prompting: Context was fed iteratively across clearly separated architectural stages (Domain/Data -> MVI -> Decompose UI & Navigation) rather than in a single large prompt.
- Strict Project Boundary Constraints: Prompts enforced existing patterns (`RatesList` conventions, `BigDecimal` with `HALF_UP`, avoiding custom chart libraries or new UI frameworks).
- Documentation Summaries: Key rules from `docs/screen.md` and repository guidelines (`AGENTS.md`) were condensed and injected directly into task instructions.

## 3. Real Agent Errors and Detection Methods

- Error 1: Unauthorized Contract Overhaul. When facing origin timeouts (521/522), the agent attempted to migrate to Frankfurter API v2 and alter DTOs/signatures.
  * Detection: Manual `git diff` review immediately caught unauthorized changes that violated the strict requirement: "Frankfurter API v1, existing Ktor setup".
- Error 2: Scope Creep (UI Testing). The agent attempted to generate automated Compose UI interaction tests.
  * Detection: Prompt & test scope review identified violation of the project rule limiting tests strictly to unit tests (`:shared:testAndroidHostTest`).
- Error 3: Misdiagnosing Server Outages. The agent initially attributed Cloudflare 521/522 timeouts to client-side date formatting rather than origin server downtime.
  * Detection: Independent terminal probing via `curl` and `adb logcat` isolated origin connection drops and deprecation headers.

## 4. Actually Performed Manual Fixes and Interventions

- Contract Protection & Rollback: Explicitly commanded the rollback of all v2 contract modifications, preserving original v1 endpoints and DTO structures.
- Resilience Strategy Design (Fallback): Designed and dictated the architecture for graceful degradation in `RatesRepositoryImpl.getRates()` (fallback to `/v1/latest` with nullable `previousRate: BigDecimal?` upon 5xx / timeout) so the app displays current rates instead of a broken error screen.
- Manual Network Diagnostics: Executed low-level `curl` calls directly in terminal to verify Cloudflare cache behavior (`HIT` vs `MISS`), deprecation status, and weekend/current-day publication constraints of the ECB.
- Code Hygiene & Test Refactoring: Enforced extraction of repeated string literals across test suites into `companion object` constants (P3 review finding).
