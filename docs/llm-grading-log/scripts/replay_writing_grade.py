"""Rebuild the grading prompt for one stored submission and send it to the learning LLM from .env.

The template is chosen with REPLAY_TEMPLATE (v2 by default, or v3). The prompt follows EssayGrader: the rubric's system
prompt comes before its criteria, error codes are listed only for the submission's task and show their model definition
when they have one, and the knowledge-point extension is appended when the question has knowledge points (at most four, by code or id). Saves the raw reply
and checks it the way EssayGrader.parse decides INVALID_GRADE. Never prints the API key.
Usage: [REPLAY_TEMPLATE=v3] replay_writing_grade.py <submission-id-prefix> [model-override]
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import pg8000.native

BACKEND = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
TEMPLATE = os.environ.get("REPLAY_TEMPLATE", "v2")
if TEMPLATE not in ("v2", "v3"):
    sys.exit("REPLAY_TEMPLATE must be v2 or v3")
env_text = (BACKEND / ".env").read_text(encoding="utf-8")


def env(key):
    if key in os.environ:  # a value set for this run wins over .env, so a key never has to be written to the file
        return os.environ[key]
    m = re.search(rf"^{key}=(.*)$", env_text, re.M)
    return m.group(1).strip().strip('"') if m else ""


def db(name, password_key):
    return pg8000.native.Connection("postgres", host="localhost", port=int(os.environ.get("REPLAY_DB_PORT", "5432")), database=name, password=env(password_key))


prefix = sys.argv[1]
# The question knowledge points of the workbook essays (E01 is the prompt of PT-WRITING-07, part 1).
SHEET_POINTS = {"E01": ["DEMO_READING_W1_CHART", "DEMO_WRITING_TREND_LANGUAGE"]}
if prefix.startswith("sheet:"):
    # A test essay from the Bai_test sheet of the grading log workbook (for example sheet:E01), no submission needed.
    import openpyxl
    workbook = openpyxl.load_workbook(next(OUT.parent.glob("llm-writing-grading-prompt-iteration-log-*.xlsx")), data_only=True)
    row = next(r for r in workbook["Bai_test"].iter_rows(min_row=4, values_only=True) if r[0] == prefix[6:])
    sid, essay = prefix, row[5]
    snap = {"task": row[1], "stem": row[3], "minWords": 150 if row[1] == "TASK_1" else 250, "chartFacts": row[4],
            "kpCodes": SHEET_POINTS.get(prefix[6:], [])}
elif prefix.startswith("file:"):
    # A calibration essay kept as JSON (task, stem, chartFacts, essay), for essays that are not in the workbook.
    item = json.loads(Path(prefix[5:]).read_text(encoding="utf-8"))
    sid, essay = Path(prefix[5:]).stem, item["essay"]
    snap = {"task": item["task"], "stem": item["stem"], "minWords": 150 if item["task"] == "TASK_1" else 250,
            "chartFacts": item.get("chartFacts"), "kpCodes": item.get("knowledgePointCodes", [])}
    prefix = "file-" + sid
else:
    learning = db("learning_db", "LEARNING_DB_PASSWORD")
    sid, essay, snapshot = learning.run(
        "SELECT id, essay_text, prompt_snapshot FROM lesson_writing_submissions WHERE CAST(id AS TEXT) LIKE :p",
        p=prefix + "%")[0]
    learning.close()
    snap = snapshot if isinstance(snapshot, dict) else json.loads(snapshot)
task_one = snap.get("task") == "TASK_1"

content = db(os.environ.get("REPLAY_CONTENT_DB", "content_db"), "CONTENT_DB_PASSWORD")
rubric_id, rubric_system = content.run(
    "SELECT id, system_prompt FROM rubrics WHERE task = :t AND status = :s AND code LIKE 'IELTS_WRITING%'",
    t=snap.get("task"), s=os.environ.get("REPLAY_RUBRIC_STATUS", "ACTIVE"))[0]
criteria = content.run("SELECT id, code, name, description FROM rubric_criteria WHERE rubric_id = :r ORDER BY sort_order",
                       r=rubric_id)
criterion_text = []
for cid, code, name, description in criteria:
    bands = content.run("SELECT band, descriptor FROM rubric_band_descriptors WHERE criterion_id = :c ORDER BY band", c=cid)
    lines = [f"  Band {format(b.normalize(), 'f')}: {d}" for b, d in bands]
    criterion_text.append(f"{code} ({name}): {description}" + ("\n" + "\n".join(lines) if lines else ""))
# The judgments block holds only the knowledge points of the question, at most four, as EssayKnowledgeJudge does.
if snap.get("kpCodes"):
    points = content.run("SELECT code, name, description FROM knowledge_points WHERE code = ANY(:codes) AND status = 'ACTIVE'",
                         codes=snap["kpCodes"])
else:
    points = content.run("SELECT code, name, description FROM knowledge_points WHERE id = ANY(:ids) AND status = 'ACTIVE'",
                         ids=[__import__("uuid").UUID(i) for i in snap.get("knowledgePointIds") or []])
kp_lines = [f"- {k} \u2014 {n or k}" + (f": {d}" if d and d.strip() else "") for k, n, d in points][:4]
errors = content.run("SELECT code, name, fix_hint, model_definition, writing_tasks FROM error_types "
                     "WHERE status = 'ACTIVE' AND 'WRITING' = ANY(skills) ORDER BY code")
content.close()
# Same rule as ErrorType.appliesToWritingTask: no task list means both tasks.
error_codes = "\n".join(f"{c} \u2014 {n}: {d or h}" for c, n, h, d, tasks in errors
                        if not tasks or snap.get("task") in tasks)

template = (BACKEND / f"docs/contracts/essay-prompt-template-{TEMPLATE}.txt").read_text(encoding="utf-8")
template = re.sub(r"\A(?:#[^\r\n]*\r?\n)+\s*", "", template)
drop, keep = ("TASK_2", "TASK_1") if task_one else ("TASK_1", "TASK_2")
template = re.sub(rf"(?s)\[\[{drop}\]\].*?\[\[/{drop}\]\]\r?\n*", "", template)
template = re.sub(rf"\[\[/?{keep}\]\]\r?\n?", "", template)
# The rubric's own band instructions come before its criteria, as in EssayGrader.systemPrompt.
criteria_block = "\n\n".join(criterion_text)
if rubric_system and rubric_system.strip():
    criteria_block = rubric_system.strip() + "\n\n" + criteria_block
system = (template.replace("{{task}}", "Academic Writing Task 1" if task_one else "Writing Task 2")
          .replace("{{criteria}}", criteria_block).replace("{{error_codes}}", error_codes or "(none)"))
if kp_lines:
    kp_template = (BACKEND / "services/learning-service/src/main/resources/essay-kp-judgment-v3.txt").read_text(encoding="utf-8")
    kp_template = re.sub(r"\A(?:#[^\r\n]*\r?\n)+\s*", "", kp_template)
    system += "\n\n" + kp_template.replace("{{knowledge_points}}", "\n".join(kp_lines))
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
print(f"submission {sid} template={TEMPLATE} task1={task_one} model={model} system_chars={len(system)} user_chars={len(user)} question_points={len(kp_lines)}")
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
(OUT / f"replay-{prefix.replace(':', '-')}-{TEMPLATE}-{model}.json").write_text(reply, encoding="utf-8")
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
