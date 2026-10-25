# Prerequisite diagnosis v1

When the blocking review of knowledge point (KP) B keeps failing, the cause is often a KP B builds on. Learning Service
then opens a diagnosis: short probes on B's prerequisites, a result matrix, and a conclusion that names the KP to review
first. FE: the diagnosis screen (probes to answer, result matrix, conclusion) and the review screen of B ("waiting for the
base KP").

## When a diagnosis opens

For a **blocking** review of B, at the first of:

1. its **second failed set** (the set after the theory step also failed);
2. **no unseen set left** for B (the review ends `SKIPPED`; Content is told through a content gap signal, even when no
   diagnosis follows).

and only if B has at least one active prerequisite (Content, `POST /internal/learning-content/knowledge-points/prerequisites`).
A review gets at most one diagnosis. A review that itself came from a diagnosis (`trigger_kind = DIAGNOSTIC`) is never
diagnosed again, which keeps the walk back finite. Optional reviews (review basket, free practice test) never open one.

## How it runs

- **Probes.** Each direct prerequisite of B gets one probe: a practice set of at least 3 questions whose answers the learner
  has not seen (not revealed in Practice, a review or another probe). With no such set the probe is `NO_SET`: the result is
  *unknown*, never a gap.
- **Pass mark.** A probe passes at 70% correct, like a review set.
- **Walk back.** A failed probe at depth < 3 opens probes on its own prerequisites (depth + 1), skipping KPs already probed
  in the session and B itself. At most 6 probes per session.
- **Conclusion**, once no probe is `OPEN`:
  - a *gap* is a failed probe none of whose probed prerequisites failed (so a probe without prerequisites, one at depth 3,
    or one whose prerequisites passed or are unknown);
  - no failed probe → the conclusion is B itself (`rootIsGap = true`): the base is solid, B stays under its own review.
- **Actions.**
  - each gap KP gets a blocking review with `trigger_kind = DIAGNOSTIC` (a pending optional review of it is made blocking);
  - B's review stays `PENDING` but gives no set until the gap reviews are `DONE` or `SKIPPED`; if B was already `SKIPPED`
    (no set left), the gap reviews are still created and nothing waits;
  - the review of a gap KP uses a completed lesson that teaches it, else B's lesson.
- **Evidence.** Probe answers are mastery evidence of the KPs their questions map to (`source = diagnostic_probe`, also in
  the question event log), so the mastery of the base KPs updates. Answered probe sets count as seen for Practice and reviews.
- **No dead end.** A probe never blocks beyond the review that opened it, `NO_SET` ends a probe without an answer, and the
  budget is 6 probes.

## Review response additions

`GET /api/learning/reviews/{reviewId}` (and the review-basket start) gains:

| Field | Meaning |
| --- | --- |
| `state` | `ACTIVE`; `DIAGNOSING` while the session is `PROBING` (no set is given); `WAITING_ON_PREREQUISITES` while the gap reviews are open (no set is given). |
| `diagnosticSessionId` | The diagnosis opened for this review, or `null`. Set on a `SKIPPED` review too. |
| `waitingOnReviewIds` | The gap reviews still `PENDING` that this review waits for. |

`GET /api/learning/report`: `action.type = DIAGNOSING` (with the review id of B) while B's session is `PROBING`.

## `GET /api/learning/diagnostics/{sessionId}`

Role `CUSTOMER`; 404 for another learner's session.

```json
{
  "sessionId": "…", "rootReviewId": "…", "rootKnowledgePointId": "…", "status": "CONCLUDED",
  "probes": [{
    "probeId": "…", "knowledgePoint": { "knowledgePointId": "…", "code": "R40_SKIM", "name": "…" },
    "parentKnowledgePointId": "…", "depth": 1, "status": "ANSWERED",
    "correct": 1, "total": 3, "result": "FAILED"
  }],
  "conclusion": [{ "knowledgePointId": "…", "code": "R40_SKIM", "name": "…" }],
  "rootIsGap": false,
  "waitingOnReviewIds": ["…"]
}
```

- `probes` is the result matrix, in the order the probes were opened. `status`: `OPEN`, `ANSWERED`, `NO_SET`. `result`:
  `PASSED`, `FAILED`, `UNKNOWN` (`NO_SET`), `null` while `OPEN`.
- `conclusion` is empty while `status = PROBING`. `rootIsGap = true` when the conclusion is B itself.

## `GET /api/learning/diagnostics/{sessionId}/probes/{probeId}`

The probe's questions: `sessionId`, `probeId`, `knowledgePointId`, `depth`, `status`, and for an `OPEN` probe `passage`,
`audio` (`mediaUrl`, `durationSeconds`) and `questions` (`questionVersionId`, `sortOrder`, `stem`, `options`, `hint`; no
answer spec, explanation or transcript). A probe whose set the learner meanwhile saw elsewhere gets another unseen set, or
becomes `NO_SET`. Other statuses return no questions.

## `POST /api/learning/diagnostics/{sessionId}/probes/{probeId}/submissions`

Body as `SubmitExerciseRequest`: `{ "requestId": "…", "answers": [{ "questionVersionId": "…", "answer": …, "durationMs": … }] }`.

```json
{
  "probeId": "…", "passed": false, "correct": 1, "total": 3,
  "results": [{ "questionVersionId": "…", "correct": true, "correctAnswer": "A", "explanation": "…", "hint": null }],
  "transcript": null, "sessionStatus": "PROBING", "conclusionKnowledgePointIds": []
}
```

Solutions and the audio transcript are shown whatever the result. Sending the same `requestId` again returns the same body
and writes nothing. Errors: `404 NOT_FOUND`; `409 PROBE_CLOSED` (already answered with another `requestId`, `NO_SET`, or its
set was seen elsewhere); `422 UNGRADABLE_EXERCISE`.

An incorrect probe answer with a recognized active error code may include `error: {code, name, fixHint}` in its result.
Unknown codes or a failed Content lookup omit the field; the diagnostic submission still succeeds. Correct and untagged
answers omit `error`.

## Content gap signals

`content_gap_signals` counts, per KP, the reviews that ran out of unseen sets. It is an internal table for the content team;
there is no public API in this version.
