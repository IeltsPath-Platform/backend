"""Rebuild the grading prompt for one stored submission and send it to the learning LLM from .env.

The template is chosen with REPLAY_TEMPLATE (v2 by default, or v3). The prompt follows EssayGrader: the rubric's system
prompt comes before its criteria, error codes are listed only for the submission's task and show their model definition
when they have one, and the knowledge-point extension is appended when the rubric has sub-points. Saves the raw reply
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
if prefix.startswith("sheet:"):
    # A test essay from the Bai_test sheet of the grading log workbook (for example sheet:E01), no submission needed.
    import openpyxl
    workbook = openpyxl.load_workbook(next(OUT.parent.glob("llm-writing-grading-prompt-iteration-log-*.xlsx")), data_only=True)
    row = next(r for r in workbook["Bai_test"].iter_rows(min_row=4, values_only=True) if r[0] == prefix[6:])
    sid, essay = prefix, row[5]
    snap = {"task": row[1], "stem": row[3], "minWords": 150 if row[1] == "TASK_1" else 250, "chartFacts": row[4]}
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
    "SELECT id, system_prompt FROM rubrics WHERE task = :t AND status = 'ACTIVE' AND code LIKE 'IELTS_WRITING%'",
    t=snap.get("task"))[0]
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
if kp_blocks:
    kp_file = "essay-kp-judgment-v3.txt" if TEMPLATE == "v3" else "essay-kp-judgment-v2.txt"
    kp_template = (BACKEND / "services/learning-service/src/main/resources" / kp_file).read_text(encoding="utf-8")
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
print(f"submission {sid} template={TEMPLATE} task1={task_one} model={model} system_chars={len(system)} user_chars={len(user)} kp_criteria={len(kp_blocks)}")
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
