# `ai-learning-service`

Service chuyên trách **Adaptive Learning và DeepTutor AI Tutor Core** cho nền tảng IELTSPath.

## Tech Stack
- Python 3.11+
- FastAPI
- DeepTutor Engine
- PostgreSQL (`ai_learning_db`)

## Phase 1 scope
- Goal-bound DeepTutor Mastery Path bootstrap from User Service and Content Service.
- Learner adaptive state persisted in PostgreSQL `mastery_paths.state_json`.
- Safe progress, status, and path-map reads derived through DeepTutor v1.6.9 policy.
- Internal JWT authentication using only the validated Authorization bearer token.

Tutor sessions, formal assessment ingestion, RabbitMQ consumers, question-level
review APIs, and mistake practice are outside this phase.

## Build the service image

Build from the repository root so Docker can install the pinned local DeepTutor
submodule:

```powershell
docker build -f services/ai-learning-service/Dockerfile -t ieltspath-ai-learning .
```

The image installs the service requirements and then installs DeepTutor from
`third_party/deeptutor`; it does not fetch a separate DeepTutor release at
runtime. The service configuration uses `AI_LEARNING_INTERNAL_JWT_SECRET`,
`AI_LEARNING_DATABASE_URL`, `AI_LEARNING_USER_SERVICE_BASE_URL`, and
`AI_LEARNING_CONTENT_SERVICE_BASE_URL`. The JWT secret must be Base64 encoded
and decode to at least 32 bytes. Supply secret values through runtime
configuration; do not put them in this file or the image.

The PostgreSQL constraint for goal-bound mastery paths is defined in
`migrations/V1__one_mastery_path_per_learning_goal.sql`. Apply it after the V5
`mastery_paths` table exists. The script stops if the database already contains
more than one path for a non-null `(user_id, learning_goal_id)` pair; reconcile
those rows before retrying. `AI_LEARNING_TEST_DATABASE_URL` enables the
concurrent-insert integration test in `tests/test_mastery_path_goal_uniqueness.py`.

The service uses DeepTutor's synchronous `LearningStore` interface. Its
PostgreSQL adapter locks one aggregate row with `SELECT ... FOR UPDATE`; nested
DeepTutor transactions for bootstrap join one PostgreSQL transaction so path
ownership, initial state, curriculum, revision, and events commit together.
Apply the V5 mastery schema and the uniqueness migration before enabling the
path endpoints.
The active-goal and curriculum contracts also depend on User Service
`V4__enforce_one_active_learning_goal_per_user.sql` and Content Service
`V2__add_knowledge_point_learning_type.sql` being applied to their own databases.

Curriculum topics follow Content Service's sibling `sortOrder` in tree preorder.
Knowledge Points are ordered by `createdAt` ascending, with UUID as a stable
tie-breaker; Content Service does not currently expose an editorial learning
order for Knowledge Points.

The public Phase 1 routes are:

- `POST /api/ai-learning/paths`
- `GET /api/ai-learning/progress`
- `GET /api/ai-learning/status`
- `GET /api/ai-learning/paths/{pathId}/map`

