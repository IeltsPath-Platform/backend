# Learning and Content dashboard implementation

Date: 2026-10-15
Status: implemented; PostgreSQL verification pending Docker availability.

## Delivered contracts

- `GET /api/learning/statistics/progress?courseId=<optional UUID>&page=0&size=4`: CUSTOMER/current-user only. `page` 0–10000; `size` 1–20. Omitted course selects started courses; selected course must be started, otherwise 404. Returns `generatedAt`, `availability` (`AVAILABLE`/`EMPTY`), `dataFrom=CURRENT_PUBLISHED_CURRICULUM`, `scope` (`STARTED_COURSES`/`COURSE`), `curriculumRevision`, `skills`, `courses`, `page`, `size`, `totalCourses`, `hasNext`.
- Skill entries: `{skill, completedLessons, totalLessons, percent}`. Each distinct published lesson contributes once to every skill it teaches; course totals count it only once. Percent is rounded once to one decimal; no denominator means null. Pagination does not change skill totals.
- Course entries: `{courseId,name,bandLevel,completedLessons,totalLessons,percent,startedAt,lastStudiedAt,activityProvenance,resumeTarget:{type:COURSE,courseId}}`. FE route is `/learn/courses/:courseId`, whose existing gates select the next permitted lesson/review. Active published curriculum only: courses without any currently published lesson are omitted.
- `GET /api/learning/statistics/study-context?sourceType=LESSON|PRACTICE|REVIEW&sourceId=<UUID>`: returns only `{sourceType,sourceId,courseId}`. LESSON uses the existing lesson gate, including permitted rereading of completed lessons; PRACTICE requires an owned unsubmitted attempt and practice gate; REVIEW requires an owned pending review and published lesson. Reviews use their own authorization rather than the lesson gate they deliberately block. Unknown/not-owned = 404, inactive = 409, unsupported source = 400; existing gate errors remain unchanged. No answers or content returned.
- `POST /internal/learning-content/progress-metadata`, body `{courseIds:[UUID...]}` (1–100 IDs): one SQL statement returns `{revision,complete,lessons:[{courseId,courseName,bandLevel,topicId,lessonId,skills}]}`. Forwarded verified bearer and correlation ID use the existing client implementation. Revision fingerprints the exact denominator/skill membership, including removals. Request is internal-only under existing gateway policy.

## Data and query behavior

Additive Learning migration `V11__course_study_activity.sql` stores one row per user/course. Existing `LessonAccess.refresh` records activity after authorization and under the existing per-user transaction lock. This covers successful lesson opens, submissions, and completion flows without changing their gates. First timestamp is immutable; previously existing lesson progress retains null `startedAt` and `LEGACY_UNKNOWN` provenance, with real new activity recorded as `lastStudiedAt`. No historical timestamp is inferred from refresh/update time. Read-only dashboard requests do not enroll/start courses.

Learning uses bounded started-course query, one Content batch, and one owned completed-lesson query. In-memory joins use maps/sets. No HTTP or repository calls occur in lesson/course loops. The metadata SQL caps at 10001 rows to detect overflow; above 10000 lessons it returns `complete=false` and no partial rows. Learning returns 503 `PROGRESS_UNAVAILABLE` rather than a false percentage. Started-course overflow likewise fails closed. No schema deletion, shared/infrastructure edits, new dependencies, or real LLM calls.

## Verification

- Compiled Content and Learning with reactor dependencies: passed (`mvn -q -pl services/content-service,services/learning-service -am test -DskipTests`).
- Focused statistics/context/security tests: passed after fixing explicit request parameter names, an issue caught by MVC tests.
- Added and passed Content batch request validation test, Learning HTTP client batch/auth forwarding test, course activity timestamp/legacy tests, skill count/multi-skill/deduplication/page-scope tests, source ownership/state/gate tests, anonymous/role/current-identity MVC coverage.
- Full Content module suite: 255 tests, 0 failures, 0 errors, 72 skipped (Docker unavailable).
- Full Learning module plus final added focused tests: 244 tests represented by current Surefire reports, 0 failures, 1 existing fixture error, 53 skipped (Docker unavailable). The broad suite originally had 240 tests; 4 new activity/client tests subsequently passed separately.
- Existing error: `AnswerSpecGraderTest.gradesEverySharedContractVector` cannot find `backend/docs/contracts/answer-spec-v1-vectors.json`. Test checks `docs/contracts/...` and `../../docs/contracts/...` from module execution. Read-only search of `D:/Fall2026/New folder/docs` found no answer-spec/vector file to restore. Test unchanged.
- PostgreSQL integration tests added for metadata membership/revision/overflow, migration, legacy ownership/completion, activity uniqueness/first-date preservation. They compile but were skipped because Docker is unavailable. No SQL EXPLAIN or runtime migration claim is made.
- `git diff --check -- services/content-service services/learning-service`: passed (only local LF/CRLF Git warnings).

## Remaining verification limits

Run the existing and new Testcontainers tests with Docker available before release, and restore the authentic shared answer-spec fixture from its maintained source before expecting the full Learning suite to pass. The existing lesson/practice access policies are reused; this change does not redesign entitlement or review gates. Study timestamps represent authorized lesson activity, not measured active duration (the User-service telemetry feature owns duration).
