# Course import v1

An author fills an Excel workbook that follows the template, checks it, and imports it. One workbook can create a whole
course with its lessons, questions, practice sets and final tests. Import runs in two steps: **validate** reads the file
and writes nothing, and **import** checks it again and then writes everything or nothing.

All routes require `CONTENT_AUTHOR`. Everything an import creates stays unpublished: the course and its topics are
`INACTIVE`, lessons, questions and packages are `DRAFT`. The author reviews and publishes them as usual.

## Routes

| Method | Path | Behaviour |
| --- | --- | --- |
| GET | `/api/content/admin/course-import/template` | Downloads the template, `course-import-template-v1.xlsx`. |
| POST | `/api/content/admin/course-import/validate` | Multipart field `file`. Reads the workbook and reports; writes nothing. `200`. |
| POST | `/api/content/admin/course-import` | Multipart field `file`. Checks again and writes. `201` with what was created; `422` with the report when the file has errors. |

The file must be `.xlsx` (by name and by content), at most 5 MiB and 2000 data rows in all sheets together. Larger
requests return `413 FILE_TOO_LARGE`; another file type, or an empty file, returns `400 NOT_XLSX`; a request without
the `file` part returns `400 MISSING_PART`. Each import takes one use from the
author's daily import allowance (`content.ai.daily-limit`, kept separately from AI requests); a validate does not.
An exhausted allowance returns `429 IMPORT_QUOTA_EXCEEDED` and nothing is written.

## Template

Sheet `Info` has `key`/`value` rows; `templateVersion` must be `1` (`TEMPLATE_VERSION_MISMATCH` otherwise). In every
input sheet row 1 holds the column titles, row 2 describes each column with an example (it is skipped), and the data
starts in row 3. Titles are matched exactly; their order does not matter. Columns with a fixed set of values have a
drop-down, and each title has a cell note. The downloaded template is generated from the maintained workbook and current
active catalogue: read-only `KnowledgePoints` and `ErrorTypes` sheets list available codes. Error types are active and approved. These two sheets are ignored
by the importer; the first data row in each input sheet can include a code suggestion.

| Sheet | Columns |
| --- | --- |
| `Course` (one row) | `code`, `name`, `bandLevel` |
| `Topics` | `code`, `name`, `skill`, `sortOrder` |
| `Lessons` | `code`, `topicCode`, `title`, `summary`, `sortOrder`, `knowledgePointCodes` |
| `Blocks` | `lessonCode`, `sortOrder`, `type` (`TEXT` or `EXERCISE`), `text`, `questionRefs`, `knowledgePointCodes` |
| `Questions` | `importRef`, `type`, `skill`, `stem`, `optionA` to `optionF`, `answer`, `explanation`, `difficulty`, `knowledgePointCodes`, `distractorErrorCodes` |
| `PracticeSets` | `packageCode`, `title`, `lessonCode`, `knowledgePointCodes`, `questionRefs` |
| `Tests` | `kind` (`TOPIC_TEST`, `COURSE_TEST` or `PRACTICE_TEST_MILESTONE`), `packageCode`, `title`, `topicCode`, `afterTopicSequence`, `questionRefs` |

Lists in one cell (`knowledgePointCodes`, `questionRefs`) are separated by commas or semicolons. Codes of courses,
topics, lessons, packages and `importRef` match `[A-Za-z0-9._-]{1,100}`.

- `Lessons.sortOrder` and `Blocks.sortOrder` order the rows inside their topic or lesson; without it the row order is
  kept. `Topics.sortOrder` is the position among all top-level topics and must be free.
- `questionRefs` name `Questions.importRef` values of the file, or `importRef` values of questions that already exist.
- `Questions.type` is `MULTIPLE_CHOICE`, `TRUE_FALSE_NOT_GIVEN`, `FILL_IN_BLANK` or `SHORT_ANSWER`. A choice question
  needs at least two of `optionA` to `optionF`; `answer` is the letter of the right option. A
  `TRUE_FALSE_NOT_GIVEN` question uses the fixed options `TRUE`, `FALSE`, and `NOT_GIVEN`; its `answer` is one of
  those keys. A typed question lists
  every accepted answer in `answer`, separated by `|`. `difficulty` is `EASY`, `MEDIUM` or `HARD`.
  `distractorErrorCodes` is `B=ERROR_CODE;C=ERROR_CODE`: the wrong options that point at an active, approved error code.
  Excel import supports one correct answer per multiple-choice question; multi-select questions are not supported.
- `Tests.topicCode` is required for `TOPIC_TEST` and names a topic of the file. `COURSE_TEST` belongs to the course of
  the file and has no `topicCode`. `PRACTICE_TEST_MILESTONE` creates a practice test, with no test source, that sits
  after topic number `afterTopicSequence` (1 to the number of topics).

## Validate

`POST …/validate` returns `200`, always, with:

```json
{
  "valid": false,
  "errors": [{ "sheet": "Questions", "row": 7, "column": "answer", "code": "ANSWER_NOT_IN_OPTIONS",
               "message": "The answer C is not one of the options." }],
  "warnings": [{ "sheet": "Lessons", "row": 3, "column": "knowledgePointCodes", "code": "FEW_PRACTICE_SETS",
                 "message": "A knowledge point has too little practice material." }],
  "summary": { "topics": 2, "lessons": 5, "questions": 30, "packages": 4 }
}
```

`row` is the Excel row (the first data row is 3); it is `0` for a problem of the whole file. `summary` counts what an
import would create and is omitted when the file cannot be read. Messages are in English and never contain cell
contents. `valid` means no errors.

**Errors**

| Code | Meaning |
| --- | --- |
| `NOT_XLSX`, `FILE_TOO_LARGE`, `UNREADABLE_FILE`, `MACRO_NOT_ALLOWED`, `TOO_MANY_ROWS`, `CELL_TOO_LONG` | The file itself (a cell is at most 20000 characters). |
| `SHEET_MISSING`, `COLUMN_MISSING`, `TEMPLATE_VERSION_MISMATCH` | Wrong shape or template version. |
| `FORMULA_NOT_ALLOWED` | A cell holds a formula. Only values are read. |
| `REQUIRED`, `TOO_LONG`, `INVALID_VALUE`, `DUPLICATE` | A cell is empty, too long, malformed or used twice in the file. |
| `CODE_CONFLICT` | A code, `importRef`, band or topic sort order is already used, or a code conflicts with a locked code setting. |
| `MIXED_SKILLS` | A practice package references questions with different skills; put one skill in each package. |
| `UNKNOWN_REFERENCE` | A `topicCode` or `lessonCode` names nothing in the file. |
| `UNKNOWN_KNOWLEDGE_POINT` | A knowledge point code is not an active one. |
| `UNKNOWN_QUESTION_REF` | A `questionRefs` entry is neither in `Questions` nor an existing import reference. |
| `UNSUPPORTED_TYPE`, `NOT_ENOUGH_OPTIONS`, `ANSWER_NOT_IN_OPTIONS`, `NO_ACCEPTED_ANSWER`, `NO_EXPLANATION`, `DIFFICULTY_REQUIRED`, `UNKNOWN_ERROR_CODE` | A question is not usable. |
| `PREREQUISITE_ORDER` | A knowledge point is taught before its prerequisite, as in [course drafts](course-drafts-v1.md#analysis). |

**Warnings** are the analysis warnings of [course drafts](course-drafts-v1.md#analysis). They never stop an import and
all point at the `Lessons` sheet:

- `KP_NOT_COVERED`: expected knowledge points that no lesson teaches (`row` `0`).
- `NO_PRACTICE_SET`, `FEW_PRACTICE_SETS`, `NO_EXERCISE_QUESTION`: the gaps of practice material for a knowledge point
  the lessons use. `code` is the reason, `column` is `knowledgePointCodes` and `row` is the first row that names the
  point. The workbook's own questions and practice sets are counted, so a gap is only reported for material the
  workbook and the catalogue together still lack.

## Import

`POST /api/content/admin/course-import` repeats the validation. With errors it answers `422` and the report above, and
creates nothing. Otherwise, in one transaction and through the authoring use cases:

1. the course, topics and lessons with their blocks and knowledge points, exactly as an approved
   [course draft](course-drafts-v1.md#approval) (course and topics `INACTIVE`, lessons `DRAFT`);
2. the questions, `DRAFT`, each with its `importRef`, knowledge points, difficulty and distractors;
3. each practice set as a `PRACTICE_SET` package of the lesson it names, each final test as a `TOPIC_TEST` or
   `COURSE_TEST` bound to its topic or course, and each milestone as a `PRACTICE_TEST`. Every package gets one `DRAFT`
   version with one section holding its questions, in the order listed;
4. the milestones, after their topic positions.

Any failure rolls everything back. Response `201`:

```json
{ "courseId": "<uuid>", "topicIds": ["<uuid>"], "lessonIds": ["<uuid>"], "questionIds": ["<uuid>"],
  "packageIds": ["<uuid>"], "packageTitles": { "<uuid>": "Package title" }, "warnings": [ … ] }
```

The practice sets and tests still need their own publication, which checks usage, purpose and gradability like any
other package. The response includes each created package title keyed by its id for the authoring follow-up links.

## Outline of a check

Every check (`validate` and the draft routes) also returns `outline` when the workbook could be read, even if it has
errors: the course, its topics with their lessons, blocks and practice sets, the topic tests, the course tests and
milestones, and the questions. Each node carries its Excel `row` and `cells` (the values of that row, by column), so a
screen can show the course as it will be made and mark the rows the errors point at. Values are shown as written and are
not validated in the outline.

## Drafts

A check can keep the workbook as a draft: the rows as read, so the author opens it again, sees the problems, corrects
them and imports it. Routes under `/api/content/admin/course-import/drafts`, all `CONTENT_AUTHOR`, and an author sees
only their own drafts (another author's draft is `404`).

| Method | Path | Behaviour |
| --- | --- | --- |
| POST | `/drafts` | Multipart field `file`. Keeps the rows and answers `201` with the draft. A file that cannot be read as a whole (wrong shape, formula, macro, oversize cell) answers `422` with the report and keeps nothing. Rows with errors are kept. At most 20 open drafts per author. |
| GET | `/drafts` | The author's drafts: `id`, `filename`, `status`, `importedCourseId`, `courseCode`, `courseName`, `updatedAt`. |
| GET | `/drafts/{id}` | The draft with `sheets` (rows by sheet, each `{number, cells}`) and a `report` checked now against the catalogue as it is, so it is never stale. |
| PUT | `/drafts/{id}` | Body `{ "sheets": { ... } }`. Replaces the rows (only template sheets and columns are kept; a cell is at most 20000 characters, 2000 rows in all), checks again and answers like `GET`. A draft already imported is `409`. |
| POST | `/drafts/{id}/import` | Checks again and creates the course exactly like the file import (same transaction, same daily allowance, same `422` report on errors), then marks the draft `IMPORTED`. |
| DELETE | `/drafts/{id}` | `204`. |

`status` is `DRAFT` or `IMPORTED`. Creating, saving and reading a draft take no use of the daily import allowance; only
the import does.

## File safety

The workbook is untrusted. Only `.xlsx` is read, formulas are refused, macros and external entities are never
executed or resolved, decompression is bounded (size and ratio), and the number of rows and the size of a cell are
limited. Cell contents are never logged. The 5 MiB limit sits inside the 25 MiB multipart limit of the service.
