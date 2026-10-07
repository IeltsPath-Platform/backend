# Course Seed Review

## Verdict

Status: DONE. Review verdict: PASS. The new seed satisfies the requested scope. The user authorized the two additional old-test expectation changes after the earlier stop; the final diff contains exactly those updates and the previously approved sequence update. Fresh Docker-backed content tests pass: 224 tests, zero failures, errors or skips; shared common-security has eight passing tests and zero skips. This reviewer independently read the Surefire XML and did not run Maven. No source, test, plan or Git changes were made by this reviewer; only this report is owned.

Reviewed `V21__seed_courses.sql`, `CourseSeedResourceTest`, `CourseSeedTest`, the current `LessonPipelineSeedTest` diff, V1/V3/V8/V9/V17/V18/V19/V20 schema and seed conventions, the JDBC reservation reader, and `answer-spec-v1.md`. The final verification executed PostgreSQL/Flyway integration tests through Docker; the fresh XML has no skipped cases.

## Specification compliance

| Requirement | Result | Evidence |
| --- | --- | --- |
| Additive V21 with fixed UUIDs, two active unique band courses | PASS by source | IELTS_5_5 / 5.5 and IELTS_6_5 / 6.5; no prior migration rewritten |
| Backfill every topic with a non-null skill into the lower course | PASS | Exact broad UPDATE precedes insertion of the new higher-band topic |
| Higher-band Reading topic follows old Reading sort orders | PASS | READING_6_5_INFERENCE has sort 960, skill READING and course 6.5 |
| Two active procedure KPs and one published lesson | PASS | R65_EVIDENCE_INFERENCE and R65_PARAPHRASE; both mapped to R65-I1 |
| Theory TEXT mapped to both KPs plus passage and exercise | PASS | Three ordered blocks; TEXT map has both KPs, ASSET uses the library trial, EXERCISE has two new questions |
| One lesson-linked eligible practice package per KP, at least three questions | PASS | Repair workshop questions 3–5 map to inference; shared garden questions 6–8 map to paraphrase; both packages point to R65-I1 |
| One published topic final test | PASS | Archive pilot questions 9–12; matching topic, package current version, section and passage link |
| One 6–10-question Reading course test per course | PASS | Six questions each: tool library 13–18 for 5.5 and shuttle experiment 19–24 for 6.5 |
| New auto-gradable Reading questions with LEARNING purpose | PASS | All 24 question/version pairs are published; CHOICE keys match options and FILL arrays contain nonblank text |
| No question reused across lesson or packages | PASS by source | Two lesson question owners plus 22 section owners use all 24 distinct new versions exactly once; new question IDs use a previously unused namespace |
| Existing FK/schema/order conventions | PASS by source | Table/column names, allowed enum values, section ownership, lesson/block order, question/version/current-version pointers and existing lower-course KP references match schema |
| Existing sequence expectation change within approved method | PASS | topicSequenceListsEachSkillsTopicsWithLessonsAndTheirActiveKnowledgePoints includes higher course and additive course metadata; original KP assertions preserved |
| Additional expectation changes exactly match user authorization | PASS | Reservation count changes only 147 to 171; practice map gains only the two named R65 entries; queries and other assertions preserved |
| Existing seed tests remain compatible with the added data | PASS | All 32 LessonPipelineSeedTest cases and DemoReadingPassageSeedTest executed with zero failures/errors/skips |

Specification compliance passes. Both earlier scope conflicts have been explicitly authorized and verified after correction; no production requirement gap or unresolved test-source finding remains.

## Resolved findings

### Resolved: global reserved-question count grows from 147 to 171

`LessonPipelineSeedTest.lessonPracticeAndTestQuestionsAreReservedForLearning` selects every `question_versions.id` and passes all IDs to `JdbcLearningContentReader.questionVersionsReservedForLearning`. Its original assertion required 147. The reader UNION includes lesson questions and PRACTICE_SET, TOPIC_TEST and COURSE_TEST section questions.

V21 adds 24 unique new versions: two lesson questions, six practice questions, four topic-test questions and twelve course-test questions. Every version has an owner included by that query, so the result becomes 147 + 24 = 171. The initial review identified this mismatch while Docker cases were skipped and stopped work under the then-current allowlist. The user subsequently authorized exactly `hasSize(147)` to `hasSize(171)`. The final diff makes only that change in this method; its query and other assertion are unchanged. The fresh Docker-backed method passes.

### Resolved: the complete practice-to-lesson map grows from 28 to 30 entries

`LessonPipelineSeedTest.practiceSetsBelongToTheEarliestLessonTeachingTheirKnowledgePoint` selects every PRACTICE_SET joined to its lesson and originally required an exact map of 28 code/lesson pairs. V21 adds `R65-PS-INFERENCE -> R65-I1` and `R65-PS-PARAPHRASE -> R65-I1`, so the complete map now has 30 entries.

This method was initially outside the approved expectation-change list. The user subsequently authorized adding exactly those two map entries. The final diff preserves the original 28 entries, query and other assertions and adds only the two R65 entries. The fresh Docker-backed method passes.

Both findings were sent to the controller and content implementation worker before any unauthorized edits. The stop was lifted after explicit authorization, now recorded in the plan's old-test allowlist. No old test expectation was edited by this reviewer. The initial review did not claim an executed failure; final XML now verifies both corrected methods executed successfully.

## Content and test quality

All six original passages and all 24 stems, correct answers, distractors and explanations were read. Correct choices preserve the passage's scope and certainty; distractors overstate, reverse or introduce unsupported claims. FILL answers are exact one-word passage extracts: damage, unexpected, approved, annual and seats. The higher-band lesson/practice/topic/course material asks about inference, sampling limits, paraphrase and causal uncertainty; the lower-course final emphasizes explicit detail and main ideas. No answer-key mismatch or material ambiguity was identified.

The SQL has six original passage assets and five section passage links, plus a separate lesson ASSET block. Package tests/practice have matching passage contexts. Existing passages, stems and explanations are unchanged. Single-row INSERTs and SQL apostrophe doubling are compatible with the resource parser; the passing Docker-backed migration/reader tests verify PostgreSQL accepts the statements and their JSONB values.

`CourseSeedResourceTest` inspects the actual migration resource and parses its INSERT tuples. It validates course/backfill order, objective shapes/options, published pointers, package question counts, lesson linkage and unique question ownership. All three resource tests pass. This is substantive structural evidence; it does not independently establish that a passage implies an answer. Content correctness was separately reviewed from source and SQL/FK behavior verified by the integration tests.

`CourseSeedTest` pins Flyway target 21, owns a separate static PostgreSQL container and performs read-only assertions. No fixture mutation or ordering dependency exists between its cases. All four cases executed successfully, checking actual reader sequencing, practice availability, course flags/packages, purpose/ownership and answer structures. Prior schema tests remain pinned to targets 19 and 20, avoiding the two new course bands conflicting with their fixtures; those suites also executed with zero skips.

The legacy snapshot and answer tests scope by IDs/codes or namespaces and are unaffected by the new namespace; the two authorized global expectation updates above cover the identified exceptions. The unchanged DEMO_MAIN_FLOW_READING passage test filters its original package code and passes.

## Verification

- Read-only row counts from the current migration: two courses, one topic, two KPs, one lesson, three blocks, six assets, 24 questions and versions, five packages/versions/sections, 22 section question owners, two lesson question owners and five passage links.
- `git diff --check` exited 0 during review.
- Controller/worker final command: `mvn -q -pl services/content-service -am test` exited 0 with Docker available. This reviewer did not rerun it and independently parsed the fresh XML: 40 content suites, 224 tests, zero failures, errors or skips. Common-security: eight tests, zero failures, errors or skips.
- Executed suites include CourseSeedResourceTest (3), CourseSeedTest (4), LessonPipelineSeedTest (32), CourseSequenceIntegrationTest (6), CourseMigrationTest (2), DemoReadingPassageSeedTest (1) and the remaining migration suites. V21 and the earlier migrations were exercised by actual PostgreSQL test contexts.
- Both initially blocked expectation conflicts are resolved by the explicitly approved changes and passing executed tests. No commit was created by this reviewer.
