"""Run the study/review tutor through the real Gateway and Compose stub.

Run from the repository root after starting the Java services and the AI Learning
Compose services. The script prints checks only; it never prints tokens or chats.
"""

from __future__ import annotations

import argparse
from datetime import date, timedelta
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
from uuid import uuid4

import httpx

ROOT = Path(__file__).resolve().parents[4]
TUTOR = "/api/ai-learning/tutor"
_COMPOSE = ["docker", "compose"]


def check(response: httpx.Response, expected: int, label: str) -> dict:
    if response.status_code != expected:
        raise RuntimeError(f"{label}: HTTP {response.status_code}, expected {expected}")
    return response.json() if response.content else {}


def register_disposable(client: httpx.Client) -> tuple[str, str]:
    email = f"tutor-e2e-{uuid4().hex}@example.invalid"
    password = secrets.token_urlsafe(24)
    check(client.post("/api/users/register", json={"email": email, "password": password,
                                                    "fullName": "Tutor E2E learner"}), 201, "register learner")
    return email, password


def login(client: httpx.Client, email: str, password: str) -> tuple[dict[str, str], str, str]:
    token = check(client.post("/auth/login", json={"email": email, "password": password}),
                  200, "login")["accessToken"]
    headers = {"Authorization": f"Bearer {token}"}
    user_id = check(client.get("/auth/me", headers=headers), 200, "current user")["id"]
    goal = client.get("/api/users/me/learning-goals/active", headers=headers)
    if goal.status_code == 404:
        goal = client.post("/api/users/me/learning-goals", headers=headers, json={
            "targetBand": 5.5, "examDate": (date.today() + timedelta(days=90)).isoformat(),
            "availableMinutesPerDay": 30,
        })
        goal_id = check(goal, 201, "create active goal")["id"]
    else:
        goal_id = check(goal, 200, "active goal")["id"]
    return headers, user_id, goal_id


def compose_python(service: str, code: str, *args: str, input_text: str | None = None) -> str:
    result = subprocess.run([*_COMPOSE, "exec", "-T", service, "python", "-c", code, *args],
                            cwd=ROOT, input=input_text, text=True, capture_output=True, check=True)
    return result.stdout.strip()


_SNAPSHOT = """import json, os
from pathlib import Path
print(json.dumps({str(p.relative_to('/app')): (p.stat().st_size, p.stat().st_mtime_ns)
                  for p in Path('/app').rglob('*') if p.is_file()}))"""

_EVIDENCE = """import json, os, sys, psycopg2
with psycopg2.connect(os.environ['AI_LEARNING_DATABASE_URL']) as conn:
    with conn.cursor() as cur:
        cur.execute('SELECT source, count(*) FROM mastery_learning_evidence WHERE path_id = %s GROUP BY source',
                    (sys.argv[1],))
        print(json.dumps(dict(cur.fetchall())))"""

_PUBLISH = """import os, sys, pika
connection = pika.BlockingConnection(pika.URLParameters(os.environ['AI_LEARNING_AMQP_URL']))
try:
    channel = connection.channel()
    channel.confirm_delivery()
    channel.basic_publish(exchange='assessment.events', routing_key='assessment.completed.v2',
                          body=sys.stdin.buffer.read(), mandatory=True,
                          properties=pika.BasicProperties(content_type='application/json', delivery_mode=2))
finally:
    connection.close()"""


def evidence(path_id: str) -> dict[str, int]:
    return json.loads(compose_python("ai-learning-api", _EVIDENCE, path_id))


def stream_turn(client: httpx.Client, headers: dict[str, str], session_id: str, body: dict):
    events: list[tuple[str, dict, float]] = []
    keep_alives = 0
    started = time.monotonic()
    with client.stream("POST", f"{TUTOR}/sessions/{session_id}/turns", headers=headers,
                       json=body, timeout=httpx.Timeout(10, read=50)) as response:
        if response.status_code != 200:
            raise RuntimeError(f"tutor turn: HTTP {response.status_code}, expected 200")
        if not response.headers.get("content-type", "").startswith("text/event-stream"):
            raise RuntimeError("tutor turn did not return SSE")
        block: list[str] = []
        for line in response.iter_lines():
            if line.startswith(": keep-alive"):
                keep_alives += 1
            elif line:
                block.append(line)
            elif block:
                kind = next(value[7:] for value in block if value.startswith("event: "))
                payload = json.loads(next(value[6:] for value in block if value.startswith("data: ")))
                if "expectedAnswer" in json.dumps(payload):
                    raise RuntimeError("SSE leaked expectedAnswer")
                events.append((kind, payload, time.monotonic() - started))
                block.clear()
    if not events or events[0][0] != "turn.started" or events[-1][0] != "turn.completed":
        raise RuntimeError("Tutor turn did not start and complete")
    return events, keep_alives


def formal_event(user_id: str, goal_id: str, knowledge_point_id: str) -> dict:
    return {"event_id": str(uuid4()), "event_type": "AssessmentCompleted.v2",
            "occurred_at": "2026-09-24T10:00:00.123456789Z", "source": "assessment-service",
            "data": {"user_id": user_id, "learning_goal_id": goal_id, "attempt_id": str(uuid4()),
                     "result_id": str(uuid4()), "result_version": 1, "assessment_type": "QUIZ",
                     "status": "COMPLETED", "completed_at": "2026-09-24T10:00:00Z",
                     "item_results": [{"item_result_id": str(uuid4()), "question_version_id": str(uuid4()),
                                       "is_correct": True, "score": 1.0, "max_score": 1.0,
                                       "knowledge_point_mappings": [{"knowledge_point_id": knowledge_point_id,
                                                                      "weight": 1.0, "qualitative_judgment": None,
                                                                      "error_type": None}]}]}}


def run(register: bool, gateway: str) -> None:
    before_files = json.loads(compose_python("ai-learning-api", _SNAPSHOT))
    with httpx.Client(base_url=gateway, timeout=30) as client:
        if register:
            credentials = [register_disposable(client), register_disposable(client)]
        else:
            credentials = [(os.environ[f"TUTOR_E2E_USER_{letter}_EMAIL"],
                            os.environ[f"TUTOR_E2E_USER_{letter}_PASSWORD"]) for letter in "AB"]
        a, user_id, goal_id = login(client, *credentials[0])
        b, _other_id, _other_goal = login(client, *credentials[1])
        before_status = check(client.get("/api/ai-learning/status", headers=a), 200, "status before turn")
        if before_status["knowledgePointType"].lower() not in ("memory", "procedure"):
            raise RuntimeError("The active objective must be MEMORY or PROCEDURE for the quiz script")
        session = check(client.post(f"{TUTOR}/sessions", headers=a, json={"title": "Tutor E2E"}),
                        201, "create session")
        session_id = session["sessionId"]
        if client.get(f"{TUTOR}/sessions/{session_id}", headers=b).status_code != 404:
            raise RuntimeError("Learner B could read learner A's session")

        first, keep_alives = stream_turn(client, a, session_id, {"message": "Teach this objective"})
        question = next((payload for kind, payload, _ in first if kind == "question"), None)
        if question is None:
            raise RuntimeError("First turn did not produce a question")
        if first[-1][2] - first[0][2] < 1 or keep_alives < 2:
            raise RuntimeError("SSE was buffered or long-turn keep-alive was absent")
        detail = check(client.get(f"{TUTOR}/sessions/{session_id}", headers=a), 200, "session detail")
        if detail["pendingQuestion"]["questionId"] != question["questionId"]:
            raise RuntimeError("Session detail lost the pending question")
        print("PASS: Gateway auth, session ownership, live SSE and 30-second keep-alive")

        previous_formal = evidence(session["pathId"]).get("assessment_service", 0)
        event = formal_event(user_id, goal_id, question["knowledgePointId"])
        compose_python("ai-learning-consumer", _PUBLISH, input_text=json.dumps(event))
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            if evidence(session["pathId"]).get("assessment_service", 0) > previous_formal:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("The formal assessment event was not applied")
        print("PASS: formal result consumed between tutor turns")

        second, _ = stream_turn(client, a, session_id,
                                {"answer": {"questionId": question["questionId"], "text": "B"}})
        if not any(kind == "grading" for kind, _, _ in second):
            raise RuntimeError("Answer turn did not produce grading")
        after_status = check(client.get("/api/ai-learning/status", headers=a), 200, "status after grade")
        if after_status["revision"] <= before_status["revision"]:
            raise RuntimeError("Mastery revision did not advance")
        sources = evidence(session["pathId"])
        if sources.get("assessment_service", 0) <= previous_formal or sources.get("mastery_path", 0) < 1:
            raise RuntimeError("Tutor grading lost formal or tutor evidence")
        print("PASS: answer graded, status advanced, both evidence sources retained")

    after_files = json.loads(compose_python("ai-learning-api", _SNAPSHOT))
    if before_files != after_files:
        raise RuntimeError("The API container wrote files under /app during the run")
    print("PASS: no container file writes")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gateway", default="http://127.0.0.1:8080")
    parser.add_argument("--register-disposable", action="store_true",
                        help="Create two local test learners; their records remain in user_db")
    args = parser.parse_args()
    try:
        run(args.register_disposable, args.gateway)
    except (KeyError, RuntimeError, httpx.HTTPError, subprocess.CalledProcessError) as error:
        print(f"FAIL: {type(error).__name__}: {error}", file=sys.stderr)
        raise SystemExit(1) from None
