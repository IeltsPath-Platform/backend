# `ai-learning-service`

Service chuyên trách **Adaptive Learning và DeepTutor AI Tutor Core** cho nền tảng IELTSPath.

## Tech Stack
- Python 3.11+
- FastAPI
- DeepTutor Engine
- PostgreSQL (`ai_learning_db`)

## Responsibilities
- DeepTutor Mastery Path
- Learner adaptive state & retention state
- Next learning objective calculation
- Tutor session runtime & interaction
- Question-level review & mistake practice

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

