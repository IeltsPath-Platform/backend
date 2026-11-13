"""Rebuild the template-v2 grading prompt for one stored submission and send it to the learning LLM from .env.

Approximates EssayGrader.systemPrompt/userPrompt without the appended knowledge-point section. Saves the raw reply
and checks it the way EssayGrader.parse decides INVALID_GRADE. Never prints the API key.
Usage: replay_writing_grade.py <submission-id-prefix> [model-override]
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import pg8000.native

BACKEND = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
env_text = (BACKEND / ".env").read_text(encoding="utf-8")


def env(key):
    m = re.search(rf"^{key}=(.*)$", env_text, re.M)
    return m.group(1).strip().strip('"') if m else ""


def db(name, password_key):
    return pg8000.native.Connection("postgres", host="localhost", port=5432, database=name, password=env(password_key))


prefix = sys.argv[1]
learning = db("learning_db", "LEARNING_DB_PASSWORD")
sid, essay, snapshot = learning.run(
    "SELECT id, essay_text, prompt_snapshot FROM lesson_writing_submissions WHERE CAST(id AS TEXT) LIKE :p",
    p=prefix + "%")[0]
learning.close()
snap = snapshot if isinstance(snapshot, dict) else json.loads(snapshot)
task_one = snap.get("task") == "TASK_1"

content = db("content_db", "CONTENT_DB_PASSWORD")
rubric_id = content.run("SELECT id FROM rubrics WHERE task = :t AND status = 'ACTIVE' AND code LIKE 'IELTS_WRITING%'",
                        t=snap.get("task"))[0][0]
criteria = content.run("SELECT id, code, name, description FROM rubric_criteria WHERE rubric_id = :r ORDER BY sort_order",
                       r=rubric_id)
criterion_text = []
for cid, code, name, description in criteria:
    bands = content.run("SELECT band, descriptor FROM rubric_band_descriptors WHERE criterion_id = :c ORDER BY band", c=cid)
    lines = [f"  Band {format(b.normalize(), 'f')}: {d}" for b, d in bands]
    criterion_text.append(f"{code} ({name}): {description}" + ("\n" + "\n".join(lines) if lines else ""))
subs = content.run("""SELECT s.criterion_code, kp.code, kp.name, kp.description
    FROM writing_criterion_sub_knowledge_points s JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
    JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id
    WHERE s.task = :task AND kp.status = 'ACTIVE' AND kp.skill = 'WRITING' AND t.status = 'ACTIVE' AND c.status = 'ACTIVE'
    ORDER BY s.criterion_code, s.sort_order""", task=snap.get("task"))
kp_blocks = []
for _, code, name, _description in criteria:
    entries = [f"- {k} \u2014 {n or k}" + (f": {d}" if d and d.strip() else "") for cc, k, n, d in subs if cc == code]
    if entries:
        kp_blocks.append(f"{code} ({name}):\n" + "\n".join(entries))
errors = content.run("SELECT code, name, fix_hint FROM error_types WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills) "
                     "ORDER BY code")
content.close()
error_codes = "\n".join(f"{c} \u2014 {n}: {h}" for c, n, h in errors)

template = (BACKEND / "docs/contracts/essay-prompt-template-v2.txt").read_text(encoding="utf-8")
template = re.sub(r"\A(?:#[^\r\n]*\r?\n)+\s*", "", template)
drop, keep = ("TASK_2", "TASK_1") if task_one else ("TASK_1", "TASK_2")
template = re.sub(rf"(?s)\[\[{drop}\]\].*?\[\[/{drop}\]\]\r?\n*", "", template)
template = re.sub(rf"\[\[/?{keep}\]\]\r?\n?", "", template)
system = (template.replace("{{task}}", "Academic Writing Task 1" if task_one else "Writing Task 2")
          .replace("{{criteria}}", "\n\n".join(criterion_text)).replace("{{error_codes}}", error_codes or "(none)"))
if kp_blocks:
    kp_template = (BACKEND / "services/learning-service/src/main/resources/essay-kp-judgment-v2.txt").read_text(encoding="utf-8")
    kp_template = re.sub(r"\A(?:#[^\r\n]*\r?\n)+\s*", "", kp_template)
    system += "\n\n" + kp_template.replace("{{knowledge_points}}", "\n".join(kp_blocks))
user = (f"Question:\n{snap.get('stem') or ''}\n\nMinimum words: {snap.get('minWords') or ''}\n"
        f"Word count of the essay: {len(essay.split())}\n\n")
if task_one:
    user += f"<chart_facts>\n{snap.get('chartFacts') or ''}\n</chart_facts>\n\n"
user += f"<essay>\n{essay}\n</essay>"

model = sys.argv[2] if len(sys.argv) > 2 else env("LEARNING_LLM_MODEL")
body = {"model": model, "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
        "max_tokens": int(__import__("os").environ.get("REPLAY_MAX_TOKENS", "8192")), "temperature": 0.0, "response_format": {"type": "json_object"}}
if env("LEARNING_LLM_REASONING_EFFORT"):
    body["reasoning_effort"] = env("LEARNING_LLM_REASONING_EFFORT")
request = urllib.request.Request(env("LEARNING_LLM_BASE_URL").rstrip("/") + "/chat/completions",
                                 data=json.dumps(body).encode(),
                                 headers={"Authorization": "Bearer " + env("LEARNING_LLM_API_KEY"),
                                          "Content-Type": "application/json"})
print(f"submission {sid} task1={task_one} model={model} system_chars={len(system)} user_chars={len(user)} kp_criteria={len(kp_blocks)}")
started = time.time()
try:
    with urllib.request.urlopen(request, timeout=120) as response:
        data = json.load(response)
except urllib.error.HTTPError as error:
    print("HTTP", error.code, error.read().decode("utf-8", "replace")[:300])
    sys.exit(1)
elapsed = time.time() - started
choice = data["choices"][0]
reply = choice["message"].get("content") or ""
(OUT / f"replay-{prefix}-{model}.json").write_text(reply, encoding="utf-8")
print(f"seconds={elapsed:.1f} finish_reason={choice.get('finish_reason')} usage={json.dumps(data.get('usage'))}")

# The checks EssayGrader.parse applies before it accepts a grade.
start, end = reply.find("{"), reply.rfind("}")
try:
    root = json.loads(reply[start:end + 1])
except Exception as exception:
    print("INVALID_GRADE: reply is not one JSON object:", exception)
    sys.exit(0)
expected = ["TA", "CC", "LR", "GRA"] if task_one else ["TR", "CC", "LR", "GRA"]
problems = []
items = root.get("criteria")
if not isinstance(items, list):
    problems.append(f"criteria is {type(items).__name__}")
else:
    seen = []
    for item in items:
        code, band = item.get("code"), item.get("band")
        if code not in expected or code in seen:
            problems.append(f"criterion code {code!r} unexpected or repeated")
        if not isinstance(band, (int, float)) or isinstance(band, bool):
            problems.append(f"{code} band is {band!r} ({type(band).__name__}), not a number")
        elif band < 0 or band > 9 or (band * 2) != int(band * 2):
            problems.append(f"{code} band {band} is off the 0-9 half-band grid")
        seen.append(code)
    if sorted(seen) != sorted(expected):
        problems.append(f"criteria codes {seen} != {expected}")
print("parse problems:", problems or "none")
print("top-level keys:", sorted(root.keys()))
print("corrections:", len(root.get("corrections") or []))
