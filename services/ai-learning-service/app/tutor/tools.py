"""Tutor tools for study, path ordering and profile updates on the learner's own path.

The tool contracts follow the mastery tools of DeepTutor v1.6.9 (``capabilities/mastery/tools.py``, Apache-2.0):
posing a question registers its expected answer server-side and ends the turn, grading is deterministic against
that stored answer, and concept/design objectives are judged qualitatively. The path id always comes from the
learner's session, never from the model. Tool results go to the model; events go to the learner and never carry
an expected answer.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any
from uuid import uuid4

from app.application.path_reorder import ReorderRejected, reorder_path
from app.mastery.models import InteractionStatus, KnowledgeType, PendingOption, PendingQuestion
from app.mastery.pending import canonical_labels, public_pending_question, resolve_answer
from app.mastery.policy import (
    display_mastery, find_knowledge_point, is_mastered, map_summary, next_objective,
)
from app.mastery.scheduler import SpacedRepetitionScheduler
from app.mastery.service import LearningService, MasteryInteractionError
from app.mastery.store import LearningStoreError

QUESTION_TYPES = ("short", "choice", "open")
_QUIZ_TYPES = frozenset({KnowledgeType.MEMORY, KnowledgeType.PROCEDURE})
_MAX_TEXT = 4000
_PROFILE_FIELDS = ("prior_knowledge", "target_level", "time_budget", "preferences", "notes")
_ONLY_REORDER = "Only reordering is allowed; content cannot be added, removed or moved between modules."
_REORDER_MESSAGES = {
    "empty": "Provide module_ids and/or knowledge_points to reorder the path.",
    "malformed": "Use arrays of module ids and knowledge point ids from path_outline.",
    "duplicate_module": "Each module may appear only once in an ordering request.",
    "unknown_module": "A module id is not in this path; call path_outline for current ids.",
    "missing_module": "Include every module id when changing module order. " + _ONLY_REORDER,
    "duplicate_knowledge_point": "Each knowledge point may appear only once in its module.",
    "unknown_knowledge_point": "A knowledge point id is not in this path. " + _ONLY_REORDER,
    "moved_knowledge_point": "A knowledge point belongs to another module. " + _ONLY_REORDER,
    "missing_knowledge_point": "Include every knowledge point in each changed module. " + _ONLY_REORDER,
}

TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {"type": "function", "function": {
        "name": "mastery_status",
        "description": "The learner's current objective, any question awaiting an answer, due reviews and progress. "
                       "Call it again after grading to see what comes next.",
        "parameters": {"type": "object", "properties": {}},
    }},
    {"type": "function", "function": {
        "name": "mastery_quiz",
        "description": "Pose one question for a MEMORY or PROCEDURE objective. Registers the expected answer "
                       "server-side and shows the question to the learner on its own card. THE TURN ENDS HERE: "
                       "the server supplies the learner-facing lead-in and discards prose from this tool reply. "
                       "The answer arrives as the next message; "
                       "grade it then with mastery_grade. For CONCEPT or DESIGN objectives use mastery_assess.",
        "parameters": {"type": "object", "properties": {
            "knowledge_point_id": {"type": "string", "description": "Objective id from mastery_status, verbatim."},
            "question": {"type": "string", "description": "The question stem only; do not list choice options here."},
            "expected_answer": {"type": "string", "description": "The correct answer (for choice: its label). "
                                                                  "Never shown to the learner before grading."},
            "question_type": {"type": "string", "enum": list(QUESTION_TYPES)},
            "options": {"type": "array", "description": "Choice questions only, in label order A, B, C...",
                        "items": {"type": "object", "properties": {"label": {"type": "string"},
                                                                   "body": {"type": "string"}},
                                  "required": ["label", "body"]}},
            "explanation": {"type": "string", "description": "Why the expected answer is right; shown after grading."},
            "difficulty": {"type": "string", "enum": ["easy", "medium", "hard"]},
        }, "required": ["knowledge_point_id", "question", "expected_answer"]},
    }},
    {"type": "function", "function": {
        "name": "mastery_grade",
        "description": "Grade the learner's answer to the open question, deterministically against the stored "
                       "expected answer. Updates mastery and review scheduling and says whether the objective's "
                       "gate is now cleared. Then give the learner feedback.",
        "parameters": {"type": "object", "properties": {
            "answer": {"type": "string", "description": "The learner's answer, verbatim."},
            "question_id": {"type": "string", "description": "Optional; the open question when omitted."},
        }, "required": ["answer"]},
    }},
    {"type": "function", "function": {
        "name": "mastery_assess",
        "description": "Record the qualitative gate for a CONCEPT or DESIGN objective after the learner explained "
                       "it in their own words. passed=true only when the explanation shows real understanding.",
        "parameters": {"type": "object", "properties": {
            "knowledge_point_id": {"type": "string"},
            "passed": {"type": "boolean"},
            "feedback": {"type": "string", "description": "What the learner showed or missed, briefly."},
        }, "required": ["knowledge_point_id", "passed"]},
    }},
    {"type": "function", "function": {
        "name": "path_outline",
        "description": "Read the learner's current path in module and knowledge point order. Call this before path_reorder "
                       "and use its ids verbatim. The result contains no question answer or evidence.",
        "parameters": {"type": "object", "properties": {}},
    }},
    {"type": "function", "function": {
        "name": "path_reorder",
        "description": "Only change order when the learner clearly asks. Call path_outline first for current ids. "
                       "This can reorder modules and knowledge points within each module only; it cannot add, "
                       "remove or move knowledge points between modules. Send module_ids only for a changed module "
                       "order, and knowledge_points only for modules whose inner order changes.",
        "parameters": {"type": "object", "properties": {
            "module_ids": {"type": "array", "description": "Every module id, in the requested order.",
                           "items": {"type": "string"}},
            "knowledge_points": {"type": "array", "description": "Only modules whose knowledge point order changes; "
                                 "include every knowledge point id in each such module.",
                                 "items": {"type": "object", "properties": {
                                     "module_id": {"type": "string"},
                                     "knowledge_point_ids": {"type": "array", "items": {"type": "string"}},
                                 }, "required": ["module_id", "knowledge_point_ids"]}},
        }},
    }},
    {"type": "function", "function": {
        "name": "learner_profile",
        "description": "Record only the learner's stated level, target, available time and study preferences. "
                       "Send only non-empty fields they actually stated; never include identity or contact details.",
        "parameters": {"type": "object", "properties": {
            field_name: {"type": "string"} for field_name in _PROFILE_FIELDS
        }},
    }},
]


@dataclass
class ToolOutcome:
    """What a tool returns to the model, the events it produces for the learner, and whether the turn ends."""

    result: dict[str, Any]
    events: list[tuple[str, dict[str, Any]]] = field(default_factory=list)
    ends_turn: bool = False
    question_id: str | None = None


def _error(message: str) -> ToolOutcome:
    return ToolOutcome({"error": message})


def _text(value: Any) -> str:
    return str(value or "").strip()[:_MAX_TEXT]


class TutorTools:
    def __init__(self, service: LearningService, path_id: str, *, session_id: str, turn_id: str) -> None:
        self._service = service
        self._path_id = path_id
        self._session_id = session_id
        self._turn_id = turn_id

    def execute(self, name: str, arguments: dict[str, Any] | None) -> ToolOutcome:
        if arguments is None:
            return _error("Tool arguments must be a JSON object.")
        handlers = {
            "mastery_status": self._status, "mastery_quiz": self._quiz,
            "mastery_grade": self._grade, "mastery_assess": self._assess,
            "path_outline": self._outline, "path_reorder": self._reorder,
            "learner_profile": self._profile,
        }
        handler = handlers.get(name)
        if handler is None:
            return _error(f"Unknown tool {name!r}. Available: {', '.join(handlers)}.")
        try:
            return handler(arguments)
        except (MasteryInteractionError, LearningStoreError, ValueError) as error:
            return _error(str(error))

    def status(self) -> dict[str, Any]:
        """The context the tutor works from: objective, open question, progress and the learner profile."""
        with self._service.store.transaction(self._path_id) as tx:
            progress = tx.progress
            active = tx.active_interaction()
        other_session_pending = active is not None and active.session_id not in ("", self._session_id)
        if other_session_pending:
            active = None
        step = next_objective(progress)
        summary = map_summary(progress)
        profile = progress.learner_profile
        return {
            "mode": "review" if step.action == "review" else "study",
            "objective": step.to_dict(),
            "pending_interaction": None if active is None else {
                "question_id": active.interaction_id,
                "status": active.status.value,
                "learner_answer": active.user_answer if active.status == InteractionStatus.ANSWERED else "",
                "question": public_pending_question(active.question).to_dict(),
            },
            "question_in_other_session": other_session_pending,
            "counts": summary["counts"],
            "due_reviews": summary["due_reviews"],
            "complete": summary["complete"],
            "learner_profile": None if profile is None or profile.is_empty()
            else profile.model_dump(mode="json", exclude={"updated_at"}),
        }

    def _status(self, _arguments: dict[str, Any]) -> ToolOutcome:
        return ToolOutcome(self.status())

    def _outline(self, _arguments: dict[str, Any]) -> ToolOutcome:
        with self._service.store.transaction(self._path_id) as tx:
            progress = tx.progress
        attempted_ids = {attempt.knowledge_point_id for attempt in progress.quiz_attempts}

        def point_row(point):
            mastered = is_mastered(progress, point)
            # Same three states as objective_status, with one scan of the attempt history.
            status = ("mastered" if mastered else "learning"
                      if point.id in attempted_ids or point.id in progress.qualitative_mastery else "new")
            return {"id": point.id, "name": point.name[:120], "type": point.type.value,
                    "status": status, "mastered": mastered}

        modules = [{
            "id": module.id, "name": module.name[:120], "order": module.order,
            "knowledge_points": [point_row(point) for point in module.knowledge_points],
        } for module in sorted(progress.modules, key=lambda entry: entry.order)]
        return ToolOutcome({"modules": modules, "objective_id": next_objective(progress).knowledge_point_id or None})

    def _reorder(self, arguments: dict[str, Any]) -> ToolOutcome:
        try:
            result = reorder_path(
                self._service, self._path_id, module_ids=arguments.get("module_ids"),
                knowledge_points=arguments.get("knowledge_points"),
                session_id=self._session_id, turn_id=self._turn_id)
        except ReorderRejected as exc:
            return ToolOutcome({"error": _REORDER_MESSAGES[exc.reason], "reason": exc.reason})
        if result.status == "unchanged":
            return ToolOutcome({"status": "unchanged"})
        progress = result.progress
        modules = sorted(progress.modules, key=lambda entry: entry.order)
        return ToolOutcome({
            "status": "reordered", "objective_id": next_objective(progress).knowledge_point_id or None,
            "modules": [{"id": module.id, "knowledge_point_ids": [point.id for point in module.knowledge_points]}
                        for module in modules],
        }, events=[("path.reordered", {
            "module_count": len(modules),
            "knowledge_point_count": sum(len(module.knowledge_points) for module in modules),
        })])

    def _profile(self, arguments: dict[str, Any]) -> ToolOutcome:
        fields = {key: value.strip() for key, value in arguments.items()
                  if key in _PROFILE_FIELDS and isinstance(value, str) and value.strip()}
        if not fields:
            return _error("Provide at least one non-empty learner profile field.")
        progress, recorded = self._service.record_learner_profile(
            self._path_id, fields=fields, session_id=self._session_id, turn_id=self._turn_id)
        profile = progress.learner_profile
        return ToolOutcome(
            {"recorded": recorded, "learner_profile": profile.model_dump(mode="json", exclude={"updated_at"})},
            events=[("profile.updated", {"fields": recorded})] if recorded else [],
        )

    def _quiz(self, arguments: dict[str, Any]) -> ToolOutcome:
        kp_id = _text(arguments.get("knowledge_point_id"))
        question = _text(arguments.get("question"))
        expected = _text(arguments.get("expected_answer"))
        question_type = _text(arguments.get("question_type")) or "short"
        if not question or not expected:
            return _error("mastery_quiz needs a non-empty question and expected_answer.")
        if question_type not in QUESTION_TYPES:
            return _error(f"question_type must be one of {', '.join(QUESTION_TYPES)}.")
        with self._service.store.transaction(self._path_id) as tx:
            kp, module_id, _module_name = find_knowledge_point(tx.progress, kp_id)
        if kp is None:
            return _error(f"Unknown objective {kp_id!r}; use an id from mastery_status.")
        if kp.type not in _QUIZ_TYPES:
            return _error(f"Objective {kp.name!r} is {kp.type.value}; judge it with mastery_assess instead.")

        options: list[PendingOption] = []
        if question_type == "choice":
            raw_options = arguments.get("options")
            if not isinstance(raw_options, list) or len(raw_options) < 2:
                return _error("A choice question needs at least two options.")
            for raw in raw_options:
                if not isinstance(raw, dict) or not _text(raw.get("label")) or not _text(raw.get("body")):
                    return _error("Every option needs a label and a body.")
                options.append(PendingOption(label=_text(raw["label"]).upper(), body=_text(raw["body"])))
            if {option.label for option in options} != canonical_labels(len(options)):
                return _error("Option labels must be A, B, C... in order, one per option.")
            label = resolve_answer(expected, {option.label: option.body for option in options})
            if not label:
                return _error("expected_answer must name exactly one of the options.")
            expected = label

        pending = PendingQuestion(
            question_id=str(uuid4()), knowledge_point_id=kp.id, module_id=module_id, prompt=question,
            question_type=question_type, expected_answer=expected, options=options,
            explanation=_text(arguments.get("explanation")), difficulty=_text(arguments.get("difficulty")),
        )
        _progress, interaction, created = self._service.register_question(
            self._path_id, pending, session_id=self._session_id, turn_id=self._turn_id,
            require_current_objective=True)
        if not created and interaction.session_id not in ("", self._session_id):
            return _error("Finish the open question in its original session before starting another.")
        self._service.mark_question_awaiting(
            self._path_id, interaction_id=interaction.interaction_id,
            session_id=self._session_id, turn_id=self._turn_id)
        public = public_pending_question(interaction.question).to_dict()
        return ToolOutcome(
            {"status": "registered" if created else "already_pending", "question_id": interaction.interaction_id,
             "note": "The question is on the learner's card. The turn ends now."},
            events=[("question", {**public, "knowledge_point_id": interaction.question.knowledge_point_id})],
            ends_turn=True,
            question_id=interaction.interaction_id,
        )

    def _grade(self, arguments: dict[str, Any]) -> ToolOutcome:
        progress, interaction, replayed = self._service.grade_interaction(
            self._path_id, answer=_text(arguments.get("answer")), question_id=_text(arguments.get("question_id")),
            scheduler=SpacedRepetitionScheduler(), session_id=self._session_id, turn_id=self._turn_id,
            require_answered=True,
        )
        kp, _module_id, _module_name = find_knowledge_point(progress, interaction.question.knowledge_point_id)
        mastered = bool(kp) and is_mastered(progress, kp)
        mastery = round(display_mastery(progress, kp), 3) if kp else 0.0
        is_correct = bool(interaction.result.get("is_correct"))
        grading = {"question_id": interaction.interaction_id, "knowledge_point_id": interaction.question.knowledge_point_id,
                   "is_correct": is_correct, "mastery": mastery, "mastered": mastered,
                   "explanation": interaction.question.explanation}
        return ToolOutcome(
            # The question is closed, so the model may now see and explain the expected answer.
            {**grading, "replayed": replayed, "expected_answer": interaction.question.expected_answer},
            events=[] if replayed else [("grading", grading)],
        )

    def _assess(self, arguments: dict[str, Any]) -> ToolOutcome:
        kp_id = _text(arguments.get("knowledge_point_id"))
        passed = arguments.get("passed")
        if not isinstance(passed, bool):
            return _error("mastery_assess needs passed: true or false.")
        progress = self._service.record_qualitative_for_path(
            self._path_id, kp_id, passed=passed, evidence=_text(arguments.get("feedback")),
            scheduler=SpacedRepetitionScheduler(), session_id=self._session_id, turn_id=self._turn_id,
            require_current_objective=True,
        )
        kp, _module_id, _module_name = find_knowledge_point(progress, kp_id)
        grading = {"knowledge_point_id": kp_id, "passed": passed, "mastered": bool(kp) and is_mastered(progress, kp),
                   "mastery": round(display_mastery(progress, kp), 3) if kp else 0.0}
        return ToolOutcome(grading, events=[("grading", grading)])
