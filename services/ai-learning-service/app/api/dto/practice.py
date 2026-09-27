"""Practice notebook and review API DTOs. Wire names are camelCase."""

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.api.dto.responses import ApiResponse


class PracticeAnswerRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    answer: str = Field(min_length=1, max_length=4000)


class PracticeReviewRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    request_id: UUID = Field(alias="requestId")
    entry_id: int = Field(alias="entryId", gt=0)
    answer: str = Field(min_length=1, max_length=4000)
    rating: Literal["hard", "good", "easy"] = "good"


class PracticeEntryResponse(ApiResponse):
    entry_id: int = Field(alias="entryId")
    question_id: str = Field(alias="questionId")
    session_id: UUID = Field(alias="sessionId")
    knowledge_point_id: UUID = Field(alias="knowledgePointId")
    knowledge_point_name: str = Field(alias="knowledgePointName")
    prompt: str
    question_type: Literal["short", "choice"] = Field(alias="questionType")
    options: list[dict[str, str]] = Field(default_factory=list)
    difficulty: str
    created_at: datetime = Field(alias="createdAt")
    answered_at: datetime | None = Field(default=None, alias="answeredAt")
    user_answer: str = Field(default="", alias="userAnswer")
    is_correct: bool = Field(alias="isCorrect")
    resolved: bool
    correct_answer: str | None = Field(default=None, alias="correctAnswer")
    explanation: str | None = None


class PracticeAnswerResponse(ApiResponse):
    entry_id: int = Field(alias="entryId")
    question_id: str = Field(alias="questionId")
    is_correct: bool = Field(alias="isCorrect")
    correct_answer: str = Field(alias="correctAnswer")
    explanation: str
    due_at: datetime | None = Field(default=None, alias="dueAt")


class PracticeReviewResponse(ApiResponse):
    entry_id: int = Field(alias="entryId")
    question_id: str = Field(alias="questionId")
    is_correct: bool = Field(alias="isCorrect")
    rating: Literal["again", "hard", "good", "easy"]
    due_at: datetime = Field(alias="dueAt")
    resolved: bool
    correct_answer: str = Field(alias="correctAnswer")
    explanation: str


__all__ = [
    "PracticeAnswerRequest", "PracticeAnswerResponse", "PracticeEntryResponse", "PracticeReviewRequest",
    "PracticeReviewResponse",
]
