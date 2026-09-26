"""Path ordering through real DeepTutor HTTP and disposable PostgreSQL schemas."""

import asyncio
import json
import unittest
from unittest.mock import patch
from uuid import uuid4

import psycopg2

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_orderer import PathOrderer
from app.application.path_service import PathService
from app.learning.deeptutor_llm import DeepTutorOrderingLlm
from app.persistence.postgres_learning_store import PostgresLearningStore
from tests.deeptutor_llm_support import OpenAiStub, TEST_API_KEY, TEST_MODEL, isolated_llm_catalog
from tests.formal_assessment_support import event, item, mapping
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_path_orderer import CONTENT_ORDER, LLM_ORDER, ContentClient, GoalClient, ordered_ids
from tests.test_path_ordering import KP_A, KP_B, KP_C, TODAY, goal, valid_proposal


class PathOrderingPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def service(self, learner_goal, llm=None):
        store = PostgresLearningStore(self.schema.url)
        return PathService(store, GoalClient(learner_goal), ContentClient(),
                           orderer=PathOrderer(store, llm or DeepTutorOrderingLlm(), today=lambda: TODAY))

    def park(self, learner_goal, *, attempt_id=None, version=1):
        payload = event(user_id=learner_goal["userId"], goal_id=learner_goal["id"],
                        attempt_id=attempt_id or str(uuid4()), result_version=version,
                        items=[item([mapping(KP_C)], is_correct=True), item([mapping(KP_B)], is_correct=False)],
                        assessment_type="PLACEMENT")
        payload["data"]["overall_band"] = 5.0
        command = FormalEvidenceAdapter.to_command(payload)
        store = PostgresLearningStore(self.schema.url)
        outcome = FormalAssessmentIngestionService(store, PathService(store)).ingest(command)
        self.assertEqual(outcome.status, "pending")
        return payload

    def ordering_events(self, path_id):
        return self.schema.query("SELECT revision, payload_json FROM mastery_events "
                                 "WHERE path_id = %s AND event_type = 'path.ordered'", (path_id,))

    def test_pending_payloads_read_without_a_path_transaction_and_remain_tenant_scoped(self):
        learner = goal()
        second_learner = goal()
        another_goal = dict(learner, id=str(uuid4()))
        attempt = str(uuid4())
        newer = self.park(learner, attempt_id=attempt, version=2)
        older = self.park(learner, attempt_id=attempt, version=1)
        self.park(second_learner)
        self.park(another_goal)
        store = PostgresLearningStore(self.schema.url)

        payloads = store.pending_formal_payloads(learner["userId"], learner["id"])

        self.assertEqual(payloads, [older, newer])
        self.assertIsNone(store.find_path(learner["userId"], learner["id"]))
        self.assertEqual(len(self.schema.query("SELECT event_id FROM pending_formal_assessment_results "
                                              "WHERE user_id = %s AND learning_goal_id = %s",
                                              (learner["userId"], learner["id"]))), 2)

    def test_real_ordering_two_learners_and_pending_placement_survive_postgres_reload(self):
        with_placement, without_placement = goal(), goal()
        self.park(with_placement)
        path_ids = []
        with OpenAiStub(content=json.dumps(valid_proposal())) as server, isolated_llm_catalog(server.base_url):
            for learner in (with_placement, without_placement):
                path_id, _ = asyncio.run(self.service(learner).ensure_active_path(learner["userId"], "internal-token"))
                path_ids.append(path_id)
                progress = PostgresLearningStore(self.schema.url).get_owned_progress(path_id, learner["userId"])
                self.assertEqual(ordered_ids(progress), LLM_ORDER)
                events = self.ordering_events(path_id)
                self.assertEqual(len(events), 1)
                self.assertEqual(events[0][0], progress.version)
                self.assertEqual((events[0][1]["source"], events[0][1]["model"]), ("llm", TEST_MODEL))
                self.assertNotIn(TEST_API_KEY, json.dumps(events))
                reloaded = self.service(learner)
                _, status = asyncio.run(reloaded.active_status(learner["userId"], "internal-token"))
                expected_next = KP_B if learner is with_placement else KP_C
                self.assertEqual(status["knowledgePointId"], expected_next)
                _, refreshed, added = asyncio.run(reloaded.refresh_active_path(learner["userId"], "internal-token"))
                self.assertEqual((ordered_ids(refreshed), added, refreshed.version), (LLM_ORDER, 0, progress.version))
        self.assertEqual(len(server.requests), 2)
        self.assertNotEqual(*path_ids)
        first = PostgresLearningStore(self.schema.url).get_owned_progress(path_ids[0], with_placement["userId"])
        self.assertEqual(set(first.learner_mastery_overrides), {KP_A, KP_C})
        self.assertEqual(len(first.learning_evidence), 2)
        self.assertIn("Placement band 5.0", first.learner_profile.prior_knowledge)
        self.assertEqual(self.schema.query("SELECT count(*) FROM pending_formal_assessment_results "
                                           "WHERE user_id = %s AND learning_goal_id = %s",
                                           (with_placement["userId"], with_placement["id"]))[0][0], 0)
        first_prompt = json.loads(server.requests[0]["body"]["messages"][-1]["content"])
        self.assertEqual(first_prompt["learner"]["placement_band"], "5.0")
        for learner, request in zip((with_placement, without_placement), server.requests):
            prompt = json.dumps(request["body"])
            for private in (learner["userId"], learner["id"], learner["email"], learner["name"], "internal-token"):
                self.assertNotIn(private, prompt)

    def test_database_connections_are_closed_before_real_http_call(self):
        learner = goal()
        connections, states_at_request = [], []
        real_connect = psycopg2.connect

        def connect(*args, **kwargs):
            connection = real_connect(*args, **kwargs)
            connections.append(connection)
            return connection

        def observe_request(_request):
            states_at_request.extend(connection.closed for connection in connections)

        with OpenAiStub(content=json.dumps(valid_proposal()), on_request=observe_request) as server:
            with isolated_llm_catalog(server.base_url), patch("psycopg2.connect", side_effect=connect):
                path_id, _ = asyncio.run(self.service(learner).ensure_active_path(learner["userId"], "internal-token"))
        self.assertTrue(states_at_request)
        self.assertTrue(all(states_at_request), "A PostgreSQL connection remained open during LLM I/O")
        self.assertEqual(self.ordering_events(path_id)[0][1]["source"], "llm")

    def test_two_concurrent_creators_commit_one_path_and_one_ordering_event(self):
        learner = goal()

        async def race():
            ready = asyncio.Event()
            arrivals = 0
            delegate = DeepTutorOrderingLlm()

            class SynchronizedLlm:
                async def propose(self, system_prompt, payload):
                    nonlocal arrivals
                    arrivals += 1
                    if arrivals == 2:
                        ready.set()
                    await asyncio.wait_for(ready.wait(), timeout=5)
                    return await delegate.propose(system_prompt, payload)

            services = [self.service(learner, SynchronizedLlm()) for _ in range(2)]
            return await asyncio.gather(*(service.ensure_active_path(learner["userId"], "internal-token")
                                          for service in services))

        with OpenAiStub(content=json.dumps(valid_proposal())) as server, isolated_llm_catalog(server.base_url):
            results = asyncio.run(race())
        self.assertEqual(results[0][0], results[1][0])
        self.assertEqual(len(server.requests), 2)
        self.assertEqual(len(self.ordering_events(results[0][0])), 1)
        self.assertEqual(self.schema.query("SELECT count(*) FROM mastery_paths WHERE user_id = %s "
                                           "AND learning_goal_id = %s", (learner["userId"], learner["id"]))[0][0], 1)
        self.assertEqual(ordered_ids(results[0][1]), LLM_ORDER)
        self.assertEqual(ordered_ids(results[1][1]), LLM_ORDER)

    def test_real_provider_failures_still_persist_content_order_and_profile(self):
        unknown = valid_proposal()
        unknown["modules"][0]["knowledge_point_ids"].append("unknown-kp")
        cases = [
            ("invalid_ordering", {"content": json.dumps(unknown)}, True),
            ("llm_timeout", {"delay_seconds": 0.2}, True),
            ("llm_error", {"status": 500}, True),
            ("llm_not_configured", {}, False),
            ("llm_unusable_response", {"content": "not JSON"}, True),
        ]
        for reason, server_kwargs, configured in cases:
            with self.subTest(reason=reason):
                learner = goal()
                with OpenAiStub(**server_kwargs) as server, isolated_llm_catalog(server.base_url, configured=configured):
                    timeout = 0.03 if reason == "llm_timeout" else 20.0
                    service = self.service(learner, DeepTutorOrderingLlm(timeout_seconds=timeout))
                    path_id, _ = asyncio.run(service.ensure_active_path(learner["userId"], "internal-token"))
                progress = PostgresLearningStore(self.schema.url).get_owned_progress(path_id, learner["userId"])
                self.assertEqual(ordered_ids(progress), CONTENT_ORDER)
                event_data = self.ordering_events(path_id)[0][1]
                self.assertEqual((event_data["source"], event_data["reason"]), ("content", reason))
                self.assertEqual(progress.learner_profile.target_level, "IELTS band 6.5")
                if reason == "invalid_ordering":
                    self.assertEqual(event_data["detail"], "unknown_knowledge_point")
                if reason == "llm_not_configured":
                    self.assertEqual(server.requests, [])
                self.assertNotIn(TEST_API_KEY, json.dumps(event_data))
