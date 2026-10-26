# Author workspace and AI drafts: demo script

Five steps, each through the Gateway (Swagger or curl), all as `CONTENT_AUTHOR` (the demo account `content@gmail.com`)
except where a learner is named. Start from a clean stack: `docker compose down -v && docker compose up -d --build`.
Request bodies are in the linked contracts.

`ADMIN` (`admin@gmail.com`) must get `403` on every route below; check one of them first.

**Model.** Steps 2 and 3 call a language model. With `CONTENT_LLM_BASE_URL`, `CONTENT_LLM_API_KEY` and
`CONTENT_LLM_MODEL` set in the root `.env`, the real model answers. Without them the draft ends `FAILED` with
`NOT_CONFIGURED` and the use is given back: that is the expected behaviour, not a fault. To run the script without a key,
point `CONTENT_LLM_BASE_URL` at any OpenAI-compatible stub that answers with JSON in the shapes the contracts describe,
and say in the report that a stub, not a real model, was used. Never present a stub run as a real-model run.

Preparation: the seeded courses `IELTS_5_5` and `IELTS_6_5` already own bands 5.5 and 6.5, so a new course in steps 3
and 4 needs a free band (for example 7.0 or 8.5) and topic sort orders no top-level topic uses; the knowledge points
for the AI course request come from the seeded tree (`GET /api/content/knowledge-tree`). The account's daily allowance
is 20 AI uses and 20 imports.

## 1. A `TOPIC_TEST` for a topic, published, and the learner sees it

Contract: [content-package-authoring-v1](../contracts/content-package-authoring-v1.md).

1. `POST /api/content/admin/packages` with `packageType = TOPIC_TEST` and the `topicId` — created `DRAFT`.
2. `POST …/{packageId}/versions`, `POST …/versions/{versionId}/sections`, then `POST …/sections/{sectionId}/questions`
   for learning questions that no other package uses (a question has one owner).
3. Publish the version (existing publish route). A question already used elsewhere answers `422 QUESTION_ALREADY_USED`.
4. `CUSTOMER`: `GET /api/learning/topics` and `GET /api/learning/topics/{id}/lessons` show the topic; once its lessons are
   done, the topic test the learner is given can be the new code (the learner is assigned one of the topic's published
   codes, see [lesson-learning-v1](../contracts/lesson-learning-v1.md)).

## 2. Five AI questions for a knowledge point, one edited, approved

Contracts: [ai-drafts-v1](../contracts/ai-drafts-v1.md), [question-drafts-v1](../contracts/question-drafts-v1.md).

1. `POST /api/content/admin/ai-drafts/questions` — `{ knowledgePointId, questionType: "MULTIPLE_CHOICE", count: 5,
   difficulty: "MEDIUM" }`; `202` with a `QUEUED` draft. Poll `GET /api/content/admin/ai-drafts/{id}` until `READY`.
2. Read `result.items` and `result.rejected` (each dropped item has its reason, for example `DUPLICATE_STEM`).
3. `PATCH …/{id}/items/1` with a new `stem`: `200`. The same patch with a stem the knowledge point already has returns
   `422 INVALID_DRAFT_ITEM` with `reason = DUPLICATE_STEM`.
4. `POST …/{id}/approve` with `{ "itemIndexes": [0, 1, 2] }`: the chosen items become `DRAFT` questions with
   `importRef = ai-<draftId>-<index>`; they appear in the question bank as `DRAFT` and a learner cannot see them.
5. Approving again answers `400` (the draft is `APPROVED`).

## 3. An AI course for "6.5, Paraphrase and TFNG", approved as an inactive course

Contracts: [course-drafts-v1](../contracts/course-drafts-v1.md), [ai-drafts-v1](../contracts/ai-drafts-v1.md).

1. `POST /api/content/admin/ai-drafts/courses` — `{ bandLevel: 7.0, skills: ["READING"], topics: ["Paraphrase",
   "True/False/Not Given"] }` (6.5 is taken by the seeded course); poll until `READY`.
2. Read the draft: the lessons, `result.analysis.gaps` (practice material still missing), `warnings` and
   `unmatchedTopics` (a phrase that matched nothing; nothing is invented for it).
3. `GET /api/content/admin/course-drafts/{id}/analysis` shows coverage and any `PREREQUISITE_ORDER` error. Fix the draft
   with `PUT …/course-drafts/{id}` if it has one.
4. `POST …/course-drafts/{id}/approve`: a course `INACTIVE` with `INACTIVE` topics, `DRAFT` lessons and empty `DRAFT`
   final tests. A band, code or topic sort order that is already used is refused with `409` and the list of conflicts, and
   nothing is created.
5. Fill the final tests on the packages screen (step 1) and publish them.

## 4. Excel: template, one bad row, fixed, imported

Contract: [course-import-v1](../contracts/course-import-v1.md).

1. `GET /api/content/admin/course-import/template` — the workbook. Fill it (row 3 onward) with one course, one topic, two
   lessons, a few questions, a practice set and tests, using real knowledge point codes and a band and topic sort order not used by step 3 (for example 7.5).
2. Break one cell (for example a `knowledgePointCodes` value that does not exist) and
   `POST …/course-import/validate`: `valid = false`, and the error names the sheet, row and column
   (`UNKNOWN_KNOWLEDGE_POINT`); nothing is written.
3. Fix it, validate again (`valid = true`, `summary` counts, warnings such as `FEW_PRACTICE_SETS`), then
   `POST …/course-import`: `201` with the ids. The course is `INACTIVE`, lessons, questions and packages `DRAFT`.
4. Import the same file again: `422` with `CODE_CONFLICT` errors; nothing is created twice.
5. A file with a formula cell, a macro, or more than 5 MiB is refused (`FORMULA_NOT_ALLOWED`, `MACRO_NOT_ALLOWED`,
   `413 FILE_TOO_LARGE`).

## 5. Activate the course; the learner studies it

1. Publish the lessons (and the final tests of the first topic), then `PUT /api/content/admin/courses/{id}` with
   `status = ACTIVE` and the topics `ACTIVE` (existing authoring routes).
2. `CUSTOMER`: `GET /api/learning/courses` lists the course and `GET /api/learning/topics` its topics; open the first
   lesson and work toward the first topic's final test.

## What this script does not prove

Steps that need a real model are only as real as the key behind `CONTENT_LLM_*`. The automated tests use a fake model
port; they cover shape, ownership, approval and rollback, not the educational quality of what a model writes.
