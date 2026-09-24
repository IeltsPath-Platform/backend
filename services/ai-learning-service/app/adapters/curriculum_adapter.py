"""Map Content Service curriculum records into DeepTutor's pinned models."""

from typing import Any
from uuid import UUID

from deeptutor.learning.models import KnowledgePoint, KnowledgeType, LearningModule


_KNOWLEDGE_TYPES = {
    "MEMORY": KnowledgeType.MEMORY,
    "CONCEPT": KnowledgeType.CONCEPT,
    "PROCEDURE": KnowledgeType.PROCEDURE,
    "DESIGN": KnowledgeType.DESIGN,
}


class CurriculumContractError(ValueError):
    """Canonical content is incomplete or cannot be mapped safely."""


class CurriculumAdapter:
    @staticmethod
    def to_modules(
        topics: list[dict[str, Any]], knowledge_points: list[dict[str, Any]]
    ) -> list[LearningModule]:
        by_topic: dict[str, list[dict[str, Any]]] = {}
        for point in knowledge_points:
            topic_id = str(point.get("topicId", ""))
            by_topic.setdefault(topic_id, []).append(point)

        modules: list[LearningModule] = []
        # ContentServiceClient already returns deterministic preorder: parents
        # followed by their children, with siblings in canonical sortOrder.
        # Keep that hierarchy order instead of globally sorting the flattened list.
        ordered_topics = topics
        for order, topic in enumerate(ordered_topics):
            try:
                module_id = str(UUID(str(topic["id"])))
                name = str(topic["name"]).strip()
                if not name:
                    raise ValueError("topic name is blank")
                mapped_points: list[KnowledgePoint] = []
                points = sorted(
                    by_topic.get(module_id, []),
                    # The current Content Service DTO has no sortOrder for KPs.
                    # createdAt is available; UUID breaks ties deterministically.
                    key=lambda point: (str(point.get("createdAt", "")), str(point["id"])),
                )
                for point in points:
                    point_id = str(UUID(str(point["id"])))
                    learning_type = str(point.get("learningType", "")).upper()
                    if learning_type not in _KNOWLEDGE_TYPES:
                        raise CurriculumContractError(
                            f"Knowledge point {point_id} has missing or unsupported learningType"
                        )
                    point_name = str(point.get("name", "")).strip()
                    if not point_name:
                        raise CurriculumContractError(f"Knowledge point {point_id} has a blank name")
                    mapped_points.append(
                        KnowledgePoint(
                            id=point_id,
                            name=point_name,
                            type=_KNOWLEDGE_TYPES[learning_type],
                            module_id=module_id,
                        )
                    )
                modules.append(
                    LearningModule(
                        id=module_id,
                        name=name,
                        order=order,
                        knowledge_points=mapped_points,
                    )
                )
            except CurriculumContractError:
                raise
            except (KeyError, TypeError, ValueError) as exc:
                raise CurriculumContractError("Content Service returned invalid curriculum metadata") from exc
        if not modules or not any(module.knowledge_points for module in modules):
            raise CurriculumContractError("Published curriculum has no active knowledge points")
        return modules
