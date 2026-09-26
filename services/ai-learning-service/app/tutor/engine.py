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
from app.tutor.prompts import SYSTEM_PROMPT
from app.tutor.session_store import ActiveTurnConflict, TutorSession, TutorSessionStore
from app.tutor.tools import TOOL_DEFINITIONS, TutorTools

logger = logging.getLogger(__name__)

Chat = Callable[..., Awaitable[ChatReply]]


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
                 max_rounds: int = 6, history: int = 20) -> None:
        self._sessions = sessions
        self._learning = LearningService(learning_store)
        self._max_rounds = max_rounds
        self._history = history

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
                  answer: CardAnswer | None = None) -> AsyncIterator[TutorEvent]:
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

            tools = TutorTools(self._learning, str(turn.session.path_id), session_id=session_id, turn_id=turn_id)
            messages = await self._context(turn, tools)
            spoken: list[str] = []
            pending_spoken: list[str] = []
            lead_in_sent = False
            question_id: str | None = None
            ended = False
            for _round in range(self._max_rounds):
                failure = "llm_error"
                reply = await chat(messages, tools=TOOL_DEFINITIONS)
                failure = "internal_error"
                # Hold model prose until the turn's outcome is known: an earlier tool reply may
                # already hint at the answer to a question the model poses in a later round.
                quiz_requested = any(call.name == "mastery_quiz" for call in reply.tool_calls)
                if quiz_requested and not lead_in_sent:
                    spoken.append("Try this question.")
                    yield TutorEvent("assistant.message", {"text": "Try this question."})
                    lead_in_sent = True
                elif not quiz_requested and reply.content.strip():
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
                        question_id = outcome.question_id
                        ended = True
                        break
                if ended:
                    break
            if not ended:
                failure = "too_many_rounds"
                raise RuntimeError("The tutor used every round without finishing")

            if question_id is None:
                for text in pending_spoken:
                    yield TutorEvent("assistant.message", {"text": text})
                spoken.extend(pending_spoken)
            if spoken or question_id:
                await asyncio.to_thread(
                    self._sessions.add_message, turn.user_id, turn.session.id, turn.turn_id, "assistant",
                    "\n\n".join(spoken), {"question_id": question_id} if question_id else {})
            await asyncio.to_thread(self._sessions.finish_turn, turn.turn_id, "completed")
            closed = True
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
        history = await asyncio.to_thread(self._sessions.recent_messages, turn.user_id, turn.session.id,
                                          self._history)
        messages: list[dict[str, Any]] = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "system", "content": "Current status (from mastery_status):\n"
                                          + json.dumps(status, ensure_ascii=False, default=str)},
        ]
        messages.extend({"role": item.role, "content": item.content} for item in history if item.content)
        return messages


__all__ = ["ActiveTurnConflict", "CardAnswer", "OpenTurn", "SessionNotFound", "TutorEngine", "TutorEvent"]
