# Tutor study/review verification — 2026-09-26

## Scope

Plan 2249, phases 1–4. Tested through the local Gateway with the real User and Content services, AI Learning API
and consumer in Compose, PostgreSQL, RabbitMQ, and the Compose LLM stub in `script` mode. The test used two
disposable local learner accounts and active goals; the accounts remain in the local `user_db`. No token, password,
conversation, or provider key is recorded here.

## Gateway E2E

Ran `python tests/e2e/tutor_e2e.py --register-disposable` from `services/ai-learning-service` with the script
LLM stub and a 45-second model timeout. The script reported:

| Check | Result |
| --- | --- |
| External JWT login through Gateway, active path session creation, learner B denied A's session | Pass |
| First turn returns `question` through SSE; `turn.started` arrives before `turn.completed` | Pass |
| Long first turn sends keep-alives past 30 seconds without Gateway buffering or timeout | Pass |
| `AssessmentCompleted.v2` published through RabbitMQ between turns and applied to the path | Pass |
| Second turn grades the card; status revision advances; formal and tutor evidence both remain | Pass |
| No new or modified files under `/app` in the API container during the run | Pass |

The test script compares file sizes and modification times before and after, and counts evidence by source in the
AI Learning database. It does not invoke a real LLM.

## Automated gates

- Python suite with isolated disposable PostgreSQL and RabbitMQ after the review fixes: **341 passed, 0 failed,
  0 skipped**, plus 71 subtests (`python -m pytest tests -rs -q -p no:cacheprovider
  --basetemp=.pytest-tmp-codex-260926-final`).
- Python `compileall` for `app`, `tests`, and `main.py`: pass.
- `docker compose config --quiet`: pass.
- `git diff --check`: pass (Git printed only Windows line-ending conversion warnings).
- `graphify update .`: pass after the final source fixes (47,082 nodes rebuilt).

The Gateway route and security configuration were not changed, so the Maven Gateway test gate was not needed.
The review found and resolved objective bypass, archive/turn races, early SSE disconnect, pre-question answer
leakage, and startup recovery failure. A read-only re-review found no remaining concrete issue in those areas.
