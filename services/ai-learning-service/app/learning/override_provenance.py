"""Provenance of DeepTutor learner mastery overrides written by IELTSPath.

DeepTutor v1.6.9 reports every override as ``learner`` and its override model
has no provenance field; the pinned submodule is the unmodified upstream
release. IELTSPath therefore marks its own overrides in the note and relabels
them in the learner-facing summary:

* ``placement:{attempt_id}:v{result_version}``: tested out by a placement result;
* ``retired:content``: no longer in the goal's curriculum, kept for its history.

Any other override is the learner's own claim and stays ``learner``.
"""

from __future__ import annotations

from typing import Any

from deeptutor.learning.policy import mastery_source as deeptutor_source

PLACEMENT_NOTE_PREFIX = "placement:"
RETIRED_NOTE = "retired:content"

_SOURCES = ((PLACEMENT_NOTE_PREFIX, "placement"), (RETIRED_NOTE, "retired"))


def override_source(override: Any) -> str:
    """``placement``, ``retired`` or ``learner`` for an override; ``""`` when there is none."""
    if override is None:
        return ""
    note = str(getattr(override, "note", ""))
    for prefix, source in _SOURCES:
        if note.startswith(prefix):
            return source
    return "learner"


def is_placement_override(override: Any) -> bool:
    return override_source(override) == "placement"


def is_retired_override(override: Any) -> bool:
    return override_source(override) == "retired"


def mastery_source(progress: Any, kp: Any) -> str:
    """DeepTutor's ``system``/``learner`` provenance, with IELTSPath overrides relabelled."""
    source = deeptutor_source(progress, kp)
    if source == "learner":
        return override_source(progress.learner_mastery_overrides.get(kp.id))
    return source


def with_placement_provenance(summary: dict[str, Any], progress: Any) -> dict[str, Any]:
    """Relabel ``map_summary`` entries whose override IELTSPath wrote (placement or retired)."""
    for module in summary.get("modules", []):
        for entry in module.get("knowledge_points", []):
            if entry.get("mastery_source") == "learner":
                entry["mastery_source"] = override_source(progress.learner_mastery_overrides.get(entry["id"]))
    return summary
