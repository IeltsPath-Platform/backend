# Course Sequence and Final-Test Packages Review

## Summary

Status: DONE_WITH_CONCERNS. Reviewed phase 2 requirements, content production/test diff since `d867758`, new untracked source/migration/tests and the updated internal contract. No code, test or plan edits; no concurrent Maven run. Unrelated user document changes were excluded from review.

Review verdict: PASS. Production implementation satisfies the sequence/package requirements. The FILL publish-validation mismatch found during initial review is corrected and covered by a regression. Fresh full-suite Surefire XML shows 217 content tests: 170 passed, 47 skipped, zero failures/errors. The only remaining concern is Docker-dependent execution; skipped database cases were not verified at runtime.

## Specification checks

| Requirement | Result | Evidence |
| --- | --- | --- |
| New V20; add COURSE_TEST package type, nullable course FK, course-required guard and index | PASS | `V20__course_test_packages.sql`; V20 is the only migration of that version; existing files untouched |
| Course ID retained through package aggregate, JPA and mapper | PASS | Constructor overload, non-null COURSE_TEST invariant, entity column, bidirectional mapper and round-trip test |
| Public creation refuses COURSE_TEST | PASS | CreateContentPackageUseCase rejects before repository calls; use-case and controller cases |
| Publish follows question locks, ownership and LEARNING purpose | PASS | COURSE_TEST added to existing switch; locks precede usage/purpose/spec queries |
| Reading objective-only publish | PASS | Rejects empty package, ESSAY/SPEAKING question type, non-Reading skill and unsupported/malformed CHOICE/FILL shapes; FILL Unicode whitespace now matches Assessment normalization |
| Legacy CHOICE grading semantics | PASS | Missing type defaults to CHOICE; correct must be a nonblank string, matching actual grader |
| All affected query type lists include COURSE_TEST | PASS | Reader exclusion constant, questionsUsedElsewhere list, questionVersionsReservedForLearning list |
| Sequence excludes missing/inactive course, inactive topic and lessonless/unassigned-skill topic | PASS | INNER JOIN courses, ACTIVE filters and existing published-lesson/skill predicates |
| Sequence order skill → course band → sortOrder → id | PASS | Database ORDER BY matches spec exactly |
| Additive course shape with course test flag | PASS | Result/DTO carry courseId, code, name, bandLevel, hasCourseTest |
| hasCourseTest means PUBLISHED package plus non-null current version | PASS | Correlated EXISTS implements the approved definition |
| Course test endpoint shape/status/filter | PASS | Internal route mirrors TopicTestPackageResponse; use case refuses unknown/inactive course; query requires PUBLISHED package and matching PUBLISHED current version |
| No N+1 or per-question repository/client loop | PASS | Course metadata joined in sequence query; KP/spec data batched; use case queries metadata once per version |
| Integration isolation from later course seeds | PASS | CourseSequenceIntegrationTest pins Flyway target 20 and rolls back each test transaction |
| Existing expectation changes constrained | PASS | Tracked existing test diff adds cases/controller mock wiring only; no old assertions rewritten |
| Contract updated and dated | PASS | `learning-content-internal-v1.md`, 2026-10-07: ten routes, sequence shape/order/filter, endpoint, objective publish and reservation rules |

## Resolved finding: FILL could publish although Assessment considered it ungradable

The initially reviewed `PublishContentPackageUseCase` rejected FILL accepted values using `String.isBlank`. The actual Assessment `AnswerSpecGrader.java:31` instead requires `normalize(text)` to be nonempty, using a Unicode-whitespace regex, replacement and trim (line 52).

A spec `{"type":"FILL","accepted":["\u00a0"]}` contains only a nonbreaking space. Java `isBlank` does not classify this character as blank, so the initial Content implementation permitted publication. Assessment normalizes it to an empty string and marks the item ungradable. This violated the approved requirement that every course-test question be auto-gradable.

Worker corrected FILL validation to use `Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS)`, replace whitespace with spaces, trim, and require a nonempty result. Re-read source confirms parity with the grader; CHOICE keeps its matching isBlank check. The new regression rejects NBSP-only alternatives while accepting text surrounded by NBSP. Worker observed the regression red (7 tests, 1 failure) before the fix; fresh CoursePackagesUseCaseTest XML now shows all seven cases passing. No shared business dependency or new library was introduced.

The added course-only reservation fixture now reuses an existing section and increments section-question sort order, avoiding the unique section/order conflicts of the earlier helper snapshot. Current source was re-read and the full suite compiled this corrected helper. Its actual PostgreSQL execution remains skipped.

## Edge cases and boundaries checked

- Existing published ownership compares question_id across versions, excludes the same package, and still obtains locks in stable question-id order.
- Existing practice and topic-test publishing rules retain their original semantics; the new objective check runs only for COURSE_TEST.
- COURSE_TEST guard is additive; other existing package types remain in the SQL CHECK and may have null course_id.
- Version readers already accept all published enum values without a restrictive type predicate; COURSE_TEST is now parsed as a valid enum and round-trip reader coverage is present.
- Course endpoint returns an empty array for a known active course without eligible packages; unknown/inactive courses use the existing 404 handler.
- Internal routes retain the verified-JWT boundary; no Gateway/security changes or new learner answer exposure were introduced.
- Invalid publish checks run before targetVersion.publish, pkg.publishVersion or repository save.
- `git diff --check` exited 0 during review.

## Verification limitations

Worker executed red/green/full Maven verification; this reviewer did not start another Maven process. Worker reports focused green 29 discovered / 23 passed / 6 skipped, then full content green 217 / 170 / 47 with zero failures/errors. This reviewer independently parsed fresh full-run Surefire XML, confirming 217 total, 47 skipped and zero failures/errors; CoursePackagesUseCaseTest has 7 passing cases and CourseSequenceIntegrationTest has 6 skipped cases. CourseSequenceIntegrationTest and old database fixtures require Docker and cannot be inferred from passing unit tests.

Between phases 2 and 3, old seed topics have no course and disappear from topic-sequence by design. The approved old LessonPipelineSeedTest sequence expectation is updated only with phase-3 backfill; no other old expectation change is authorized. Do not claim complete database regression validation while those tests are skipped.

## Next steps

Retain fresh worker test evidence, then proceed to phase-3 backfill on the same branch. No unresolved production or test-source finding remains. Final `git diff --check` exited 0; only normal LF/CRLF conversion notices were emitted.
