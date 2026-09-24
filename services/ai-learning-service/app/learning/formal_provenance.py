"""Provenance of formal Assessment evidence inside the DeepTutor aggregate.

DeepTutor v1.6.9 models ignore unknown fields (``extra="ignore"``), so provenance
cannot be added as new attributes without a fork. It is carried in fields the
pinned models already persist:

* ``LearningEvidence.source``     -> ``FORMAL_EVIDENCE_SOURCE``
* ``LearningEvidence.turn_id``    -> the deterministic ``source_reference_id``
* ``LearningEvidence.session_id`` -> ``assessment:{attempt}:{result}:{version}:{item_result}``
* ``QuizAttempt.question_id``     -> the same ``source_reference_id``

Because the aggregate itself keeps these, superseded evidence stays
identifiable after any reload or rebuild of ``mastery_paths.state_json``.
"""

from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID, uuid5

from deeptutor.learning.models import LearningEvidence

FORMAL_EVIDENCE_SOURCE = "assessment_service"

# Fixed namespace for formal evidence identities. Changing it would give
# already-applied evidence new identities and break idempotent redelivery.
FORMAL_EVIDENCE_NAMESPACE = UUID("6f0c1b2e-3d7a-4e59-9b8a-2c4d5e6f7a81")

_PROVENANCE_PREFIX = "assessment"


def source_reference_id(
    result_id: str, result_version: int, item_result_id: str, knowledge_point_id: str
) -> str:
    """Deterministic identity of one item/knowledge-point outcome of one result version."""
    canonical = ":".join(
        (
            str(UUID(str(result_id))),
            str(int(result_version)),
            str(UUID(str(item_result_id))),
            str(UUID(str(knowledge_point_id))),
        )
    )
    return str(uuid5(FORMAL_EVIDENCE_NAMESPACE, canonical))


@dataclass(frozen=True)
class FormalProvenance:
    attempt_id: str
    result_id: str
    result_version: int
    item_result_id: str

    def encode(self) -> str:
        return ":".join(
            (
                _PROVENANCE_PREFIX,
                self.attempt_id,
                self.result_id,
                str(self.result_version),
                self.item_result_id,
            )
        )

    @classmethod
    def decode(cls, value: str) -> FormalProvenance | None:
        parts = str(value or "").split(":")
        if len(parts) != 5 or parts[0] != _PROVENANCE_PREFIX:
            return None
        try:
            return cls(
                attempt_id=str(UUID(parts[1])),
                result_id=str(UUID(parts[2])),
                result_version=int(parts[3]),
                item_result_id=str(UUID(parts[4])),
            )
        except ValueError:
            return None


def formal_provenance(evidence: LearningEvidence) -> FormalProvenance | None:
    """Provenance of evidence written by formal Assessment ingestion, else ``None``."""
    if evidence.source != FORMAL_EVIDENCE_SOURCE:
        return None
    return FormalProvenance.decode(evidence.session_id)


def formal_source_reference(evidence: LearningEvidence) -> str | None:
    if evidence.source != FORMAL_EVIDENCE_SOURCE:
        return None
    try:
        return str(UUID(evidence.turn_id))
    except ValueError:
        return None
