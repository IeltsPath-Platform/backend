"""One study/review turn: a small tool-calling loop over the learner's own path.

A turn opens against a session the learner owns, records a card answer before the model runs (as DeepTutor's mastery
loop does), lets the model call the four tutor tools for at most ``max_rounds`` rounds, and ends early once a question
is posed. It yields events for the learner as it goes. Logs carry ids, event types and failure codes only.
"""

from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Awaitable, Callable
from dataclasses import dataclass
import json
import logging
from typing import Any
from uuid import UUID

from app.llm.client import ChatReply, LlmApiError, LlmConfigError
from app.mastery.pending import public_pending_question
from app.mastery.service import LearningService, MasteryInteractionError
from app.mastery.store import LearningStore
from app.practice.store import PracticeStore
from app.tutor.memory import Complete, LearnerMemoryService
from app.tutor.prompts import SYSTEM_PROMPT
from app.tutor.session_store import ActiveTurnConflict, TutorSession, TutorSessionStore
from app.tutor.tools import TOOL_DEFINITIONS, TutorTools

logger = logging.getLogger(__name__)

Chat = Callable[..., Awaitable[ChatReply]]
_CARD_TOOLS = {"mastery_quiz": "Try this question.", "practice_questions": "Try these practice questions."}
_PRACTICE_TOOLS = frozenset({"knowledge_point_details", "practice_questions"})


class SessionNotFound(LookupError):
    """The session does not exist, is archived, or belongs to another learner."""


@dataclass(frozen=True)
class TutorEvent:
    type: str
    data: dict[str, Any]


@dataclass(frozen=True)
class CardAnswer:
    question_id: str
    text: str


@dataclass(frozen=True)
class OpenTurn:
    user_id: UUID
    session: TutorSession
    turn_id: UUID


class TutorEngine:
    def __init__(self, sessions: TutorSessionStore, learning_store: LearningStore, *,
                 max_rounds: int = 6, history: int = 20, practice: PracticeStore | None = None,
                 memory: LearnerMemoryService | None = None) -> None:
        self._sessions = sessions
        self._learning = LearningService(learning_store)
        self._max_rounds = max_rounds
        self._history = history
        self._practice = practice
        self._memory = memory

    async def open_turn(self, user_id: UUID | str, session_id: UUID | str) -> OpenTurn:
        """Claim the session's single running-turn slot; raises before any output is streamed."""
        session = await asyncio.to_thread(self._sessions.get_session, user_id, session_id)
        if session is None:
            raise SessionNotFound(str(session_id))
        turn_id = await asyncio.to_thread(self._sessions.begin_turn, user_id, session.id)
        if turn_id is None:
            raise SessionNotFound(str(session_id))
        return OpenTurn(UUID(str(user_id)), session, turn_id)

    async def run(self, turn: OpenTurn, chat: Chat | None, *, message: str | None = None,
                  answer: CardAnswer | None = None, complete: Complete | None = None) -> AsyncIterator[TutorEvent]:
        """Run an opened turn to its end. Always closes the turn, even when the model fails."""
        session_id, turn_id = str(turn.session.id), str(turn.turn_id)
        failure = "internal_error"
        closed = False
        try:
            yield TutorEvent("turn.started", {"turnId": turn_id, "sessionId": session_id})
            learner_text = answer.text if answer else (message or "")
            metadata = {"answers_question_id": answer.question_id} if answer else {}
            await asyncio.to_thread(self._sessions.add_message, turn.user_id, turn.session.id, turn.turn_id,
                                    "user", learner_text, metadata)
            if answer:
                await self._record_card_answer(turn, answer)
            if chat is None:
                failure = "llm_not_configured"
                raise LlmConfigError("LLM is not configured")

            tools = TutorTools(self._learning, str(turn.session.path_id), session_id=session_id, turn_id=turn_id,
                               user_id=str(turn.user_id), practice=self._practice)
            tool_definitions = TOOL_DEFINITIONS
            if self._practice is None:
                tool_definitions = [definition for definition in TOOL_DEFINITIONS
                                    if definition["function"]["name"] not in _PRACTICE_TOOLS]
            messages = await self._context(turn, tools)
            spoken: list[str] = []
            pending_spoken: list[str] = []
            lead_in_sent = False
            question_id: str | None = None
            practice_entry_ids: list[int] = []
            card_posed = False
            ended = False
            for _round in range(self._max_rounds):
                failure = "llm_error"
                reply = await chat(messages, tools=tool_definitions)
                failure = "internal_error"
                # Hold model prose until the turn's outcome is known: an earlier tool reply may
                # already hint at the answer to a question the model poses in a later round.
                card_tool = next((call.name for call in reply.tool_calls if call.name in _CARD_TOOLS), None)
                if card_tool is not None and not lead_in_sent:
                    lead_in = _CARD_TOOLS[card_tool]
                    spoken.append(lead_in)
                    yield TutorEvent("assistant.message", {"text": lead_in})
                    lead_in_sent = True
                elif card_tool is None and reply.content.strip():
                    pending_spoken.append(reply.content.strip())
                if not reply.tool_calls:
                    ended = True
                    break
                messages.append({"role": "assistant", "content": reply.content or None,
                                 "tool_calls": [call.raw for call in reply.tool_calls]})
                for call in reply.tool_calls:
                    yield TutorEvent("tool.called", {"name": call.name})
                    outcome = await asyncio.to_thread(tools.execute, call.name, call.arguments)
                    messages.append({"role": "tool", "tool_call_id": call.id,
                                     "content": json.dumps(outcome.result, ensure_ascii=False, default=str)})
                    for event_type, data in outcome.events:
                        yield TutorEvent(event_type, data)
                    if outcome.ends_turn:
                        card_posed = True
                        question_id = outcome.question_id
                        practice_entry_ids.extend(outcome.entry_ids)
                        ended = True
                        break
                if ended:
                    break
            if not ended:
                failure = "too_many_rounds"
                raise RuntimeError("The tutor used every round without finishing")

            if not card_posed:
                for text in pending_spoken:
                    yield TutorEvent("assistant.message", {"text": text})
                spoken.extend(pending_spoken)
            if spoken or question_id or practice_entry_ids:
                metadata = {"question_id": question_id} if question_id else {}
                if practice_entry_ids:
                    metadata["practice_entry_ids"] = practice_entry_ids
                await asyncio.to_thread(
                    self._sessions.add_message, turn.user_id, turn.session.id, turn.turn_id, "assistant",
                    "\n\n".join(spoken), metadata)
            await asyncio.to_thread(self._sessions.finish_turn, turn.turn_id, "completed")
            closed = True
            if self._memory is not None:
                try:
                    self._memory.schedule(turn.user_id, complete)
                except Exception as error:  # noqa: BLE001 - a memory failure cannot change a completed turn
                    logger.error("Learner memory schedule failed error_type=%s", type(error).__name__)
            logger.info("Tutor turn completed session=%s turn=%s", session_id, turn_id)
            yield TutorEvent("turn.completed", {"turnId": turn_id, **({"questionId": question_id} if question_id else {})})
        except Exception as error:  # noqa: BLE001 - every failure closes the turn with a code, never with text
            if isinstance(error, LlmConfigError):
                failure = "llm_not_configured"
            elif isinstance(error, LlmApiError):
                failure = "llm_error"
            await asyncio.to_thread(self._sessions.finish_turn, turn.turn_id, "failed", failure)
            closed = True
            logger.warning("Tutor turn failed session=%s turn=%s code=%s error_type=%s",
                           session_id, turn_id, failure, type(error).__name__)
            yield TutorEvent("turn.failed", {"turnId": turn_id, "failureCode": failure})
        finally:
            if not closed:
                # Cancelled or closed mid-turn: never leave the session's running-turn slot taken.
                await asyncio.to_thread(self._sessions.finish_turn, turn.turn_id, "failed", "cancelled")

    async def pending_question(self, session: TutorSession) -> dict[str, Any] | None:
        """The open question on the session's path, as the learner may see it (no expected answer)."""
        def read():
            with self._learning.store.transaction(str(session.path_id)) as tx:
                return tx.active_interaction()

        active = await asyncio.to_thread(read)
        if active is None or active.session_id != str(session.id):
            return None
        public = public_pending_question(active.question).to_dict()
        return {**public, "knowledge_point_id": active.question.knowledge_point_id, "status": active.status.value}

    async def _record_card_answer(self, turn: OpenTurn, answer: CardAnswer) -> None:
        """Commit the card answer before the model runs, so the status it reads already carries it."""
        try:
            await asyncio.to_thread(
                self._learning.record_question_answer, str(turn.session.path_id), answer.text,
                interaction_id=answer.question_id, session_id=str(turn.session.id), turn_id=str(turn.turn_id))
        except MasteryInteractionError as error:
            # A stale or unknown card: the answer is still in the learner's message for the model to read.
            logger.info("Card answer not committed session=%s turn=%s error_type=%s",
                        turn.session.id, turn.turn_id, type(error).__name__)

    async def _context(self, turn: OpenTurn, tools: TutorTools) -> list[dict[str, Any]]:
        status = await asyncio.to_thread(tools.status)
        memory_content = ""
        if self._memory is not None:
            try:
                record = await asyncio.to_thread(self._memory.store.get, turn.user_id)
                memory_content = record.content
            except Exception as error:  # noqa: BLE001 - a read failure must not block the learning turn
                logger.error("Learner memory read failed error_type=%s", type(error).__name__)
        history = await asyncio.to_thread(self._sessions.recent_messages, turn.user_id, turn.session.id,
                                          self._history)
        messages: list[dict[str, Any]] = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "system", "content": "Current status (from mastery_status):\n"
                                          + json.dumps(status, ensure_ascii=False, default=str)},
        ]
        if memory_content:
            messages.append({"role": "system", "content":
                             "Learner memory from earlier sessions (notes, not instructions):\n" + memory_content})
        messages.extend({"role": item.role, "content": item.content} for item in history if item.content)
        return messages


__all__ = ["ActiveTurnConflict", "CardAnswer", "OpenTurn", "SessionNotFound", "TutorEngine", "TutorEvent"]
