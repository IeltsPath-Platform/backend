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
from app.practice.store import PracticeNotFound, PracticeStore
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
    {"type": "function", "function": {
        "name": "knowledge_point_details",
        "description": "Read the saved Content description and skill for a knowledge point in the learner's path. "
                       "Call this before writing extra practice questions.",
        "parameters": {"type": "object", "properties": {
            "knowledge_point_id": {"type": "string", "description": "A knowledge point id from path_outline."},
        }, "required": ["knowledge_point_id"]},
    }},
    {"type": "function", "function": {
        "name": "practice_questions",
        "description": "Save and pose 1 to 5 extra short-answer or multiple-choice questions for any knowledge point "
                       "in the path. Every question needs an explanation. The cards are shown without answers and "
                       "this tool ends the turn; the learner answers them outside the conversation.",
        "parameters": {"type": "object", "properties": {
            "knowledge_point_id": {"type": "string"},
            "questions": {"type": "array", "minItems": 1, "maxItems": 5, "items": {
                "type": "object", "properties": {
                    "question": {"type": "string"},
                    "question_type": {"type": "string", "enum": ["short", "choice"]},
                    "expected_answer": {"type": "string"},
                    "options": {"type": "array", "items": {"type": "object", "properties": {
                        "label": {"type": "string"}, "body": {"type": "string"},
                    }, "required": ["label", "body"]}},
                    "explanation": {"type": "string"},
                    "difficulty": {"type": "string", "enum": ["easy", "medium", "hard"]},
                }, "required": ["question", "question_type", "expected_answer", "explanation"],
            }},
        }, "required": ["knowledge_point_id", "questions"]},
    }},
    {"type": "function", "function": {
        "name": "save_note",
        "description": "Offer a note draft only when the learner asks to save or remember something. "
                       "The learner's app saves the draft to their notes. Do not include an answer "
                       "to an open question.",
        "parameters": {"type": "object", "properties": {
            "title": {"type": "string", "description": "A short title for the learner's note."},
            "body": {"type": "string", "description": "What the learner asked to keep, clear for later reading."},
            "knowledge_point_id": {"type": "string", "description": "Optional id from this path when the note is about a knowledge point."},
        }, "required": ["title", "body"]},
    }},
]


@dataclass
class ToolOutcome:
    """What a tool returns to the model, the events it produces for the learner, and whether the turn ends."""

    result: dict[str, Any]
    events: list[tuple[str, dict[str, Any]]] = field(default_factory=list)
    ends_turn: bool = False
    question_id: str | None = None
    entry_ids: list[int] = field(default_factory=list)


def _error(message: str) -> ToolOutcome:
    return ToolOutcome({"error": message})


def _text(value: Any) -> str:
    return str(value or "").strip()[:_MAX_TEXT]


def _parse_question(arguments: dict[str, Any]) -> tuple[str, str, str, list[PendingOption]] | str:
    """Parse the question fields shared by mastery_quiz and practice_questions."""
    question = _text(arguments.get("question"))
    expected = _text(arguments.get("expected_answer"))
    question_type = _text(arguments.get("question_type")) or "short"
    if not question or not expected:
        return "question and expected_answer must be non-empty."
    if question_type not in QUESTION_TYPES:
        return f"question_type must be one of {', '.join(QUESTION_TYPES)}."

    options: list[PendingOption] = []
    if question_type == "choice":
        raw_options = arguments.get("options")
        if not isinstance(raw_options, list) or len(raw_options) < 2:
            return "A choice question needs at least two options."
        for raw in raw_options:
            if not isinstance(raw, dict) or not _text(raw.get("label")) or not _text(raw.get("body")):
                return "Every option needs a label and a body."
            options.append(PendingOption(label=_text(raw["label"]).upper(), body=_text(raw["body"])))
        if {option.label for option in options} != canonical_labels(len(options)):
            return "Option labels must be A, B, C... in order, one per option."
        label = resolve_answer(expected, {option.label: option.body for option in options})
        if not label:
            return "expected_answer must name exactly one of the options."
        expected = label
    return question, question_type, expected, options


class TutorTools:
    def __init__(self, service: LearningService, path_id: str, *, session_id: str, turn_id: str,
                 user_id: str | None = None, practice: PracticeStore | None = None) -> None:
        self._service = service
        self._path_id = path_id
        self._session_id = session_id
        self._turn_id = turn_id
        self._user_id = user_id
        self._practice = practice

    def execute(self, name: str, arguments: dict[str, Any] | None) -> ToolOutcome:
        if arguments is None:
            return _error("Tool arguments must be a JSON object.")
        handlers = {
            "mastery_status": self._status, "mastery_quiz": self._quiz,
            "mastery_grade": self._grade, "mastery_assess": self._assess,
            "path_outline": self._outline, "path_reorder": self._reorder,
            "learner_profile": self._profile, "knowledge_point_details": self._details,
            "practice_questions": self._practice_questions, "save_note": self._save_note,
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

    def _details(self, arguments: dict[str, Any]) -> ToolOutcome:
        kp_id = _text(arguments.get("knowledge_point_id"))
        with self._service.store.transaction(self._path_id) as tx:
            kp, module_id, module_name = find_knowledge_point(tx.progress, kp_id)
            if kp is None:
                return _error(f"Unknown knowledge point {kp_id!r}; use an id from path_outline.")
            detail = self._service.store.knowledge_point_details(self._path_id).get(kp.id)
            band = self._service.store.knowledge_point_bands(self._path_id).get(kp.id)
        return ToolOutcome({
            "id": kp.id,
            "name": kp.name,
            "type": kp.type.value,
            "module_id": module_id,
            "module_name": module_name,
            "skill": detail.skill if detail else None,
            "description": detail.description if detail else "",
            "band_min": float(band.min) if band and band.min is not None else None,
            "band_max": float(band.max) if band and band.max is not None else None,
        })

    def _save_note(self, arguments: dict[str, Any]) -> ToolOutcome:
        raw_title, raw_body = arguments.get("title"), arguments.get("body")
        if not isinstance(raw_title, str) or not raw_title.strip():
            return _error("title must be non-empty.")
        if not isinstance(raw_body, str) or not raw_body.strip():
            return _error("body must be non-empty.")
        title = raw_title.strip()[:255]
        body = raw_body.strip()
        if len(body) > 19900:
            body = body[:19900] + "\n\n[truncated]"

        # Models often send an optional field as null or ""; both mean "no knowledge point".
        kp_id = arguments.get("knowledge_point_id")
        if kp_id is not None and not isinstance(kp_id, str):
            return _error("knowledge_point_id must be an id from path_outline.")
        kp_id = (kp_id or "").strip()
        has_kp = bool(kp_id)
        with self._service.store.transaction(self._path_id) as tx:
            if has_kp:
                kp, _module_id, _module_name = find_knowledge_point(tx.progress, kp_id)
                if kp is None:
                    return _error(f"Unknown knowledge point {kp_id!r}; use an id from path_outline.")
            active = tx.active_interaction()
            if active is not None and active.status != InteractionStatus.GRADED and active.session_id == self._session_id:
                expected = active.question.expected_answer.casefold()
                if len(expected) >= 3 and (expected in title.casefold() or expected in body.casefold()):
                    return _error("The note would reveal the answer to the open question; save it after grading.")

        return ToolOutcome(
            {"status": "offered", "note": "The learner's app saves this note."},
            events=[("note.draft", {
                "title": title, "body": body,
                "source_type": "KNOWLEDGE_POINT" if has_kp else "TUTOR_SESSION",
                "source_reference_id": kp.id if has_kp else self._session_id,
            })],
        )

    def _practice_questions(self, arguments: dict[str, Any]) -> ToolOutcome:
        if self._practice is None or self._user_id is None:
            return _error("Practice is not available.")
        kp_id = _text(arguments.get("knowledge_point_id"))
        with self._service.store.transaction(self._path_id) as tx:
            kp, _module_id, _module_name = find_knowledge_point(tx.progress, kp_id)
        if kp is None:
            return _error(f"Unknown knowledge point {kp_id!r}; use an id from path_outline.")

        raw_questions = arguments.get("questions")
        if not isinstance(raw_questions, list) or not raw_questions:
            return _error("question 1: provide at least one practice question.")
        if len(raw_questions) > 5:
            return _error("question 6: a practice batch can contain at most five questions.")

        questions: list[dict[str, Any]] = []
        public_questions: list[dict[str, Any]] = []
        for index, raw in enumerate(raw_questions, start=1):
            if not isinstance(raw, dict):
                return _error(f"question {index}: each question must be a JSON object.")
            if not isinstance(raw.get("question_type"), str) or not raw["question_type"].strip():
                return _error(f"question {index}: question_type must be short or choice.")
            parsed = _parse_question(raw)
            if isinstance(parsed, str):
                return _error(f"question {index}: {parsed}")
            question, question_type, expected, options = parsed
            if question_type not in {"short", "choice"}:
                return _error(f"question {index}: question_type must be short or choice.")
            explanation = _text(raw.get("explanation"))
            if not explanation:
                return _error(f"question {index}: explanation must be non-empty.")
            question_id = str(uuid4())
            option_rows = [{"label": option.label, "body": option.body} for option in options]
            difficulty = _text(raw.get("difficulty"))
            if difficulty and difficulty not in {"easy", "medium", "hard"}:
                return _error(f"question {index}: difficulty must be easy, medium, or hard.")
            questions.append({
                "question_id": question_id,
                "question": question,
                "question_type": question_type,
                "options": option_rows,
                "correct_answer": expected,
                "explanation": explanation,
                "difficulty": difficulty,
            })
            public_questions.append({
                "entry_id": None,
                "prompt": question,
                "question_type": question_type,
                "options": option_rows,
                "difficulty": difficulty,
            })

        try:
            entry_ids = self._practice.create_entries(
                self._user_id, self._session_id, self._turn_id, self._path_id, kp.id, kp.name, questions,
            )
        except PracticeNotFound:
            return _error("This session can no longer hold practice questions.")
        for public_question, entry_id in zip(public_questions, entry_ids, strict=True):
            public_question["entry_id"] = entry_id
        return ToolOutcome(
            {"status": "posed", "entry_ids": entry_ids,
             "note": "The practice cards are shown. The turn ends now."},
            events=[("practice.questions", {"knowledge_point_id": kp.id, "questions": public_questions})],
            ends_turn=True,
            entry_ids=entry_ids,
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

        parsed = _parse_question(arguments)
        if isinstance(parsed, str):
            return _error(parsed)
        question, question_type, expected, options = parsed

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
