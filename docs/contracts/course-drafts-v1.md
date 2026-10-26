# Course drafts v1

A course draft is a plan for a whole course: its topics, lessons, the practice sets and exercise questions that go in
them, and the final tests. Nothing in it is real content. An author checks it against the knowledge tree and, when it is
good, approves it. Approval creates ordinary content that learners cannot see yet.

Authors write a draft by hand through the routes below. [AI course drafts](#ai-course-drafts) fill the same structure
from a request. Both are stored as an [AI draft](ai-drafts-v1.md) with `kind = COURSE`, so listing, reading and
rejecting use the routes of that contract (`?kind=COURSE`). A handwritten draft is created already `READY`, with `model`
and `promptVersion` null; it does not use the author's AI allowance.

All routes require `CONTENT_AUTHOR`. An author sees and changes only their own drafts; another author's draft is `404`.

## Structure

```json
{
  "course": { "code": "IELTS_6_5_R", "name": "IELTS 6.5 Reading", "bandLevel": 6.5 },
  "topics": [{
    "code": "R65_PARAPHRASE", "name": "Paraphrase", "skill": "READING", "sortOrder": 970,
    "lessons": [{
      "code": "R65_PARA_L1", "title": "Spot the paraphrase", "summary": "Short text",
      "knowledgePointIds": ["<uuid>"],
      "blocks": [
        { "type": "TEXT", "text": "Written by the author", "knowledgePointIds": ["<uuid>"] },
        { "type": "EXERCISE", "questionRefs": ["<question version uuid or import reference>"] }
      ],
      "practiceSetPackageIds": ["<uuid>"]
    }],
    "topicTest": { "code": "R65_PARA_TT", "title": "Paraphrase final test" }
  }],
  "courseTest": { "code": "R65_CT", "title": "Course final test" },
  "practiceTestMilestones": [{ "packageId": "<uuid>", "afterTopicSequence": 1 }],
  "gaps": []
}
```

- `course.bandLevel` is a half band from 0 to 9. A topic's `skill` is `LISTENING`, `READING`, `WRITING` or `SPEAKING`.
- A `TEXT` block carries text the author writes (it may be empty) and no questions. An `EXERCISE` block lists
  `questionRefs`. A reference is a question version id, or the `importRef` of a question (its latest version is used).
  AI never writes `TEXT` content.
- `topicTest` and `courseTest` name the final tests that approval creates empty. Topics are ordered by `sortOrder`, and
  lessons by their position in the list. The course reads in that order.
- `gaps` is filled by the AI generator; the analysis route recomputes it, so a handwritten draft may leave it empty.
- Limits: at most 30 topics, 50 lessons per topic, 50 blocks per lesson, 100 `questionRefs` per block, 50
  knowledge points per lesson, 20 practice sets per lesson, 100 milestones. Codes match `[A-Za-z0-9._-]{1,100}`; names
  and titles are up to 255 characters, `summary` up to 2000, `text` up to 20000.

## Routes

| Method | Path | Behaviour |
| --- | --- | --- |
| POST | `/api/content/admin/course-drafts` | Body `{ "structure": { … } }`, or empty for an empty draft. Syntax is checked. `201`, returns the draft. |
| PUT | `/api/content/admin/course-drafts/{id}` | Body `{ "structure": { … } }`. Replaces the structure. Only a `READY` draft. Syntax is checked, coverage is not. Returns the draft. |
| GET | `/api/content/admin/course-drafts/{id}/analysis` | The check against the knowledge tree (below). |
| POST | `/api/content/admin/course-drafts/{id}/approve` | Creates the content. Returns what it created. |

The draft response is the one of [ai-drafts-v1](ai-drafts-v1.md); the structure is in `result`. An empty draft is
`{ "course": null, "topics": [], "courseTest": null, "practiceTestMilestones": [], "gaps": [] }`.

Syntax errors return `400` with `details.code = "INVALID_COURSE_DRAFT"` and `details.issues`, a list of
`{ "path": "topics[1].lessons[0].code", "problem": "REQUIRED" }`. Problems are `REQUIRED`, `TOO_LONG`, `TOO_MANY`,
`INVALID_VALUE` and `DUPLICATE` (a code used twice inside the draft). A draft that is not `READY` returns `400`.

## Analysis

`GET …/analysis` returns

```json
{
  "valid": false,
  "errors": [{ "code": "PREREQUISITE_ORDER", "subject": "<kp id>", "related": "<prerequisite kp id>" }],
  "warnings": [{ "code": "KP_NOT_COVERED", "subject": "<kp id>", "related": null }],
  "gaps": [{ "knowledgePointId": "<uuid>", "reason": "FEW_PRACTICE_SETS" }],
  "coverage": { "expected": 12, "covered": 9 }
}
```

`valid` is true when `errors` is empty. Warnings and gaps never stop an approval; errors always do.

**Coverage.** The knowledge points the course should teach are the `ACTIVE` ones whose `cefrLevel` is the CEFR level of
the course band (up to 4.5 `B1`, up to 6.0 `B2`, up to 7.5 `C1`) and whose skill is one of the skills of its topics or
`ALL`. A knowledge point without a level is not expected. `covered` counts the expected points that appear in some
lesson. Each expected point that appears in none is the warning `KP_NOT_COVERED`.

**Errors**

| Code | `subject` | `related` | Meaning |
| --- | --- | --- | --- |
| `PREREQUISITE_ORDER` | knowledge point | its prerequisite | The prerequisite is not taught in an earlier lesson of this course, and is not taught by an existing course of a lower band. A prerequisite in the same lesson does not count as earlier. |
| `KNOWLEDGE_POINT_UNKNOWN` | id | | The knowledge point does not exist. |
| `KNOWLEDGE_POINT_INACTIVE` | id | | The knowledge point is not `ACTIVE`. |
| `QUESTION_REF_UNKNOWN` | the reference | | No question version or import reference matches. |
| `PACKAGE_UNKNOWN` | id | | The package does not exist. |
| `PACKAGE_NOT_PUBLISHED` | id | | The package is not `PUBLISHED`. |
| `PACKAGE_WRONG_TYPE` | id | | A lesson's practice set is not a `PRACTICE_SET`, or a milestone is not a `PRACTICE_TEST`. |
| `PACKAGE_ALREADY_LINKED` | id | | A practice set already belongs to a lesson, or a milestone test already belongs to another course, or the package is chosen twice in the draft. |
| `CODE_TAKEN` | the code | `COURSE`, `TOPIC`, `LESSON` or `PACKAGE` | That code is used by existing content. |
| `BAND_TAKEN` | the band | | Another course already has this band. |
| `TOPIC_SORT_ORDER_TAKEN` | the topic code | the sort order | Another top-level topic already has this sort order. |

**Gaps.** For every knowledge point used in a lesson: `NO_PRACTICE_SET` when no published practice set that belongs to
no lesson measures it, `FEW_PRACTICE_SETS` when there are one or two (three are wanted), and `NO_EXERCISE_QUESTION` when
no `PUBLISHED` question of purpose `LEARNING` measures it. A point can have more than one gap. Gaps are not filled
here: the author asks for questions with the [question drafts](question-drafts-v1.md) and tries again.

## Approval

`POST …/approve` runs the analysis first. Any `CODE_TAKEN`, `BAND_TAKEN` or `TOPIC_SORT_ORDER_TAKEN` returns
`409 CODE_CONFLICT` with `details.conflicts`, the list of those errors. Any other error returns `422
COURSE_DRAFT_INVALID` with `details.errors`. Neither changes anything. A draft that is not `READY` returns `400`.

Otherwise everything below happens in one transaction, through the same use cases the authoring screens use. Any
failure leaves no course, topic, lesson, package or milestone behind and the draft stays `READY`.

1. The course, `INACTIVE`, so learners cannot see it until its author activates it.
2. Each topic, `INACTIVE`, in the course, with the course band, its skill and its sort order.
3. Each lesson, `DRAFT`, in its topic (positions follow the list), with its knowledge points and blocks. `EXERCISE`
   blocks point at the resolved question versions.
4. Each chosen practice set joins its new lesson (its `lessonId` is set).
5. For each topic a `TOPIC_TEST` and for the course a `COURSE_TEST`, `DRAFT`, bound to the new topic or course, each with
   one empty `DRAFT` version. The author fills them in the package screens before activating the course.
6. The practice test milestones of the course. The milestone route of the course screens only takes positions of topics
   that are already live, which the new topics are not, so approval writes them directly. Their tests and positions were
   checked by the analysis and by the syntax check instead (a position is at least 1 and at most the number of topics in
   the draft).
7. The draft becomes `APPROVED`.

Response `200`:

```json
{ "draftId": "<uuid>", "courseId": "<uuid>", "topicIds": ["<uuid>"], "lessonIds": ["<uuid>"],
  "finalTestPackageIds": ["<uuid>"] }
```

Lessons stay `DRAFT` and the course `INACTIVE`: the author publishes the lessons and the final tests, then activates the
course. Nothing here reaches a learner.

## AI course drafts

`POST /api/content/admin/ai-drafts/courses` asks the model for a course plan. `CONTENT_AUTHOR` only.

```json
{ "bandLevel": 6.5, "skills": ["READING"], "topics": ["Paraphrase", "True/False/Not Given"],
  "knowledgePointIds": [], "lessonCount": 8, "instructions": "optional" }
```

- `bandLevel` is a half band; `skills` has at least one of `LISTENING`, `READING`, `WRITING`, `SPEAKING`;
  `lessonCount` is 1 to 30 (default 8); `instructions` is up to 500 characters. At least one of `topics` (up to 10 phrases
  of up to 100 characters) and `knowledgePointIds` (up to the candidate limit) is needed. `knowledgePointIds` is the
  exact way; `topics` are matched to knowledge points by their name, code and description.
- It takes one use from the author's daily AI allowance, stores a `COURSE` draft as `QUEUED` and answers `202` with the
  draft. The model runs in the background, like the question drafts. A failure ends in `FAILED` and gives the use back.
  Other errors are `400` for a bad request and `429 AI_QUOTA_EXCEEDED`.

**The model never invents content.** The service first picks the candidates with fixed rules, and the model may only
group and order them.

1. *Candidates.* The `ACTIVE` knowledge points named by `knowledgePointIds`, plus those the `topics` phrases match
   (a phrase that matches nothing is listed in `unmatchedTopics`; no point is made up for it), plus the prerequisites of
   those points that sit at a lower CEFR level, marked as foundation (they may stay out of the lessons). At most
   `content.ai.max-course-candidates` points (default 40) go to the model, requested points first. They are ordered so
   every prerequisite comes before the points that need it. Each point comes with the published practice sets that belong to
   no lesson and the published learning questions that measure it.
2. *The model* receives the points (code, name, short description, foundation flag), the practice set codes and short
   question labels, never question text or learner data. It returns lessons that group the points, with a name, a
   summary, the practice sets and questions chosen for each lesson, final test names, and a course name.
3. *Check.* Every point, practice set and question the model names must be in the candidate set. Others are dropped and
   reported as the warning `HALLUCINATED_REFERENCE`; one used in two lessons stays in the first and the second is the
   warning `DUPLICATE_REFERENCE`. The result is the structure above with real ids, no `TEXT` content, topic sort orders
   after the existing ones, and `gaps` filled from the analysis. The model's reply is tried a second time if it cannot
   be read; if it still cannot, the draft is `FAILED` with `INVALID_REPLY`.

The draft's `result` is the structure plus `unmatchedTopics`, `warnings` (`{ "code", "subject" }`), `analysis` (the same
object as the analysis route) and `gapHints`, one entry for each knowledge point with a gap:
`{ "knowledgePointId", "reason", "action": "REQUEST_QUESTION_DRAFT" }`. The shortage is not filled here: the author asks
for questions with [question drafts](question-drafts-v1.md) and runs the analysis again.

A draft made by the AI has `model` and `promptVersion`, and is edited (`PUT`), analysed and approved with the routes
above. The extra fields are dropped when the author saves the structure. Approval creates the course `INACTIVE` and empty
`DRAFT` final tests as for any other draft.
