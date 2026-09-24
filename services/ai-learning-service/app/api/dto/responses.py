"""Allowlisted response models for learner-facing Phase 1 APIs."""

from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ApiResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")


class PathCreatedResponse(ApiResponse):
    path_id: UUID = Field(alias="pathId")
    revision: int
    module_count: int = Field(alias="moduleCount")
    knowledge_point_count: int = Field(alias="knowledgePointCount")


class MasteryCounts(ApiResponse):
    mastered: int
    learning: int
    new: int
    total: int


class KnowledgePointProgress(ApiResponse):
    id: UUID
    name: str
    type: str
    status: Literal["mastered", "learning", "new"]
    mastery: float
    mastery_source: str = Field(alias="masterySource")
    override_note: str = Field(default="", alias="overrideNote")


class ModuleProgress(ApiResponse):
    id: UUID
    name: str
    objective: str = ""
    order: int
    mastered: int
    total: int
    knowledge_points: list[KnowledgePointProgress] = Field(alias="knowledgePoints")


class MasterySummary(ApiResponse):
    name: str
    counts: MasteryCounts
    due_reviews: int = Field(alias="dueReviews")
    complete: bool
    modules: list[ModuleProgress]


class LearningProgressResponse(ApiResponse):
    path_id: UUID = Field(alias="pathId")
    revision: int
    module_count: int = Field(alias="moduleCount")
    knowledge_point_count: int = Field(alias="knowledgePointCount")
    mastery: MasterySummary


class LearningStatusResponse(ApiResponse):
    path_id: UUID = Field(alias="pathId")
    revision: int
    action: str
    module_id: str = Field(alias="moduleId")
    module_name: str = Field(alias="moduleName")
    knowledge_point_id: str = Field(alias="knowledgePointId")
    knowledge_point_name: str = Field(alias="knowledgePointName")
    knowledge_point_type: str = Field(alias="knowledgePointType")
    status: str
    mastery: float
    reason: str


class LearningPathMapResponse(ApiResponse):
    path_id: UUID = Field(alias="pathId")
    revision: int
    map: MasterySummary
