"""Request and response models of the tutor API. Wire names are camelCase, like the other learner APIs."""

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.api.dto.responses import ApiResponse


class CreateSessionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    title: str | None = Field(default=None, max_length=200)


class CardAnswerRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    question_id: UUID = Field(alias="questionId")
    text: str = Field(min_length=1, max_length=2000)


class TurnRequest(BaseModel):
    """Exactly one of a free message or an answer to the question card."""

    model_config = ConfigDict(extra="forbid")

    message: str | None = Field(default=None, min_length=1, max_length=4000)
    answer: CardAnswerRequest | None = None

    @model_validator(mode="after")
    def exactly_one_input(self) -> "TurnRequest":
        if (self.message is None) == (self.answer is None):
            raise ValueError("Send exactly one of message or answer")
        return self


class TutorSessionResponse(ApiResponse):
    session_id: UUID = Field(alias="sessionId")
    path_id: UUID = Field(alias="pathId")
    title: str
    created_at: datetime = Field(alias="createdAt")
    updated_at: datetime = Field(alias="updatedAt")


class LearnerMemoryResponse(ApiResponse):
    content: str
    updated_at: datetime | None = Field(default=None, alias="updatedAt")


class TutorMessageResponse(ApiResponse):
    id: int
    role: str
    content: str
    created_at: datetime = Field(alias="createdAt")
    question_id: str | None = Field(default=None, alias="questionId")


class PendingOptionResponse(ApiResponse):
    label: str
    body: str


class PendingQuestionResponse(ApiResponse):
    question_id: str = Field(alias="questionId")
    knowledge_point_id: str = Field(alias="knowledgePointId")
    prompt: str
    question_type: str = Field(alias="questionType")
    options: list[PendingOptionResponse] = Field(default_factory=list)
    status: str


class TutorSessionDetailResponse(TutorSessionResponse):
    messages: list[TutorMessageResponse]
    pending_question: PendingQuestionResponse | None = Field(default=None, alias="pendingQuestion")


def session_payload(session: Any) -> dict[str, Any]:
    return {"sessionId": session.id, "pathId": session.path_id, "title": session.title,
            "createdAt": session.created_at, "updatedAt": session.updated_at}
