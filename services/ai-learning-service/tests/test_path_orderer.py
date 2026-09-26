"""Path creation orders once, preserves assessment behavior, and records a safe fallback."""

import asyncio
from contextlib import contextmanager
import copy
import json
import unittest
from unittest.mock import patch
from uuid import uuid4

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_orderer import PathOrderer
from app.application.path_service import PathService
from app.learning.deeptutor_llm import DeepTutorOrderingLlm, LlmProposal
from app.learning.path_ordering import PayloadTooLarge
from tests.formal_assessment_support import InMemoryLearningStore, event, item, mapping
from tests.test_path_ordering import (
    KP_A, KP_B, KP_C, MODULE_A, MODULE_B, TODAY, goal, ordering_curriculum, valid_proposal,
)


class StubLlm:
    def __init__(self, proposal=None, reason=None, store=None):
        self.result = LlmProposal(payload=proposal, reason=reason, model="test-ordering-model")
        self.calls = []
        self.store = store

    async def propose(self, system_prompt, payload):
        if self.store is not None:
            if self.store.open_transactions:
                raise AssertionError("LLM called while a path transaction was open")
            self.store.operations.append("llm")
        self.calls.append((system_prompt, payload))
        return self.result


class TrackingStore(InMemoryLearningStore):
    def __init__(self):
        super().__init__()
        self.open_transactions = 0
        self.operations = []

    @contextmanager
    def transaction(self, *args, **kwargs):
        self.operations.append("transaction")
        self.open_transactions += 1
        try:
            with super().transaction(*args, **kwargs) as tx:
                yield tx
        finally:
            self.open_transactions -= 1

    def pending_formal_payloads(self, user_id, goal_id):
        self.operations.append("pending_read")
        return super().pending_formal_payloads(user_id, goal_id)


class GoalClient:
    def __init__(self, data):
        self.data = data

    async def get_active_goal(self, _token):
        return dict(self.data)


class ContentClient:
    def __init__(self):
        _, scoped = ordering_curriculum()
        self.topics = scoped.topics
        self.points = scoped.knowledge_points

    async def get_curriculum(self, _token):
        return copy.deepcopy(self.topics), copy.deepcopy(self.points)


def ordered_ids(progress):
    return [(module.id, [point.id for point in module.knowledge_points]) for module in progress.modules]


CONTENT_ORDER = [(MODULE_A, [KP_A, KP_B]), (MODULE_B, [KP_C])]
LLM_ORDER = [(MODULE_B, [KP_C]), (MODULE_A, [KP_B, KP_A])]


class PathOrdererTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.goal = goal()
        self.modules, self.scoped = ordering_curriculum()

    def order(self, llm):
        return asyncio.run(PathOrderer(self.store, llm, today=lambda: TODAY).order(
            self.goal["userId"], self.goal, self.modules, self.scoped))

    def pending(self, observations, *, attempt_id=None, version=1, band=None,
                completed_at="2026-09-24T10:00:00Z", assessment_type="PLACEMENT"):
        payload = event(user_id=self.goal["userId"], goal_id=self.goal["id"],
                        attempt_id=attempt_id or str(uuid4()), result_version=version,
                        items=observations, assessment_type=assessment_type)
        payload["data"].update(overall_band=band, completed_at=completed_at)
        self.store.park_formal_result(FormalEvidenceAdapter.to_command(payload))
        return payload

    def test_newest_version_of_each_placement_attempt_is_used_and_bad_payloads_are_skipped(self):
        attempt_id = str(uuid4())
        self.pending([item([mapping(KP_A)], is_correct=False)], attempt_id=attempt_id, version=1, band=4.0,
                     completed_at="2026-09-26T10:00:00Z")
        self.pending([item([mapping(KP_A)], is_correct=True)], attempt_id=attempt_id, version=2, band=5.5)
        self.pending([item([mapping(KP_B)], is_correct=False)], band=6.0,
                     completed_at="2026-09-25T10:00:00Z")
        self.pending([item([mapping(KP_C)], is_correct=False)], band=9.0, assessment_type="QUIZ")
        self.store.pending[str(uuid4())] = {"user_id": self.goal["userId"], "learning_goal_id": self.goal["id"],
                                           "attempt_id": str(uuid4()), "result_version": 1,
                                           "payload": {"malformed": True}}
        llm = StubLlm(valid_proposal())

        outcome = self.order(llm)

        data = json.loads(llm.calls[0][1])
        placements = {point["id"]: point["placement"] for module in data["modules"]
                      for point in module["knowledge_points"]}
        self.assertEqual(placements, {KP_A: "correct", KP_B: "incorrect", KP_C: "not_tested"})
        self.assertEqual(data["learner"]["placement_band"], "6.0")
        self.assertEqual(outcome.learner_profile, {
            "target_level": "IELTS band 6.5", "time_budget": "45 minutes per day; 60 days until the exam",
            "prior_knowledge": "Placement band 6.0; 1 of 2 tested knowledge points answered correctly",
        })

    def test_profile_is_always_available_when_llm_is_not_configured(self):
        outcome = self.order(StubLlm(reason="llm_not_configured"))
        self.assertEqual(outcome.learner_profile, {
            "target_level": "IELTS band 6.5", "time_budget": "45 minutes per day; 60 days until the exam",
        })
        self.assertEqual(outcome.source, "content")

    def test_profile_omits_unknown_data_and_preserves_each_known_time_component(self):
        cases = [(None, None, None), (45, None, "45 minutes per day"),
                 (None, "2026-11-25", "60 days until the exam"),
                 (None, "2026-01-01", "0 days until the exam")]
        for minutes, exam_date, expected in cases:
            with self.subTest(minutes=minutes, exam_date=exam_date):
                self.goal.update(availableMinutesPerDay=minutes, examDate=exam_date)
                profile = self.order(StubLlm(reason="llm_not_configured")).learner_profile
                self.assertEqual(profile.get("time_budget"), expected)
                self.assertNotIn("prior_knowledge", profile)

    def test_profile_can_contain_only_the_placement_band_or_only_observations(self):
        self.pending([], band=5.0)
        self.assertEqual(self.order(StubLlm(reason="llm_timeout")).learner_profile["prior_knowledge"],
                         "Placement band 5.0")
        self.store.pending.clear()
        self.pending([item([mapping(KP_A)], is_correct=False)])
        self.assertEqual(self.order(StubLlm(reason="llm_timeout")).learner_profile["prior_knowledge"],
                         "0 of 1 tested knowledge points answered correctly")

    def test_each_llm_failure_preserves_the_content_order(self):
        for reason in ("llm_not_configured", "llm_timeout", "llm_error", "llm_unusable_response"):
            with self.subTest(reason=reason):
                outcome = self.order(StubLlm(reason=reason))
                self.assertEqual(outcome.modules, self.modules)
                self.assertEqual((outcome.source, outcome.reason, outcome.detail), ("content", reason, None))
                self.assertEqual(outcome.rationale, "")

    def test_too_large_payload_never_calls_llm_and_still_returns_profile(self):
        llm = StubLlm(valid_proposal())
        with patch("app.application.path_orderer.OrderingRequest.build", side_effect=PayloadTooLarge):
            outcome = self.order(llm)
        self.assertEqual((outcome.source, outcome.reason), ("content", "payload_too_large"))
        self.assertEqual(outcome.modules, self.modules)
        self.assertEqual(llm.calls, [])
        self.assertEqual(outcome.learner_profile["target_level"], "IELTS band 6.5")

    def test_invalid_proposal_returns_a_machine_readable_detail_without_the_response(self):
        outcome = self.order(StubLlm({"modules": [], "rationale": "private LLM response"}))
        self.assertEqual((outcome.source, outcome.reason, outcome.detail),
                         ("content", "invalid_ordering", "missing_module"))
        self.assertEqual(outcome.modules, self.modules)
        self.assertEqual(outcome.rationale, "")

    def test_rationale_is_optional_and_capped_at_five_hundred_characters(self):
        for rationale, expected in (("x" * 550, "x" * 500), (None, ""), (123, ""), (["text"], "")):
            with self.subTest(rationale=rationale):
                proposal = valid_proposal()
                proposal["rationale"] = rationale
                outcome = self.order(StubLlm(proposal))
                self.assertEqual((outcome.source, outcome.reason), ("llm", None))
                self.assertEqual(outcome.rationale, expected)
        proposal = valid_proposal()
        proposal.pop("rationale")
        self.assertEqual(self.order(StubLlm(proposal)).rationale, "")

    def test_database_read_errors_propagate_without_calling_llm(self):
        llm = StubLlm(valid_proposal())
        with patch.object(self.store, "pending_formal_payloads", side_effect=RuntimeError("database unavailable")):
            with self.assertRaisesRegex(RuntimeError, "database unavailable"):
                self.order(llm)
        self.assertEqual(llm.calls, [])


class OrderedPathCreationTest(unittest.TestCase):
    def setUp(self):
        self.store = TrackingStore()
        self.goal = goal()
        self.content = ContentClient()
        self.llm = StubLlm(valid_proposal(), store=self.store)
        self.paths = self.service(self.llm)

    def service(self, llm):
        return PathService(self.store, GoalClient(self.goal), self.content,
                           orderer=PathOrderer(self.store, llm, today=lambda: TODAY))

    def create(self):
        return asyncio.run(self.paths.ensure_active_path(self.goal["userId"], "private-bearer-token"))

    def ordering_events(self, path_id):
        return [payload for pid, _, name, payload in self.store.committed_events
                if pid == path_id and name == "path.ordered"]

    def test_creation_persists_llm_order_profile_and_safe_event_in_one_revision(self):
        path_id, progress = self.create()
        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertEqual([module.order for module in progress.modules], [0, 1])
        _, status = asyncio.run(self.paths.active_status(self.goal["userId"], "internal-token"))
        self.assertEqual(status["knowledgePointId"], KP_C)
        _, summary = asyncio.run(self.paths.active_progress(self.goal["userId"], "internal-token"))
        path_map = asyncio.run(self.paths.path_map(path_id, self.goal["userId"]))
        for mapped in (summary["mastery"], path_map["map"]):
            self.assertEqual([(module["id"], [point["id"] for point in module["knowledge_points"]])
                              for module in mapped["modules"]], LLM_ORDER)
        self.assertEqual(self.ordering_events(path_id), [{
            "source": "llm", "reason": None, "detail": None, "model": "test-ordering-model",
            "rationale": "Start with the learner's writing gap.", "module_count": 2, "knowledge_point_count": 3,
        }])
        profile = progress.learner_profile
        self.assertEqual(profile.target_level, "IELTS band 6.5")
        self.assertEqual(profile.time_budget, "45 minutes per day; 60 days until the exam")
        events = [(revision, name) for pid, revision, name, _ in self.store.committed_events if pid == path_id]
        names = [name for _, name in events]
        self.assertIn("path.learner_profile_recorded", names)
        self.assertLess(names.index("path.scope_applied"), names.index("path.ordered"))
        self.assertLess(names.index("path.ordered"), names.index("path.learner_profile_recorded"))
        self.assertEqual(len({revision for revision, _ in events}), 1)
        serialized = json.dumps(self.ordering_events(path_id)) + profile.model_dump_json()
        for private in (self.goal["email"], self.goal["name"], self.goal["userId"], self.goal["id"],
                        "private-bearer-token"):
            self.assertNotIn(private, serialized)

    def test_llm_call_finishes_before_any_path_transaction_opens(self):
        self.create()
        self.assertEqual(self.store.operations[:2], ["pending_read", "llm"])
        self.assertLess(self.store.operations.index("llm"), self.store.operations.index("transaction"))

    def test_post_paths_creation_also_orders_and_reports_all_new_points(self):
        path_id, progress, added = asyncio.run(self.paths.refresh_active_path(
            self.goal["userId"], "internal-token"))
        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertEqual(added, 3)
        self.assertEqual(len(self.llm.calls), 1)
        self.assertEqual(self.ordering_events(path_id)[0]["source"], "llm")

    def test_each_fallback_still_creates_a_path_and_records_the_reason(self):
        for reason in ("llm_not_configured", "llm_timeout", "llm_error", "llm_unusable_response",
                       "invalid_ordering", "payload_too_large"):
            with self.subTest(reason=reason):
                self.store = TrackingStore()
                llm = StubLlm({"modules": []} if reason == "invalid_ordering" else None,
                              reason=None if reason in ("invalid_ordering", "payload_too_large") else reason)
                self.paths = self.service(llm)
                if reason == "payload_too_large":
                    with patch("app.application.path_orderer.OrderingRequest.build", side_effect=PayloadTooLarge):
                        path_id, progress = self.create()
                    self.assertEqual(llm.calls, [])
                else:
                    path_id, progress = self.create()
                self.assertEqual(ordered_ids(progress), CONTENT_ORDER)
                event_data = self.ordering_events(path_id)[0]
                self.assertEqual((event_data["source"], event_data["reason"]), ("content", reason))
                self.assertEqual(progress.learner_profile.target_level, "IELTS band 6.5")

    def test_pending_placement_informs_prompt_and_is_still_applied_after_ordering(self):
        payload = event(user_id=self.goal["userId"], goal_id=self.goal["id"], attempt_id=str(uuid4()),
                        items=[item([mapping(KP_C)], is_correct=True), item([mapping(KP_B)], is_correct=False)],
                        assessment_type="PLACEMENT")
        payload["data"]["overall_band"] = 5.0
        self.store.park_formal_result(FormalEvidenceAdapter.to_command(payload))

        path_id, progress = self.create()

        data = json.loads(self.llm.calls[0][1])
        self.assertEqual(data["learner"]["placement_band"], "5.0")
        placements = {point["id"]: point["placement"] for module in data["modules"]
                      for point in module["knowledge_points"]}
        self.assertEqual(placements, {KP_A: "not_tested", KP_B: "incorrect", KP_C: "correct"})
        self.assertEqual(set(progress.learner_mastery_overrides), {KP_A, KP_C})
        self.assertEqual(len(progress.learning_evidence), 2)
        self.assertEqual(self.store.pending, {})
        _, status = asyncio.run(self.paths.active_status(self.goal["userId"], "internal-token"))
        self.assertEqual(status["knowledgePointId"], KP_B)
        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertTrue(any(pid == path_id and name == "placement.tested_out"
                            for pid, _, name, _ in self.store.committed_events))

    def test_existing_path_reads_and_refresh_do_not_call_llm_or_reset_created_order(self):
        path_id, _ = self.create()
        revision = self.store.load(path_id).version

        self.create()
        refreshed_id, progress, added = asyncio.run(self.paths.refresh_active_path(
            self.goal["userId"], "internal-token"))

        self.assertEqual(len(self.llm.calls), 1)
        self.assertEqual(refreshed_id, path_id)
        self.assertEqual(added, 0)
        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertEqual(progress.version, revision)
        self.assertEqual(len(self.ordering_events(path_id)), 1)

    def test_later_assessment_updates_evidence_mastery_and_status_without_reordering(self):
        path_id, _ = self.create()
        payload = event(user_id=self.goal["userId"], goal_id=self.goal["id"], attempt_id=str(uuid4()),
                        items=[item([mapping(KP_C)], is_correct=True)])

        outcome = FormalAssessmentIngestionService(self.store, self.paths).ingest(
            FormalEvidenceAdapter.to_command(payload))

        self.assertEqual(outcome.status, "applied")
        progress = self.store.load(path_id)
        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertEqual(len(progress.learning_evidence), 1)
        self.assertGreater(progress.mastery_levels[KP_C], 0)
        _, status = asyncio.run(self.paths.active_status(self.goal["userId"], "internal-token"))
        self.assertEqual(status["revision"], progress.version)
        self.assertEqual(status["knowledgePointId"], KP_C)
        self.assertEqual(len(self.llm.calls), 1)

    def test_no_orderer_retains_the_existing_creation_contract(self):
        self.paths = PathService(self.store, GoalClient(self.goal), self.content)
        path_id, progress = self.create()
        self.assertEqual(ordered_ids(progress), CONTENT_ORDER)
        self.assertIsNone(progress.learner_profile)
        self.assertEqual(self.ordering_events(path_id), [])

    def test_path_creation_uses_the_real_deeptutor_llm_with_a_local_server(self):
        from tests.deeptutor_llm_support import OpenAiStub, TEST_MODEL, isolated_llm_catalog

        with OpenAiStub(content=json.dumps(valid_proposal())) as server, isolated_llm_catalog(server.base_url):
            self.paths = self.service(DeepTutorOrderingLlm())
            path_id, progress = self.create()

        self.assertEqual(ordered_ids(progress), LLM_ORDER)
        self.assertEqual(self.ordering_events(path_id)[0]["model"], TEST_MODEL)
        self.assertEqual(len(server.requests), 1)
