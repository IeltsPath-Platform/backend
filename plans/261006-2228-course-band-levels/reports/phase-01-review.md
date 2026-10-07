# Content Course Model Review

## Summary

Status: DONE_WITH_CONCERNS. Read-only review of the phase-one tracked diff and new Course source/test/migration files against `phase-01-content-course-model.md` and the approved decisions in `plan.md`.

Specification compliance passes for the production implementation. The initial migration-test isolation defect and adapter coverage gap have been corrected and re-reviewed. No production defect or unauthorized old-test expectation change was found. Docker tests remain skipped. The content worker owns Maven validation; this reviewer started no concurrent Maven process.

## Specification compliance

| Requirement | Result | Evidence |
| --- | --- | --- |
| Additive V19 schema with exact course columns, band/code uniqueness, status/range/half-band constraints and timestamps | PASS | `V19__courses.sql`; old migrations absent from diff |
| Nullable indexed topic course FK | PASS | V19 `topics.course_id`; matching UUID column in `TopicJpaEntity` |
| Domain protects band and nonblank/length-limited text | PASS | `Course` delegates band validation to existing `BandRange`; validates before update mutation |
| Course list orders by band in DB | PASS | `CourseJpaRepository.findAllByOrderByBandLevelAsc`; adapter preserves order |
| Course list bounded without pagination | PASS | Unique band values from 0 through 9 in half steps allow at most 19 rows |
| GET `/api/content/courses` permissions | PASS | ADMIN, CONTENT_AUTHOR, CUSTOMER, EXAMINER explicitly allowed |
| POST/PUT authoring permissions and 201/200 statuses | PASS | `CourseController`; only ADMIN and CONTENT_AUTHOR |
| Duplicate code/band returns 409, including race after precheck | PASS by source | Prechecks plus `saveAndFlush`, translation of named course unique constraints, existing response handler extension |
| Invalid band/name returns 400; unknown course returns 404 | PASS by source/tests | Bean Validation, Course/BandRange, service exception handler and controller tests |
| Update editable fields only; code retained | PASS | Update command has no code; aggregate code is final |
| Topic course ID flows through create/update/read/tree, domain and persistence | PASS | Request/command/result/response additions, use cases and bidirectional topic mapper |
| Missing course rejected before topic mutation/save | PASS | Create/update topic use cases; new focused tests |
| Topic update null retains course; create without course supported | PASS | Conditional validation/assignment; compatibility constructors; focused tests |
| Layer and scope boundaries | PASS | Pure domain, separate JPA entity, MapStruct mapper, inward repository contract; no Gateway/shared/config/Docker changes |
| Old test expectations unchanged | PASS | Existing test-file diff contains additional cases/imports/controller mock wiring; no removed assertions or changed expectations |
| Red/green and full content test evidence | PARTIAL | Worker reports full green exit 0 before two additional adapter tests: 199 discovered, 158 passed, 41 skipped, zero failures/errors; fresh XML confirms adapter suite now has 5 passing tests |

## Findings

### Resolved: migration tests shared fixtures and depended on method order

`services/content-service/src/test/java/com/group01/content/infrastructure/persistence/CourseMigrationTest.java:32` queries every course ordered by band and assumes the first band is 5.5. The other test inserts `MEMBERSHIP` at band 4.5 (line 59) into the same static container database. If membership runs first, the first query result is 4.5 and the assertion fails.

Worker corrected the query to `WHERE code IN ('LOW','HIGH')` and added the exhaustion assertion. Re-reading the corrected source confirms that the membership row no longer affects the assertion. This was a defect in a new test, not a failing old test or an allowed expectation rewrite. Docker unavailability prevents an executed migration-test confirmation.

### Resolved: race-conflict translation lacked focused coverage

`CourseRepositoryAdapter.save` translates `courses_code_key` and `courses_band_level_key` from nested Hibernate constraint exceptions. Controller duplicate tests currently exercise only prechecks, and `CoursePersistenceTest` does not exercise the adapter's save branch. With Docker unavailable, no executed test covers this error translation.

Worker added focused cases covering both named course constraints becoming `DuplicateCourseException` and an unrelated integrity constraint preserving the original exception. Reviewed source and fresh `CoursePersistenceTest` XML show all five cases pass. Production handling is present and correct by source inspection; no production failure was observed.

### Resolved: course migration fixtures vs later seeds

The later approved course seeds use the same unique bands 5.5/6.5 as the schema test fixtures. Worker pinned `CourseMigrationTest` to Flyway target 19; re-reading line 23 confirms future seeds cannot conflict with this test. `CoursePersistenceTest` has no seed conflict because its courses are mapper objects and its repository is mocked.

## Quality checks

- Method security is active through `CommonSecurityAutoConfiguration`; existing authentication boundary was not weakened.
- Null/blank name, out-of-range/quarter-band input, unknown IDs, update retaining the current band and update changing course status have appropriate handling.
- Course update validates text/band before mutating aggregate fields. Topic course existence is verified before mutation.
- Course mapping includes all fields; generated MapStruct implementation constructs Course and maps UUID/code/name/band/status/timestamps correctly.
- Collection mapping contains no repository/client calls in loops. No relation fetch/N+1 path was introduced.
- Course conflicts extend existing `ErrorResponse` handling; unrelated persistence exceptions are not incorrectly relabeled as duplicates.
- `git diff --check` exited 0 during review.

## Verification limitations and next steps

Controller should retain the worker's red/green command evidence, full content suite result and subsequent adapter result in Verification. All Testcontainers skips must remain explicit; no migration or database-runtime claim is justified until Docker tests run. No source, test or plan files were edited by this reviewer.
