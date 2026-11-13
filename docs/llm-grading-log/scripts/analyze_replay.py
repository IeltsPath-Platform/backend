"""Summarise one saved raw grading reply: bands, corrections, data checks, the overview fix, comment lengths."""
import json
import re
import sys
from pathlib import Path

reply = Path(sys.argv[1]).read_text(encoding="utf-8")
g = json.loads(reply[reply.find("{"):reply.rfind("}") + 1])
corrections = g.get("corrections") or []
codes = {}
for c in corrections:
    codes[c.get("errorCode")] = codes.get(c.get("errorCode"), 0) + 1
print("bands:", [(c.get("code"), c.get("band")) for c in g.get("criteria", [])])
print("comment lengths:", [len(c.get("comment") or "") for c in g.get("criteria", [])])
print(f"corrections={len(corrections)} by code={codes}")
print("SUGGESTION certainty:", sum(1 for c in corrections if c.get("certainty") == "SUGGESTION"),
      "| empty suggestion:", sum(1 for c in corrections if (c.get("suggestion") or "") == ""))
checks = g.get("taskChecks") or {}
data = checks.get("dataChecks") or []
print(f"dataChecks={len(data)} verdicts={[d.get('verdict') for d in data]} overviewAccurate={checks.get('overviewAccurate')}")
for c in corrections:
    if "young people" in (c.get("excerpt") or "") or "conclusion" in (c.get("excerpt") or "").lower():
        print("  ", c.get("errorCode"), "|", c.get("excerpt"), "=>", repr(c.get("suggestion")), "|", (c.get("reason") or "")[:120])
print("priorities:", [p.get("title") for p in g.get("priorities") or []])
