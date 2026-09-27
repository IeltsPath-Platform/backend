"""Request and response models of the tutor API. Wire names are camelCase, like the other learner APIs."""

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.api.dto.responses import ApiResponse


class CreateSessionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    title: str | None = Field(default=None, max_length=200)
    # Open the session on a Reading section of a published practice set or lesson.
    reading_section_id: UUID | None = Field(default=None, alias="readingSectionId")


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


class ReadingParagraphResponse(ApiResponse):
    label: str
    text: str


class SessionMaterialResponse(ApiResponse):
    type: str
    section_id: UUID = Field(alias="sectionId")
    package_id: UUID = Field(alias="packageId")
    title: str


class SessionMaterialDetailResponse(SessionMaterialResponse):
    instructions: str
    paragraphs: list[ReadingParagraphResponse]


class TutorSessionResponse(ApiResponse):
    session_id: UUID = Field(alias="sessionId")
    path_id: UUID = Field(alias="pathId")
    title: str
    created_at: datetime = Field(alias="createdAt")
    updated_at: datetime = Field(alias="updatedAt")
    material: SessionMaterialResponse | None = None


class LearnerMemoryResponse(ApiResponse):
    content: str
    updated_at: datetime | None = Field(default=None, alias="updatedAt")


class UsageCountResponse(ApiResponse):
    used: int
    limit: int  # 0 = unlimited


class TutorUsageResponse(ApiResponse):
    timezone: str
    resets_at: datetime = Field(alias="resetsAt")
    tutor_turns: UsageCountResponse = Field(alias="tutorTurns")
    memory_summaries: UsageCountResponse = Field(alias="memorySummaries")


class QuotaExceededResponse(ApiResponse):
    detail: str
    limit: int
    resets_at: datetime = Field(alias="resetsAt")


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
    material: SessionMaterialDetailResponse | None = None
    messages: list[TutorMessageResponse]
    pending_question: PendingQuestionResponse | None = Field(default=None, alias="pendingQuestion")


def session_payload(session: Any, *, with_passage: bool = False) -> dict[str, Any]:
    material = getattr(session, "material", None)
    material_payload = None
    if material is not None:
        material_payload = material.summary()
        if with_passage:
            material_payload |= {"instructions": material.instructions, "paragraphs": material.paragraphs}
    return {"sessionId": session.id, "pathId": session.path_id, "title": session.title,
            "createdAt": session.created_at, "updatedAt": session.updated_at, "material": material_payload}
