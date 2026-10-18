# Answer spec v1

**Status: approved 2026-10-01.**

This contract is shared by Content, Assessment, and AI Learning for objective lesson, review, and test questions. Content stores `answer_spec` and exposes `answerSpec` only through `/internal/learning-content/*`. Learner responses never include the spec.

## Shapes and grading

| Shape | Meaning | Example from `seed-content.md` |
| --- | --- | --- |
| `{"type":"CHOICE","correct":"A"}` | Compare the submitted string with an `options[].optionKey`. Keys are case-sensitive (`ii`, `DETAIL`, and `NOT_GIVEN` are distinct). | Q13 |
| `{"correct":"A"}` | Legacy V4 shape; interpreted as `CHOICE`. | V4 choice question |
| `{"type":"FILL","accepted":["critics"]}` | Match any accepted string after normalization. | Q12 |
| `{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":6.0}` | Writing essay (added 2026-10-01, plan `260930-0737`). **Ungradable** by this grader, like `{}`: Learning Service grades it with an LLM through `lesson-writing-v1`. `task` is `TASK_1` or `TASK_2`; `passBand` is 4.0–9.0 in steps of 0.5. `TASK_1` also requires `chartFacts` (1–2,000 characters: the chart's figures and key features, read by the grader instead of the image) and defaults `minWords` to 150. | DEMO_WRITING W2 (Task 2), W1 (Task 1); in L4/L3 before content V15 |

`CHOICE` compares the submitted key exactly. The client sends `optionKey`, not the visible option text. `FILL` trims leading and trailing whitespace, collapses each run of whitespace to one space, and compares without case. It does not remove punctuation or change spelling. For example, Q12 accepts `" Critics "` and rejects `"critic"`. An empty or whitespace-only answer, a missing answer, or JSON `null` is wrong for a gradable spec. Invalid or unsupported specs, including `{}`, are **ungradable**, not a zero-score answer; Assessment retains its manual grading path when any attempt item is ungradable.

For a gradable item, a correct answer receives that item's `maxScore` and a wrong or omitted answer receives `0`. `question_knowledge_points.weight` is attribution metadata; it changes neither grading nor mastery evidence. This v1 contract grades objective `CHOICE` and `FILL` questions; `ESSAY` is recognised only so every consumer treats it as ungradable.

## Response transport

Lesson and review `answers[]` carry `{"questionVersionId":"…","answer":"…"}`. Assessment's existing `SaveAttemptResponseRequest` remains `{payload, schemaVersion, expectedRevision}`: `payload` is a **string containing JSON** with an `answer` property. After parsing that string, the grader reads only `answer`, which must be a JSON string or `null`. It must not compare the raw JSON string or the outer request body with the spec.

```json
{
  "payload": "{\"answer\":\" Critics \"}",
  "schemaVersion": 1,
  "expectedRevision": 0
}
```

With Q12's spec, the parsed answer above is correct. A missing response row, a payload `{}`, or `{"answer":null}` is an omitted answer and scores zero. A malformed payload or an `answer` of another JSON type is invalid input and must not silently match an accepted answer. See [the shared vectors](answer-spec-v1-vectors.json); vector `response` is normally the extracted string or `null`, while a JSON object represents the parsed assessment payload. Vectors normally have `gradable=true` implicitly. The `{}` vector adds `"gradable":false`; its required `correct:false` is only a placeholder, and consumers must take the ungradable/manual path rather than assign zero.

## Error tags (phase 3a, additive)

Two optional keys tell consumers which error a wrong objective answer reflects. Neither changes grading, and `schema_version` stays `1`.

- `distractors` on a `CHOICE` spec maps a wrong option key to an error code, for example `{"type":"CHOICE","correct":"A","distractors":{"B":"PARAPHRASE_TRAP"}}`. Content refuses a version when a key is not one of its options, when the value is not text, or when a code is not an `ACTIVE` error type (`422`, `details.code = UNKNOWN_ERROR_CODE`). A `FILL` spec has no options, so it cannot carry `distractors`.
- `maxWords` on a `FILL` spec is the word limit used for the `WORD_LIMIT_EXCEEDED` tag. It does not change whether an answer is correct.

A tag is assigned only to a wrong, gradable objective answer. `CHOICE` takes its code from `distractors`. A wrong `FILL` answer is classified in this order: over `maxWords`, then a number in another value or format (`NUMBER_FORMAT`), then a missing or extra trailing `s` (`PLURAL_FORM`), then at most two edits from an accepted answer (`SPELLING`). Anything else has no tag. A correct, omitted or ungradable answer has no tag.

Learner responses never include `distractors` or `maxWords`. Learning builds its responses from explicit fields (`correctAnswer`, `explanation`), not from the raw spec. The shared vectors for tags are in [answer-spec-error-tags-v1-vectors.json](answer-spec-error-tags-v1-vectors.json). Learning and Assessment each implement this tagger and must pass the same vectors.
