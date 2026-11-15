"""Facts of tonight's h2 Writing gradings, as JSON for the tracking workbook."""
import json
import re
from pathlib import Path

import pg8000.native

ENV = Path(__file__).resolve().parents[3] / ".env"
OUT = Path(__file__).resolve().parent / "runs.json"
USER_ID = "0bc0fab3-b78f-4180-b8cf-8e877a017f1f"
text = ENV.read_text(encoding="utf-8")
password = re.search(r"^LEARNING_DB_PASSWORD=(.*)$", text, re.M).group(1).strip()
conn = pg8000.native.Connection("postgres", host="localhost", port=5432, database="learning_db", password=password)
rows = conn.run("""SELECT id, submitted_at, grading_started_at, graded_at, status, failure_code, prompt_version,
        word_count, essay_text, result
    FROM lesson_writing_submissions WHERE user_id = :u AND submitted_at >= '2026-11-12 21:00:00+07'
    ORDER BY submitted_at""", u=USER_ID)
conn.close()
runs = []
for sid, sub, started, graded, status, failure, pv, words, essay, result in rows:
    g = (result if isinstance(result, dict) else json.loads(result)) if result else {}
    corrections = g.get("corrections") or []
    checks = g.get("task1Checks") or {}
    data = checks.get("dataChecks") or []
    runs.append({
        "id": str(sid), "submitted": str(sub)[:19], "seconds": (graded - started).total_seconds() if graded and started else None,
        "status": status, "failure": failure, "promptVersion": pv, "words": words,
        "hasVietnameseTail": "tÃ´i Ä‘Æ°a" in (essay or ""), "model": g.get("model"),
        "bands": {c["code"]: float(c["band"]) for c in g.get("criteria", [])},
        "corrections": len(corrections), "withReason": sum(1 for c in corrections if c.get("reason")),
        "falseSuggestions": sum(1 for c in corrections if c.get("suggestion") is False),
        "dataChecks": len(data), "dataWrong": sum(1 for d in data if d.get("verdict") == "WRONG"),
        "overviewAccurate": checks.get("overviewAccurate"), "hasTask1Checks": bool(checks),
        "maxCommentLen": max([len(" ".join(c.get("improvements") or [])) for c in g.get("criteria", [])] or [0]),
    })
OUT.write_text(json.dumps(runs, ensure_ascii=False, indent=1), encoding="utf-8")
for r in runs:
    print({k: v for k, v in r.items() if k != "id"} | {"id": r["id"][:8]})
