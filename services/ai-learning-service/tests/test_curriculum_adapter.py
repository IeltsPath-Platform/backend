"""Tests for the canonical Content Service to DeepTutor curriculum boundary."""

import unittest

from deeptutor.learning.models import KnowledgeType

from app.adapters.curriculum_adapter import CurriculumAdapter, CurriculumContractError


TOPIC_ID = "4ed3d7e1-7529-4572-921d-2e54403f7d7b"


class CurriculumAdapterTest(unittest.TestCase):
    def test_maps_canonical_ids_and_all_supported_learning_types(self):
        topics = [{"id": TOPIC_ID, "name": "Writing", "sortOrder": 1, "status": "ACTIVE"}]
        points = [
            {
                "id": f"4ed3d7e1-7529-4572-921d-2e54403f7d7{i}",
                "topicId": TOPIC_ID,
                "name": learning_type,
                "learningType": learning_type,
                "status": "ACTIVE",
            }
            for i, learning_type in enumerate(("MEMORY", "CONCEPT", "PROCEDURE", "DESIGN"))
        ]

        modules = CurriculumAdapter.to_modules(topics, points)

        self.assertEqual(modules[0].id, TOPIC_ID)
        self.assertEqual(
            [point.type for point in modules[0].knowledge_points],
            [KnowledgeType.MEMORY, KnowledgeType.CONCEPT, KnowledgeType.PROCEDURE, KnowledgeType.DESIGN],
        )
        self.assertEqual([point.id for point in modules[0].knowledge_points], [point["id"] for point in points])

    def test_rejects_missing_or_unknown_learning_type(self):
        topics = [{"id": TOPIC_ID, "name": "Writing", "status": "ACTIVE"}]
        point = {
            "id": "c5b2641f-28c8-467d-9d64-f52c8bdc15a1",
            "topicId": TOPIC_ID,
            "name": "Task response",
            "learningType": "UNCLASSIFIED",
        }

        with self.assertRaises(CurriculumContractError):
            CurriculumAdapter.to_modules(topics, [point])

    def test_rejects_empty_active_curriculum(self):
        with self.assertRaises(CurriculumContractError):
            CurriculumAdapter.to_modules([], [])

    def test_preserves_topic_preorder_and_uses_available_kp_creation_order(self):
        child_id = "0e7d3d36-e9c1-42b9-bec5-b6a981217658"
        second_topic_id = "f38d0a21-6281-4bc2-9b4c-e871f665844d"
        topics = [
            {"id": TOPIC_ID, "name": "Parent", "sortOrder": 0},
            {"id": child_id, "name": "Child", "sortOrder": 1},
            {"id": second_topic_id, "name": "Second root", "sortOrder": 0},
        ]
        points = [
            {
                "id": "c5b2641f-28c8-467d-9d64-f52c8bdc15a1",
                "topicId": TOPIC_ID,
                "name": "Later",
                "learningType": "CONCEPT",
                "createdAt": "2026-09-02T00:00:00Z",
            },
            {
                "id": "0a1aa73c-6d98-4a3e-98db-f45ef18117a3",
                "topicId": TOPIC_ID,
                "name": "Earlier",
                "learningType": "CONCEPT",
                "createdAt": "2026-09-01T00:00:00Z",
            },
        ]

        modules = CurriculumAdapter.to_modules(topics, points)

        self.assertEqual([module.id for module in modules], [TOPIC_ID, child_id, second_topic_id])
        self.assertEqual([point.name for point in modules[0].knowledge_points], ["Earlier", "Later"])


if __name__ == "__main__":
    unittest.main()
