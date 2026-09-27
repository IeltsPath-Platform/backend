"""POST /paths refreshes an existing path from Content, adding and never removing learner state."""

import asyncio
import unittest
from uuid import uuid4

from app.mastery.policy import next_objective

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.learning.override_provenance import mastery_source
from app.mastery.service import LearningService
from app.tutor.tools import TutorTools

from tests.formal_assessment_support import InMemoryLearningStore, event, item, mapping

TOPIC_A = "4ed3d7e1-7529-4572-921d-2e54403f7da1"
TOPIC_B = "4ed3d7e1-7529-4572-921d-2e54403f7da2"
KP_1 = "c5b2641f-28c8-467d-9d64-f52c8bdc1a01"
KP_2 = "c5b2641f-28c8-467d-9d64-f52c8bdc1a02"
KP_NEW = "c5b2641f-28c8-467d-9d64-f52c8bdc1a03"
KP_A2 = "c5b2641f-28c8-467d-9d64-f52c8bdc1a04"


class GoalClient:
    def __init__(self, goal):
        self.goal = goal

    async def get_active_goal(self, _bearer_token):
        return dict(self.goal)


class EditableContent:
    """Content stub whose curriculum a test edits between calls."""

    def __init__(self):
        self.topics = [{"id": TOPIC_A, "name": "Topic A", "sortOrder": 0, "status": "ACTIVE"},
                       {"id": TOPIC_B, "name": "Topic B", "sortOrder": 1, "status": "ACTIVE"}]
        self.points = [self.point(KP_1, TOPIC_A), self.point(KP_2, TOPIC_B)]

    @staticmethod
    def point(kp_id, topic_id, band_min=None, name=None):
        return {"id": kp_id, "topicId": topic_id, "name": name or kp_id[-4:], "learningType": "PROCEDURE",
                "status": "ACTIVE", "effectiveBandMin": band_min, "effectiveBandMax": None,
                "skill": None, "description": ""}

    async def get_curriculum(self, _bearer_token):
        return [dict(t) for t in self.topics], [dict(p) for p in self.points]


def kp_ids(progress):
    return [kp.id for module in progress.modules for kp in module.knowledge_points]


class PathRefreshTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.user_id, self.goal_id = str(uuid4()), str(uuid4())
        self.content = EditableContent()
        self.content.points[0]["skill"] = "writing"
        self.content.points[0]["description"] = "d" * 1200
        self.content.points[1]["description"] = "Second point description"
        self.paths = PathService(self.store, GoalClient(
            {"id": self.goal_id, "userId": self.user_id, "status": "ACTIVE", "targetBand": 6.0}), self.content)
        self.path_id, _, self.created_added = self.refresh()

    def refresh(self):
        return asyncio.run(self.paths.refresh_active_path(self.user_id, "internal-token"))

    def record(self, kp_id, *, correct=True):
        payload = event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=str(uuid4()),
                        items=[item([mapping(kp_id)], is_correct=correct)])
        FormalAssessmentIngestionService(self.store, self.paths).ingest(FormalEvidenceAdapter.to_command(payload))

    def progress(self):
        return self.store.load(self.path_id)

    def kp(self, kp_id):
        return next(kp for module in self.progress().modules for kp in module.knowledge_points if kp.id == kp_id)

    def test_creating_the_path_reports_every_point_as_added(self):
        self.assertEqual(self.created_added, 2)

    def test_a_new_point_in_scope_is_added_and_existing_state_is_kept(self):
        self.record(KP_1)
        mastery_before = self.progress().mastery_levels[KP_1]
        self.content.points.append(EditableContent.point(KP_NEW, TOPIC_A))

        _, progress, added = self.refresh()

        self.assertEqual(added, 1)
        self.assertEqual(kp_ids(progress), [KP_1, KP_NEW, KP_2])
        self.assertEqual(progress.mastery_levels[KP_1], mastery_before)

    def test_an_unchanged_curriculum_changes_nothing(self):
        revision = self.progress().version
        events = len(self.store.committed_events)

        _, _, added = self.refresh()

        self.assertEqual((added, self.progress().version, len(self.store.committed_events)), (0, revision, events))

    def test_creation_snapshots_details_for_the_points_in_scope(self):
        with self.store.transaction(self.path_id):
            details = self.store.knowledge_point_details(self.path_id)

        self.assertEqual(set(details), {KP_1, KP_2})
        self.assertEqual((details[KP_1].skill, details[KP_1].description), ("writing", "d" * 1000))
        self.assertEqual((details[KP_2].skill, details[KP_2].description), (None, "Second point description"))

    def test_metadata_change_commits_a_revision_and_unchanged_refresh_does_not(self):
        initial_revision = self.progress().version
        self.content.points[0]["description"] = "Updated curriculum guidance"
        self.content.points[0]["skill"] = "speaking"

        _, changed, _ = self.refresh()

        self.assertEqual(changed.version, initial_revision + 1)
        with self.store.transaction(self.path_id):
            snapshot = self.store.knowledge_point_details(self.path_id)[KP_1]
        self.assertEqual((snapshot.skill, snapshot.description), ("speaking", "Updated curriculum guidance"))

        _, unchanged, added = self.refresh()

        self.assertEqual((added, unchanged.version), (0, changed.version))

    def test_a_renamed_point_is_renamed_in_the_path(self):
        self.content.points[0] = EditableContent.point(KP_1, TOPIC_A, name="Renamed")

        self.refresh()

        self.assertEqual(self.kp(KP_1).name, "Renamed")

    def test_a_point_that_left_the_curriculum_is_retired_not_removed(self):
        self.record(KP_1, correct=False)
        with self.store.transaction(self.path_id):
            original_details = self.store.knowledge_point_details(self.path_id)[KP_1]
        self.content.points = [p for p in self.content.points if p["id"] != KP_1]

        _, progress, _ = self.refresh()

        self.assertIn(KP_1, kp_ids(progress))
        self.assertEqual([a.knowledge_point_id for a in progress.quiz_attempts], [KP_1])
        self.assertEqual(mastery_source(progress, self.kp(KP_1)), "retired")
        self.assertEqual(next_objective(progress).knowledge_point_id, KP_2)
        with self.store.transaction(self.path_id):
            self.assertEqual(self.store.knowledge_point_details(self.path_id)[KP_1], original_details)

    def test_a_point_whose_band_rose_above_the_target_is_retired(self):
        self.content.points[0] = EditableContent.point(KP_1, TOPIC_A, band_min=7.0)

        _, progress, _ = self.refresh()

        self.assertEqual(mastery_source(progress, self.kp(KP_1)), "retired")

    def test_a_retired_point_that_returns_is_learnable_again(self):
        removed = self.content.points.pop(0)
        self.refresh()
        self.content.points.insert(0, removed)

        _, progress, _ = self.refresh()

        self.assertEqual(mastery_source(progress, self.kp(KP_1)), "")
        self.assertEqual(next_objective(progress).knowledge_point_id, KP_1)

    def test_a_topic_that_left_keeps_its_points_in_their_original_position(self):
        self.content.topics = [self.content.topics[1]]
        self.content.points = [self.content.points[1], EditableContent.point(KP_NEW, TOPIC_B)]

        _, progress, added = self.refresh()

        self.assertEqual(added, 1)
        self.assertEqual([m.id for m in progress.modules], [TOPIC_A, TOPIC_B])
        self.assertEqual(kp_ids(progress), [KP_1, KP_2, KP_NEW])
        self.assertEqual([m.order for m in progress.modules], [0, 1])

    def test_content_reordering_does_not_change_the_existing_path_order(self):
        self.content.topics.reverse()
        revision = self.progress().version

        _, progress, added = self.refresh()

        self.assertEqual([module.id for module in progress.modules], [TOPIC_A, TOPIC_B])
        self.assertEqual([module.order for module in progress.modules], [0, 1])
        self.assertEqual(kp_ids(progress), [KP_1, KP_2])
        self.assertEqual((added, progress.version), (0, revision))

    def test_refresh_keeps_learner_order_and_appends_new_point(self):
        self.content.points.append(EditableContent.point(KP_A2, TOPIC_A))
        self.refresh()
        tools = TutorTools(LearningService(self.store), self.path_id, session_id="session", turn_id="turn")
        reordered = tools.execute("path_reorder", {
            "module_ids": [TOPIC_B, TOPIC_A],
            "knowledge_points": [{"module_id": TOPIC_A, "knowledge_point_ids": [KP_A2, KP_1]}],
        })
        self.assertEqual(reordered.result["status"], "reordered")
        revision = self.progress().version

        _, unchanged, added = self.refresh()

        self.assertEqual((added, unchanged.version), (0, revision))
        self.assertEqual([module.id for module in unchanged.modules], [TOPIC_B, TOPIC_A])
        self.assertEqual(kp_ids(unchanged), [KP_2, KP_A2, KP_1])

        self.content.points.append(EditableContent.point(KP_NEW, TOPIC_A))
        _, updated, added = self.refresh()

        self.assertEqual((added, updated.version), (1, revision + 1))
        self.assertEqual([module.id for module in updated.modules], [TOPIC_B, TOPIC_A])
        self.assertEqual(kp_ids(updated), [KP_2, KP_A2, KP_1, KP_NEW])

    def test_new_points_append_even_when_their_content_creation_date_sorts_first(self):
        self.content.points[0]["createdAt"] = "2026-09-26"
        new_point = EditableContent.point(KP_NEW, TOPIC_A)
        new_point["createdAt"] = "2020-01-01"
        self.content.points.insert(0, new_point)

        _, progress, added = self.refresh()

        self.assertEqual(added, 1)
        self.assertEqual(kp_ids(progress), [KP_1, KP_NEW, KP_2])

    def test_new_modules_append_even_when_content_puts_them_first(self):
        new_topic = str(uuid4())
        self.content.topics.insert(0, {"id": new_topic, "name": "New topic", "sortOrder": -1})
        self.content.points.insert(0, EditableContent.point(KP_NEW, new_topic))

        _, progress, added = self.refresh()

        self.assertEqual(added, 1)
        self.assertEqual([module.id for module in progress.modules], [TOPIC_A, TOPIC_B, new_topic])
        self.assertEqual([module.order for module in progress.modules], [0, 1, 2])
        self.assertEqual(kp_ids(progress), [KP_1, KP_2, KP_NEW])

    def test_a_point_moved_by_content_appends_to_its_new_module_and_keeps_its_history(self):
        self.record(KP_1)
        mastery = self.progress().mastery_levels[KP_1]
        self.content.points[0]["topicId"] = TOPIC_B

        _, progress, added = self.refresh()

        self.assertEqual(added, 0)
        self.assertEqual([point.id for point in progress.modules[0].knowledge_points], [])
        self.assertEqual([point.id for point in progress.modules[1].knowledge_points], [KP_2, KP_1])
        self.assertEqual(progress.modules[1].knowledge_points[1].module_id, TOPIC_B)
        self.assertEqual(progress.mastery_levels[KP_1], mastery)
        self.assertEqual([attempt.knowledge_point_id for attempt in progress.quiz_attempts], [KP_1])

    def test_a_refresh_is_one_revision_with_a_scope_refreshed_event(self):
        revision = self.progress().version
        self.content.points.append(EditableContent.point(KP_NEW, TOPIC_B))

        self.refresh()

        self.assertEqual(self.progress().version, revision + 1)
        refreshed = [payload for pid, _, name, payload in self.store.committed_events
                     if pid == self.path_id and name == "path.scope_refreshed"]
        self.assertEqual(refreshed, [{"target_band": "6.0", "added": [KP_NEW], "retired": [], "restored": []}])

    def test_get_endpoints_still_never_read_content_for_an_existing_path(self):
        self.content.points.append(EditableContent.point(KP_NEW, TOPIC_A))

        _, progress = asyncio.run(self.paths.ensure_active_path(self.user_id, "internal-token"))

        self.assertNotIn(KP_NEW, kp_ids(progress))


if __name__ == "__main__":
    unittest.main()
