# Lesson writing API v1

**Status: approved 2026-10-01.** Owner: Learning Service (`/api/learning`). Extends
[`lesson-learning-v1.md`](lesson-learning-v1.md) with Writing essay blocks: Task 2 (plan `260930-0737`) and Task 1 Academic (plan `260930-0812`, added 2026-10-01).

**Implemented 2026-10-01** in `learning-service` (`SubmitLessonEssayUseCase`, `GetWritingSubmissionUseCase`, `EssayGrader`), Task 1 and Task 2 alike.
In `latestSubmission`, `overallBand` and `passed` are `null` unless the submission is `GRADED`, so a withheld grade is
never visible. `failureCode` values: `LLM_UNAVAILABLE`, `INVALID_GRADE`, `INVALID_PROMPT`, `DAILY_LIMIT_REACHED`,
`GRADING_ABANDONED`. A resend of a `FAILED` `requestId` grades the essay text stored with it, not a new text.

Band scores are an **estimate** produced by an LLM against the public IELTS band descriptors. They are not an official
IELTS result.

## Essay blocks inside a lesson

Content marks an `EXERCISE` block that holds exactly one `ESSAY` question with `blockKind = "ESSAY"`
([`learning-content-internal-v1.md`](learning-content-internal-v1.md)). Learning Service applies these rules on top of
`lesson-learning-v1`:

- `GET /lessons/{id}` returns an essay block as
  `{blockId, blockType: "EXERCISE", blockKind: "ESSAY", sortOrder, question: {questionVersionId, stem, task, minWords, passBand, images}, latestSubmission, sampleAnswer?}`.
  `images` is `[{mediaUrl, altText}]` in Content `sortOrder` (empty for Task 2). Clients render it only through `<img src>`, never as inline SVG, because `mediaUrl` may be a `data:image/svg+xml` URI. `chartFacts` is never returned.
  `latestSubmission` is `{id, status, overallBand, passed}` of the learner's newest submission for the block, or `null`.
  `sampleAnswer` (the Content `explanation`) is present only after the learner has passed the block. `answerSpec` is
  never returned. Auto-graded blocks gain `blockKind: "EXERCISE"` and are otherwise unchanged.
- A lesson is complete when every block with `blockKind = "EXERCISE"` has passed. Essay blocks do not count, so a
  learner without points can still finish every lesson and topic. A lesson with no auto-graded block uses
  `POST /lessons/{id}/complete`.
- `POST /lessons/{id}/exercises/{blockId}/submissions` on an essay block → 409 `ESSAY_BLOCK`.
- Lesson gates (`REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`) apply to essay submissions exactly as to exercises.

## Course practice essays (2026-10-20)

The open attempt returned by `POST /api/learning/lessons/{lessonId}/practice-attempts` and
`GET /api/learning/practice-attempts/{attemptId}` contains essays in `sections[].essays`, alongside the section's
auto-graded questions. Each essay has `{questionVersionId, sortOrder, stem, task, minWords, passBand,
latestSubmission, guide, images}`. `guide` is learner-facing Markdown or null, separate from the grading explanation;
`images` is an ordered array of `{mediaUrl, altText}`, empty for a prompt with no attached image. URLs are validated
by Content and rendered as image sources. The response does not expose `chartFacts`, `answerSpec` or `explanation`.

Course Writing uses Introduction/Overview/Body 1/Body 2 for Task 1 and Introduction/Body 1/Body 2/Conclusion for Task 2.
The frontend joins non-empty parts with paragraph breaks and submits the same `{requestId, essayText}` payload to
`POST /api/learning/practice-attempts/{attemptId}/essays/{questionVersionId}/submissions`. Grading, charging and the
requirement to grade every essay before finishing the set are unchanged. The chart URLs are also frozen in the
submission's existing prompt snapshot. No timer is introduced; dictionary lookup remains available beside the editor.

Content V64 seeds the initial guides and four public Supabase chart references for the ten V63 course Writing tasks.
V66 adds paraphrasing and vocabulary guidance to those ten guides and clarifies Task 1 overview choices. V67 adds a
third W1 Task 1 table prompt and its fifth chart reference. Image upload is performed by the content owner; files and exact URLs are in
[course-writing-charts](../assets/course-writing-charts/README.md).

## `POST /lessons/{lessonId}/essays/{blockId}/submissions`

Request: `{"requestId":"<UUID>","essayText":"..."}`. The essay is graded **within the request** using one LLM request
for both criterion bands and sub-point judgments. Its default timeout is 20 s; service and payment work add latency.

Order: validate input → look up `requestId` → load the lesson from Content → check the point balance → gate check and
record `GRADING` → daily grading limit → LLM grading → debit points → record `GRADED` and mastery evidence.

Response 200:

```json
{
  "submissionId":"…","status":"GRADED","task":"TASK_2","wordCount":312,
  "overallBand":6.5,"passed":true,
  "criteria":[
    {"code":"TR","band":6.5,"strengths":["…"],"improvements":["…"]},
    {"code":"CC","band":6.0,"strengths":["…"],"improvements":["…"],
     "knowledgePointJudgments":[{"knowledgePointId":"…","code":"DEMO_WRITING_LINKING",
       "name":"Từ nối và mạch lạc","judgment":"FAIL","excerpt":"…","reason":"Dùng từ nối phù hợp."}]},
    {"code":"LR","band":6.5,"strengths":["…"],"improvements":["…"]},
    {"code":"GRA","band":6.5,"strengths":["…"],"improvements":["…"]}
  ],
  "corrections":[{"excerpt":"peoples can save","suggestion":"people can save","category":"GRAMMAR","errorCode":"PLURAL_FORM"}],
  "studyPoints":[
    {"kind":"CRITERION","criterionCode":"CC","band":5.5,"knowledgePointId":"3c000000-0000-4000-8000-020000000003","code":"DEMO_WRITING_CRITERION_CC","name":"Mạch lạc và liên kết (CC)","title":"Mạch lạc và liên kết (CC)","reason":"Nối câu bằng từ nối đúng chức năng."},
    {"kind":"KNOWLEDGE_POINT","criterionCode":"CC","band":6.0,"knowledgePointId":"…","code":"DEMO_WRITING_LINKING","name":"Từ nối và mạch lạc","title":"Từ nối và mạch lạc","reason":"Dùng từ nối phù hợp.","excerpt":"…","practiceKnowledgePointId":"…"},
    {"kind":"AI","title":"Cohesive Devices","reason":"Dùng từ nối tự nhiên để liên kết câu và đoạn mạch lạc."}
  ],
  "summary":"…","pointsCharged":3,"sampleAnswer":"…"
}
```

- `criteria`: exactly `TR`, `CC`, `LR`, `GRA` for `TASK_2`, and `TA`, `CC`, `LR`, `GRA` for `TASK_1` (Task Achievement, judged against the question's `chartFacts`); each band is 0–9 in steps of 0.5; `strengths` and `improvements` hold at
  most 3 items of ≤ 300 characters.
- `overallBand` is computed by Learning Service, not taken from the LLM: the mean of the four bands rounded to the
  nearest half band, with .25 rounding up to .5 and .75 up to the next whole band.
- `corrections`: at most 10 with prompt template `v1`, at most `learning.writing.max-corrections` (default 30, range 1–50)
  with template `v2`; `excerpt` is an exact substring of the essay; `category` remains the broad category
  (`GRAMMAR`, `VOCABULARY`, `COHERENCE`, `TASK` or `OTHER`) and the additive `errorCode` identifies a matching active,
  human-approved Content error type. A correction with an unknown, inactive or unapproved `errorCode` is omitted.
  Each correction also carries `errorName`, `fixHint` and `severity` (1–3) copied from that error type when the grade
  is stored, and `practiceKnowledgePointId` with `practiceKnowledgePointCode` only when the error type maps to a knowledge point that has a practice set.
  These fields are absent (not null) on grades stored before they were added and are not backfilled.
- `model` is stored with the grade for audit (value of `LEARNING_LLM_MODEL`); the learner API does not return it.
- Quota: a grading that fails for a system reason (`LLM_UNAVAILABLE`, `AI_BUSY`, `RUBRIC_UNAVAILABLE`,
  `PROMPT_VERSION_UNAVAILABLE`) gives its daily grading unit back. `INVALID_GRADE` keeps the unit, because a malformed
  output may be caused by the essay itself.
- `bandLimits` (optional, present only when a cap lowered a band): `[{reason, criterion, cap, message}]`, the same shape
  as Speaking's `bandLimits` plus `criterion` (TA or TR). `reason` is `TOO_SHORT`, `OFF_TOPIC`, `T1_NO_OVERVIEW`,
  `PARTIAL_ANSWER` or `NO_POSITION`. A criterion lowered by a cap carries `limited: true`. Caps are off until
  `learning.writing.band-caps-enabled` is true and the thresholds are set; while off, responses carry neither field.
  The length rule needs only the essay. `T1_NO_OVERVIEW` fires when the second prompt template reports
  `hasOverview = false`; `PARTIAL_ANSWER` and `NO_POSITION` fire from the Task 2 checks below. `OFF_TOPIC` has no check
  behind it yet and never fires.
- `dataChecks` (Task 1, only for a grade made with the second prompt template; absent otherwise):
  `{wrong: [{excerpt, expected}], correctCount, hasOverview?, overviewAccurate?, hasPersonalOpinion?}`. `wrong` lists the
  sentences of the essay that contradict the chart; `expected` is a short Vietnamese statement of what the chart shows.
  Sentences judged right come back only as `correctCount`. A flag the model did not give is omitted. The chart facts
  themselves are never returned. A wrong figure also appears in `corrections` with the code `T1_DATA_MISREPORTED`.
- `taskChecks` (Task 2, same condition): `{questionParts: [{part, addressed, excerpt?}], position?: {required, stated,
  excerpt?, consistent?, conflictExcerpt?}, outline: [{role, mainIdea, developed, excerpt?}], partialAnswer, noPosition,
  inconsistentPosition, noConclusion}`. `addressed` is `FULL`, `PARTIAL` or `NONE`; `role` is `INTRODUCTION`, `BODY` or
  `CONCLUSION`. `questionParts` has at most 4 items and `outline` at most 8. The four flags are derived: `partialAnswer`
  when some part is not `FULL`; `noPosition` when the question asks for an opinion and the essay gives none;
  `noConclusion` only when an outline was returned and has no `CONCLUSION`. These checks add no evidence and no
  correction; they are shown to the learner and feed the band caps.
- `corrections[]` also carries `start` and `end` (the character range `[start, end)` of `excerpt` in `essayText`) and,
  from the second prompt template, `reason` (one Vietnamese sentence naming the broken rule) and `certainty` (`ERROR` or
  `SUGGESTION`; a missing value reads as `ERROR`). Each quotation takes the first occurrence that no earlier correction
  with the same quotation took; a quotation with no free occurrence has no `start`/`end`. Overlapping quotations of
  different corrections each keep their own range; the client decides which one to draw.
- `errorGroups` (absent when there are no errors): `[{errorCode, errorName, category, count, severity, fixHint,
  example?: {wrong, right}, practiceKnowledgePointId?}]`, worst first by `count x severity`, fallback codes (`*_OTHER`) last.
  Only `ERROR` corrections count; a `SUGGESTION` never starts a group.
- `priorities` (absent when there is nothing to prioritise): at most 3 items
  `{title, explanation, excerpts, action, errorCodes, practiceKnowledgePointId?, source}`. `source = AI` when the model
  chose them (second prompt template); otherwise `RULE`: the three worst `errorGroups`, with `title = errorName`,
  `action = fixHint` and the first correction of the group as the excerpt. `errorGroups` and `priorities` are worked out
  when the grade is read, from what is stored; no call to Content or the model is made.
- `criteria[].evidence` and `criteria[].descriptor` (second prompt template only, omitted when empty): the quotations that
  support the band and the rubric descriptor that matched.
- `criteria[].nextBand` (`{band, descriptor}`, omitted at the top of the rubric and for grades stored before it was kept):
  the lowest band the rubric describes above the band shown, which is the capped band when a cap applied. It is
  taken from the rubric at grading time and stored with the grade.
- `revision` (only for an essay sent again, see [essay-revision-v1](essay-revision-v1.md)):
  `{previousSubmissionId, fixedCodes, repeatedCodes, newCodes}`. The error codes of the version before are compared with
  those of this one: fixed (gone), repeated (in both), new (only here). Suggestions and fallback codes (`*_OTHER`) do not
  count. Omitted when the earlier version cannot be read.
- Every quotation in these fields is an exact part of `essayText`; one that was not is dropped. A reply with a missing or
  malformed `taskChecks` gives empty lists and unknown flags, and never fails the grade.
- `summary` ≤ 600 characters. `passed = overallBand ≥ passBand`. `sampleAnswer` only when `passed` or the block was
  passed before.

`studyPoints` (added 2026-10-21) is always an array for a `GRADED` essay, possibly `[]`. It is computed once during
grading, outside the submission transaction, and stored in the grade JSON; reads never recalculate it. Older grades
without this field return `[]`. Null fields inside a point are omitted.

Each criterion also includes `knowledgePointJudgments`, an ordered array that is `[]` for grades without stored judgments.
Each judgment has `knowledgePointId`, `code`, `name`, and `judgment` (`PASS`, `FAIL` or `NOT_ASSESSED`); `excerpt` and `reason` are omitted
when no evidence is available. When the rubric lists sub-points, Learning appends `essay-kp-judgment-v2` instructions to
the unchanged band template `v1`; one response supplies `criteria`, `corrections`, `summary`, `focus` and `judgments`.
Rubrics without sub-points do not require `judgments`. `promptVersion` is
`<rubricCode>@<rubricVersion>+tplv1` for band activation, or `+tplv2` when an administrator has activated a run made with the
second template (`docs/contracts/essay-prompt-template-v2.txt`); with nothing activated, `v1` is used and an unknown template
version is `PROMPT_VERSION_UNAVAILABLE`. Template `v2` additionally returns, and the grade JSON stores (not yet exposed by the API),
per correction a `reason` (at most 300 characters, optional) and `certainty` (`ERROR` or `SUGGESTION`; missing or unknown reads as
`ERROR`; a `SUGGESTION` never creates a knowledge-point `FAIL` and is not counted in the essay event's `error_code`), per criterion
`evidence` (exact quotations, at most 3; a quotation not in the essay is dropped and the band is kept) and `descriptor`, at most 3
`priorities` and one `errorExamples` entry per error code in `corrections`. Missing optional parts never make the grade `INVALID_GRADE`.
Band activation is unchanged; internal grade JSON stores `judgmentVersion=kpj2` whenever the rubric has
sub-points (the prompt was extended), even if the response has no judgments array, otherwise null. This version is not
exposed by the API. `EssayKnowledgeJudge` validates the received data without LLM or clock
dependencies; the grader supplies a lazy Content catalog, loaded at most once when needed for practice targets.
The essay template's single source is `docs/contracts/essay-prompt-template-v*.txt`; Maven copies it to the root
classpath of Learning and Assessment. Each grader owns the `<rubricCode>@<rubricVersion>+tpl<templateVersion>`
identifier; the shared LLM transport does not construct or interpret it.
Missing or malformed judgments preserve a valid band and leave the affected model
judgments `NOT_ASSESSED`; validated corrections whose active error type maps to a knowledge point can still produce
`FAIL`. Corrections and judgments come from the same model response and are checked against application rules.

There is no second request to repair JSON and no automatic HTTP retry for provider 429/503. Invalid JSON or invalid
criterion bands fail with stored `INVALID_GRADE` (public `GRADING_UNAVAILABLE`), without charging points. A provider
failure or timeout fails the grading without charging points. The single request uses
`learning.writing.grading-timeout-seconds` (default 20 s); there is no separate judgment timeout or skip setting.

| Kind | Fields and meaning |
| --- | --- |
| `CRITERION` | `criterionCode`, `band` and `knowledgePointId` are required. Only criteria with `band < passBand` and a mapped KP qualify; no criterion points when `passBand` is null. `code`, `name`, `title` and `reason` are optional snapshots of Content's KP details; `title` is its name, and `reason` prefers `canDo` over `description` (maximum 300 characters). If Content is unavailable, grading still succeeds without those details. |
| `AI` | `kind`, `title` and optional `reason` describe a model `focus` entry. At most four entries, split at the first colon, with title limited to 80 and reason to 300 characters; blank or non-text entries are discarded. |
| `KNOWLEDGE_POINT` | A sub knowledge point judged `FAIL`. Carries the parent `criterionCode` and `band`, the sub-point fields, exact validated `excerpt` and `reason`; `practiceKnowledgePointId` is the sub-point ID when it has a practice set, otherwise the parent criterion point ID when available. |

Study points keep criterion points in their existing band/rubric order, followed immediately by failed sub-points for that
criterion; failed sub-points without a criterion study point follow in rubric order, then AI points in model order.
The same response applies to lesson, course Practice, free-writing and revision submissions. Mentor review and
Assessment result shapes are unchanged. The learner can use a criterion point's KP ID with the existing
[free-practice API](free-practice-v1.md); its reserve and exhaustion rules still apply.

## `GET /writing-submissions/{submissionId}`

Only the owner can read a submission; another user's id → 404.

| `status` | Body |
| --- | --- |
| `GRADED` | Same as the POST response |
| `PAYMENT_PENDING` | `{submissionId, status, code}`; the grade is withheld until payment succeeds |
| `GRADING`, `FAILED` | `{submissionId, status, failureCode}` |

## Revisions and Mentor review

A graded lesson essay can be rewritten and sent again (`POST /writing-submissions/{id}/revisions`, `GET
/writing-submissions/{id}/history`): see [essay-revision-v1](essay-revision-v1.md). A learner with a human-grading credit
can ask a Mentor to review a graded essay (`POST`/`GET /writing-submissions/{id}/human-review`): see
[human-review-v1](human-review-v1.md). A Mentor review does change evidence: once the examiner finishes it, its criterion
results replace this essay's AI criterion evidence, and its marked errors count against their trap points (see
[human-review-completed-v1](human-review-completed-v1.md)). Asking for a review still changes no gate.

## Idempotency and payment

- `requestId` identifies one grading. Resending the same `requestId`: `GRADED` → the stored response; `PAYMENT_PENDING`
  → retry the debit only (the LLM is not called again); `FAILED` → grade again on the same submission; `GRADING` younger
  than 120 s → 409 `GRADING_IN_PROGRESS`, older → taken over and graded again.
- 3 points per successful grading (Access Service, `POST /internal/access/points/debit`, idempotency key
  `lesson-writing:{userId}:{requestId}`). Points are charged only after the LLM grade succeeds; there is no refund.
- At most one `GRADING` submission per learner and block.
- Daily limit: at most 10 gradings per learner per day (`Asia/Ho_Chi_Minh`), counted when the LLM is called. Exceeding
  it → 429 `DAILY_LIMIT_REACHED`, no LLM call, no charge.
- Mastery evidence (`source = lesson_writing`) is recorded on every graded submission until the block is first passed,
  and never after. It has one row per knowledge point of the question with `correct = passed`, and one row per rubric
  criterion whose knowledge point is mapped, with `correct = criterion band >= passBand`. A sub-point judgment of
  `PASS` or `FAIL` adds evidence regardless of the essay pass band; `NOT_ASSESSED` adds none. Criterion band evidence
  takes priority when the same KP is mapped twice, and sub-point evidence overrides the question's pass result when IDs
  overlap. Criterion knowledge points appear only in `studyPoints[]`; `criteria[]` exposes sub-point IDs only inside
  `knowledgePointJudgments[]`. The question's event carries the most frequent correction `errorCode`.

## Optional criterion reviews

When a lesson essay records evidence, or a lesson Practice package is submitted with `countedAsEvidence = true`,
failed rubric criterion KPs may receive optional reviews in the same transaction as their evidence. A criterion fails
only when its band is strictly below the prompt's pass band (5.5 against 5.5 passes). Judged sub knowledge points do
not create these reviews. Existing evidence, including sub-point judgments, is unchanged.

The service selects the lowest criterion bands first, breaking ties by KP code, and creates at most
`learning.writing.criterion-review-limit` reviews per submission (default `2`, pending user/mentor confirmation).
A KP with any existing `PENDING` review is skipped. Content must have an eligible unrevealed set with at least three
questions; packages already revealed in Practice/review or assigned in any review are excluded. Availability is
checked once for all candidate KPs. This uses the published `PS-WCR-*` criterion practice sets.

Each new review stores the essay's lesson ID, `skill = WRITING`, `blocking = false`, and
`trigger_kind = WRITING_CRITERION`. These reviews never block lessons, Practice or topic tests and do not change
Practice clearance. They are accessible through `GET /reviews`, `GET /review-basket` (in `weak` when absent from
`required`, `consolidate` and `due`), and the existing review start/submission routes. A successful review set writes `review_set` evidence and updates mastery.
Replaying an existing submission or resubmitting a revealed Practice package creates no additional reviews.

Free writing and essay revisions do not create criterion reviews in this version. Free writing has no lesson,
while `review_items.lesson_id` remains required. Revisions retain their existing evidence behavior but do not trigger
these optional reviews. Learners cannot start a new `PRIORITY` review themselves; basket start only reopens an
existing pending review for that tier.

## Errors

Body `{detail, code}`, as in `lesson-learning-v1`.

| HTTP | `code` | When |
| --- | --- | --- |
| 403 | `LESSON_LOCKED`, `TOPIC_LOCKED`, `REVIEW_REQUIRED` | Lesson gate |
| 404 | `NOT_FOUND` | Lesson, block or submission not found for this learner |
| 409 | `NOT_ESSAY_BLOCK` | Essay submitted to a block whose `blockKind` is not `ESSAY` |
| 409 | `ESSAY_BLOCK` | Exercise submission sent to an essay block |
| 409 | `REQUEST_CONFLICT` | `requestId` already used for another learner, lesson or block |
| 409 | `GRADING_IN_PROGRESS` | The block already has a recent `GRADING` submission |
| 422 | `ESSAY_EMPTY`, `ESSAY_TOO_SHORT`, `ESSAY_TOO_LONG` | Empty; under 50 words; over 1,000 words or 10,000 characters. Checked before the balance or the LLM |
| 402 | `INSUFFICIENT_POINTS` | Balance below the cost before grading (no LLM call), or debit refused after grading (`PAYMENT_PENDING`, with `submissionId`) |
| 429 | `DAILY_LIMIT_REACHED` | Daily grading limit reached |
| 503 | `GRADING_UNAVAILABLE` | LLM not configured, failed, timed out or returned an invalid result; no charge |
| 503 | `PAYMENT_UNAVAILABLE` | Access Service failed or rejected the token during debit; status `PAYMENT_PENDING`, resend the same `requestId` |
