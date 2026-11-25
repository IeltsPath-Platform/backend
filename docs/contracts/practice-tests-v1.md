# Free practice tests v1

The practice-test catalog (`POST /api/content/practice-tests/search`) includes `requiredFeatureKey` on each card. A non-null value marks a Premium test; the catalog remains visible to all learners.

For a tagged test, learner routes require active Premium before returning prompt content:

- `GET /api/content/sections/{sectionId}` (Reading passage)
- `GET /api/content/practice-tests/{packageVersionId}/sections/{sectionId}/writing-task`
- `GET /api/content/practice-tests/{packageVersionId}/sections/{sectionId}/speaking-task`

A learner without Premium receives HTTP 403 with `details.code = PREMIUM_REQUIRED` and the message `Đề này dành cho tài khoản Premium. Nâng cấp để luyện đề này.` If Access cannot confirm Premium, the route returns HTTP 503 with `details.code = ENTITLEMENT_UNAVAILABLE` and `Chưa kiểm tra được gói Premium. Vui lòng thử lại sau.` Free tests do not call Access. CONTENT_AUTHOR and EXAMINER retain their existing access and bypass the learner entitlement lookup.

The task routes return only the public task content. Answer specifications, grading explanations and transcripts remain governed by their existing submission and result contracts.