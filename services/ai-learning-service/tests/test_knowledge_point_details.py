"""Content knowledge point snapshots, and goal-less results while paths are still keyed by goal."""

import unittest
from unittest import mock
from uuid import uuid4

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.adapters.knowledge_point_details import KnowledgePointDetails, details_from_content
from app.application.formal_assessment_ingestion import (
    FormalAssessmentIngestionService,
    GoallessResultUnsupported,
)
from tests.formal_assessment_support import event, item, mapping


class KnowledgePointDetailsTest(unittest.TestCase):
    def test_snapshot_reads_skill_description_and_practice_set_flag(self):
        details = details_from_content({"skill": "READING", "description": "d" * 1200, "hasPracticeSet": True})
        self.assertEqual(details, KnowledgePointDetails("READING", "d" * 1000, True))

    def test_missing_fields_take_empty_values(self):
        self.assertEqual(details_from_content({}), KnowledgePointDetails(None, "", False))
        self.assertFalse(details_from_content({"hasPracticeSet": "true"}).has_practice_set)


class GoallessResultTest(unittest.TestCase):
    def test_goalless_result_is_retried_rather_than_applied_to_some_path(self):
        payload = event(user_id=str(uuid4()), goal_id=str(uuid4()), attempt_id=str(uuid4()),
                        items=[item([mapping(str(uuid4()))], is_correct=True)])
        payload["data"]["learning_goal_id"] = None
        command = FormalEvidenceAdapter.to_command(payload)
        paths = mock.Mock()
        store = mock.Mock()

        with self.assertRaises(GoallessResultUnsupported):
            FormalAssessmentIngestionService(store, paths).ingest(command)

        paths.ensure_path.assert_not_called()
        store.park_formal_result.assert_not_called()
