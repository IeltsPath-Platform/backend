"""Learner API DTOs must only expose the allowlisted policy summary."""

import unittest
from uuid import UUID

from app.api.dto.responses import LearningProgressResponse


class ApiResponseDtoTest(unittest.TestCase):
    def test_progress_response_serializes_public_camel_case_fields(self):
        payload = {
            "pathId": "4ed3d7e1-7529-4572-921d-2e54403f7d7b",
            "revision": 1,
            "moduleCount": 1,
            "knowledgePointCount": 1,
            "mastery": {
                "name": "Writing",
                "counts": {"mastered": 0, "learning": 0, "new": 1, "total": 1},
                "due_reviews": 0,
                "complete": False,
                "modules": [
                    {
                        "id": "4ed3d7e1-7529-4572-921d-2e54403f7d7b",
                        "name": "Writing",
                        "objective": "",
                        "order": 0,
                        "mastered": 0,
                        "total": 1,
                        "knowledge_points": [
                            {
                                "id": "c5b2641f-28c8-467d-9d64-f52c8bdc15a1",
                                "name": "Task response",
                                "type": "concept",
                                "status": "new",
                                "mastery": 0.0,
                                "mastery_source": "none",
                                "override_note": "",
                            }
                        ],
                    }
                ],
            },
        }

        response = LearningProgressResponse.model_validate(payload)
        output = response.model_dump(by_alias=True)

        self.assertIsInstance(response.path_id, UUID)
        self.assertIn("knowledgePoints", output["mastery"]["modules"][0])
        self.assertIn("masterySource", output["mastery"]["modules"][0]["knowledgePoints"][0])
        self.assertNotIn("state_json", output)

    def test_response_models_reject_unreviewed_fields(self):
        with self.assertRaises(ValueError):
            LearningProgressResponse.model_validate({"pathId": "4ed3d7e1-7529-4572-921d-2e54403f7d7b", "internal": {}})


if __name__ == "__main__":
    unittest.main()
