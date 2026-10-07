# Content baseline verification

Date: 2026-10-07 (Asia/Saigon)
Status: PASS
Commit: `668dca36d210923c213d25389c0bc6372fb54138`

Command: `mvn -q -pl services/content-service -am test`

Maven exited with code 0. Docker Desktop was already running (Docker Server 29.6.1); no startup was necessary. Testcontainers ran PostgreSQL 15.19 from `postgres:15-alpine`, and Flyway successfully migrated the content schema through V22.

| Module | Suites | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| content-service | 41 | 228 | 0 | 0 | 0 |
| common-security | 4 | 8 | 0 | 0 | 0 |
| Total | 45 | 236 | 0 | 0 | 0 |

Counts were read from the modules' Surefire `TEST-*.xml` reports immediately after completion. No failing or skipped test names exist. No old test failed, so the plan's stop condition was not triggered.

No source or test expectations were changed by the baseline tester. No `.env` was opened and no real LLM was called. Only this report was written.
