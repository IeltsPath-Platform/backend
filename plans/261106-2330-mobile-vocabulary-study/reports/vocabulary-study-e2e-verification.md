# Vocabulary study live gateway verification

Date: 2026-11-07. Result: accepted matrix **404 passing assertions, zero failed assertions**. These are HTTP/DB assertions, not 404 separate test cases. Earlier diagnostic runs and their failures are retained separately below.

Final fresh learner flow ran from 04:42:33Z to 04:43:34Z (11:42:33–11:43:34 UTC+07). Requests used real login tokens through `http://127.0.0.1:18080`, Gateway internal-token exchange, real services and isolated PostgreSQL. No mocked downstream replaced Access.

## Executed checks

| Evidence | Assertions | Result | Duration |
|---|---:|---|---:|
| Actual OpenAPI, public seed content and nullable media | 58 | PASS | 7.613 s |
| Live timezone/settings and local-day boundary | 14 | PASS | 2.512 s |
| Fresh web baseline before final mobile actions | 5 | PASS | 2.534 s |
| Committed `scripts/verify-vocabulary-study.ps1`, final fresh learner | 239 | PASS | 9.760 s |
| Complete supplemental matrix, same final fresh learner | 88 | PASS | 49.154 s |
| **Accepted matrix** | **404** | **PASS** | Timings above are local execution measurements |

The final main and supplemental commands both exited zero. Additional earlier repetitions are excluded from the 404 total.

### Main learner and public contract

- Anonymous summary returned 401. Both anonymous unit GET routes returned 200 through Gateway. Locked unit start returned 409.
- Actual `/api-docs/library-service` contained all 11 learner and 7 authoring operations. This verifies generated documentation presence; it does not claim a live authoring workflow for every author endpoint.
- Education and environment each exposed four published units, eight words per unit and 24 distinct senses across the topic. All eight seeded units were read live after Flyway V42 completed.
- Start returned 201, resume and snapshot returned 200. WORD, CHOICE, MATCH and FILL ran from public attempt snapshots.
- First incorrect answer lost one heart; repeating the request did not lose another. First imperfect completion earned 20 XP; replay earned zero XP and lost no hearts; perfect second unit earned 30 XP.
- Main result: **50 XP, level 1, streak 1, hearts 4/5**. Subsequent real refill restored hearts to 5/5 without changing XP/streak.

### Real Points, failure recovery and concurrency

- Empty wallet refill returned 402 `INSUFFICIENT_POINTS`; no pending purchase remained and hearts stayed 4.
- A real admin Points adjustment funded only the generated isolated learner. Premium was granted through the real Access admin subscription API.
- Stopping only the isolated Access container produced 503 `ACCESS_UNAVAILABLE`, a PENDING purchase, zero Access debit rows and unchanged hearts.
- While Access was unavailable, cached Premium remained 7/7; failed refresh did not advance its cache timestamp. Starting a unit and acknowledging its public WORD step still worked without heart loss.
- Access was restarted in `finally` and recovered healthy. Retrying the original refill UUID succeeded; replay returned exactly the saved result. Exactly one `vocab-study:<requestId>` ledger debit of five Points existed and purchase status was DONE.
- Full-heart refill returned 409 `HEARTS_FULL`. Freeze without a streak returned 409 `NO_STREAK_TO_PROTECT`.
- Freeze purchase spent ten Points, set ownership, replayed the same result and created one debit.
- Two concurrent refill requests sharing one UUID and a wallet funded with exactly five Points both returned the same 200 result. One debit existed, wallet ended at zero and hearts ended full.

### Premium, flashcards and timezone

- New Premium learner received 7/7 hearts. Expiring only that generated subscription left the fresh ten-minute cache at seven; aging the cache then clamped hearts to 5/5.
- A real incorrect flashcard review qualified the study streak without creating a study profile first. Repeating it left exact streak/XP rows unchanged. Summary showed zero XP, streak one and five hearts.
- Invalid IANA timezone returned 400 `INVALID_TIMEZONE`. `Pacific/Auckland` settings round-tripped live.
- A separate generated learner used `Pacific/Honolulu` for a real flashcard review at UTC date 2026-11-07. Its qualified date was **2026-11-06**, matching that timezone and differing from UTC. No application clock was changed. Auckland midnight itself was not exercised at the current wall clock.

### Two-way independence: exact stored rows

All columns were compared, with **no excluded timestamp or state columns**. Snapshots were scoped to the same generated learner.

| Direction | Compared tables | Timing and observed result |
|---|---|---|
| Mobile actions → web | `learner_xp`, `learner_streaks`, `learner_hearts`, `xp_ledger`, `learner_settings` | Web baseline captured immediately before mobile actions, after baseline web GETs. Post-mobile raw rows captured before another web GET. Exact equality passed. |
| Real web objective submission → mobile | `vocabulary_study_profiles`, `vocabulary_study_xp_ledger`, `vocabulary_study_streaks`, `vocabulary_study_purchases` | Raw mobile rows captured immediately before and after the actual web POST, before another mobile summary GET. Exact equality passed. |

The real web objective changed web XP from zero to **40**, proving a meaningful web action occurred. Mobile remained **50 XP, streak one, hearts five**. Points intentionally remain shared through Access.

### Live metrics and media limits

Actual isolated Library Prometheus counters were scraped after final execution:

```text
vocabulary_study_attempts_started_total 12
vocabulary_study_attempts_completed_total 9
vocabulary_study_access_failures_total 10
vocabulary_study_purchases_total{kind="HEART_REFILL",status="PENDING"} 9
vocabulary_study_purchases_total{kind="HEART_REFILL",status="DONE"} 6
vocabulary_study_purchases_total{kind="STREAK_FREEZE",status="PENDING"} 3
vocabulary_study_purchases_total{kind="STREAK_FREEZE",status="DONE"} 3
```

These counters accumulate across the diagnostic and final runs. PENDING counts are transitions, not counts of currently pending database rows.

All 192 media fields across the eight unit responses were null with optional cloud storage deliberately unconfigured. Nullable response fallback passed. **Zero non-null remote URLs were available to probe; HTTPS asset availability, physical-device audio playback and image rendering remain mobile integration checks.** Local harness timings are not a network performance SLA.

## Preserved diagnostic runs

1. First main runner passed 239 assertions. First supplemental segment recorded **45 pass, one fail**: it expected freeze 200 immediately after the intentional outage. Actual status was 503; its response body was not persisted, so its exact error code is not claimed as observed. The purchase was PENDING. Actual Access circuit-breaker metrics showed six failed calls and one rejected call. Shared source configuration requires ten calls before evaluation, opens at 50% failures and waits 30 seconds; the recovered refill call could open the breaker from the preceding outage history. Retrying the **same freeze UUID** after cooldown returned 200; replay matched and exactly one ten-Point debit existed. Continuation passed **44 assertions**. No production code/configuration was changed for this behavior.
2. A subsequent fresh full scenario recorded **76 pass, one fail** when its web fixture reused the original learner's global submission UUID. Reproduction returned **409 `REQUEST_CONFLICT`**. The harness now generates a fresh submission UUID per learner; no Learning source change was needed.
3. Final fresh baseline/main/supplemental passed **5/239/88**, with both corrections applied. The harness permits one explicit 31-second cooldown retry only after a 503 `ACCESS_UNAVAILABLE`, PENDING state and zero debit; it retains the same UUID and requires 200 after that bounded wait. The final run's first freeze request returned 200, while the preserved first-run evidence proves the post-outage breaker case.

## Runtime and artifact provenance

Only Compose project `capstone-study-e2e` was used. All 12 containers were healthy after final execution. Gateway is port 18080; isolated PostgreSQL 15440, Redis 16380, RabbitMQ 15682/UI 15683 and Eureka 18761. Root config-repo was mounted read-only. Shared `ieltspath-*` containers, databases, volumes and root dotenv files were untouched.

The Library jar was packaged from the verified phase-four **working tree**, with HEAD `1ec9f46541ccf0b53b4f45dc41aae55f8dda29e7` before the feature commit. It includes then-uncommitted V42/metrics production inputs; HEAD alone does not describe the jar. Production source remained unchanged after the copy. Jar hashes and image IDs below identify the actual artifacts.

| Runtime overlay | Jar SHA-256 | Local Docker image ID |
|---|---|---|
| Library, root V42/metrics source | `70ec4d28ae54691a414c3dc2af4f17c0656cf0cde9e09aacc563b2e56539a3e5` | `sha256:ec8eda024d6770f335921edf839d86808ef82c3b41a1b506dd289296e88328d6` |
| Gateway, root public route/security changes | `4a0dcff33f058c89fd9632f368319fdcff1ffe0c6c0b02335d465cf14d1587f8` | `sha256:b79d910c94bbd6d8358bdd45585d3997a050832fc73b75eaf4b82741ced0f6f1` |
| Access, root debit idempotency fix | `65a37c2d9230bb1ff97b1025114d855a2bfd531e817c9543b1622cdab38976f2` | `sha256:1b22398bed4a5a97eacd86ac5c8dc3fd3f31abc6d2b5d969dae14e7ba03e7c7f` |

Packaging used `JAVA_HOME=D:/jdk-21` and `mvn.cmd -pl <module> -o package -DskipTests` separately for `infra/api-gateway`, `services/access-service`, and `services/library-service`. Skipped package tests are not claimed as verification. Owning agents separately reported clean suites: Gateway 58, Access 112, Library 407, all zero failures/errors/skips. Library's successful full run logged Surefire's forced JVM exit warning after tests; it was not hidden.

User, Content and Learning overlays were rebuilt from unchanged current supporting sources in the comment worktree; their recorded source HEAD was `0098b2b8e17d7d89dfa9039ff880d0fbe257ce09`. Exact supporting jar/image hashes remain in the local runtime manifest. Existing October support images were insufficient for current Gateway/profile contracts and were replaced before verification.

## Fixtures, evidence and cleanup

Generated accounts, real admin wallet adjustments and real admin Premium grants were isolated runtime data. Direct SQL fixtures only aged generated learners' plan-cache timestamps, expired their subscription or set one missing heart for the concurrency case. No shared DB or application clock was changed. A separate disposable catalog-probe DB used V1–V38 only for catalog discovery; service verification used the real Flyway-managed `library_db` through V42.

Sanitized machine evidence is under `%TEMP%/capstone-study-e2e/`: `vocabulary-openapi-media-results.json`, `vocabulary-timezone-results.json`, `vocabulary-baseline-results.json`, `vocabulary-main-results.json`, `vocabulary-verify-corrected-results.json`, plus `first-pass-evidence/` and `second-pass-evidence/`. Private credential/session files and runtime secrets are retained locally and must not be committed. The full runner is [verify-vocabulary-study.ps1](../../../scripts/verify-vocabulary-study.ps1).

After coordinator authorization, the helper ran `docker compose -p capstone-study-e2e ... down --volumes --remove-orphans`. All 12 containers were verified healthy and project labels were checked before teardown. At **04:46:05Z**, zero project containers, volumes or networks remained. Unrelated container identities were unchanged. Final health and cleanup evidence is retained in `final-container-health.json` and `cleanup-results.json`; local evidence archives and built images were retained.
