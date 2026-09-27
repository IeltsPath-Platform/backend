import json
import unittest
from types import SimpleNamespace
from uuid import uuid4

from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.messaging.assessment_consumer import FAILURE_HEADER, AssessmentCompletedConsumer
from app.messaging.topology import AssessmentCompletedTopology

from tests.formal_assessment_support import VOCABULARY_KP, InMemoryLearningStore, curriculum, event, item, mapping


class FakeChannel:
    def __init__(self, *, fail_ack_once=False, fail_publish=False):
        self.acks, self.nacks, self.published = [], [], []
        self._fail_ack_once = fail_ack_once
        self._fail_publish = fail_publish

    def basic_ack(self, delivery_tag):
        if self._fail_ack_once:
            self._fail_ack_once = False
            raise ConnectionError("channel closed before ACK")
        self.acks.append(delivery_tag)

    def basic_nack(self, delivery_tag, requeue):
        self.nacks.append((delivery_tag, requeue))

    def basic_publish(self, exchange, routing_key, body, properties, mandatory):
        if self._fail_publish:
            raise ConnectionError("broker unavailable")
        self.published.append((exchange, routing_key, body, properties))


class FailingIngestion:
    def ingest(self, command):
        raise ConnectionError("database unavailable")


def properties(*, rejected_before=0):
    headers = {}
    if rejected_before:
        headers["x-death"] = [{"queue": AssessmentCompletedTopology().queue, "reason": "rejected",
                               "count": rejected_before}]
    return SimpleNamespace(message_id=str(uuid4()), content_type="application/json",
                           type="AssessmentCompleted.v2", headers=headers)


class AssessmentCompletedConsumerTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        paths = PathService(self.store)
        self.user_id, self.goal_id = str(uuid4()), str(uuid4())
        self.path_id, _ = paths.ensure_path(self.user_id, self.goal_id, curriculum())
        self.topology = AssessmentCompletedTopology()
        self.consumer = AssessmentCompletedConsumer(
            FormalAssessmentIngestionService(self.store, paths), self.topology, max_delivery_attempts=3)

    def body(self):
        return json.dumps(event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=str(uuid4()),
                                items=[item([mapping(VOCABULARY_KP)], is_correct=True)])).encode()

    def attempts(self):
        return len(self.store.load(self.path_id).quiz_attempts)

    def test_committed_ingestion_is_acknowledged(self):
        channel = FakeChannel()

        self.consumer.on_message(channel, SimpleNamespace(delivery_tag=1), properties(), self.body())

        self.assertEqual(channel.acks, [1])
        self.assertEqual(self.attempts(), 1)

    def test_result_for_a_goal_without_a_path_is_parked_and_acknowledged(self):
        channel = FakeChannel()
        body = json.dumps(event(user_id=self.user_id, goal_id=str(uuid4()), attempt_id=str(uuid4()),
                                items=[item([mapping(VOCABULARY_KP)], is_correct=True)])).encode()

        with self.assertLogs("app.messaging.assessment_consumer", level="INFO") as logs:
            self.consumer.on_message(channel, SimpleNamespace(delivery_tag=6), properties(), body)

        self.assertEqual(channel.acks, [6])
        self.assertEqual((channel.nacks, channel.published), ([], []))
        self.assertEqual(len(self.store.pending), 1)
        self.assertIn("pending", "\n".join(logs.output))

    def test_database_failure_is_not_acknowledged_and_goes_to_the_retry_queue(self):
        consumer = AssessmentCompletedConsumer(FailingIngestion(), self.topology, max_delivery_attempts=3)
        channel = FakeChannel()

        consumer.on_message(channel, SimpleNamespace(delivery_tag=7), properties(rejected_before=1), self.body())

        self.assertEqual(channel.acks, [])
        self.assertEqual(channel.nacks, [(7, False)])
        self.assertEqual(channel.published, [])

    def test_exhausted_retries_are_parked_in_the_dead_letter_queue(self):
        consumer = AssessmentCompletedConsumer(FailingIngestion(), self.topology, max_delivery_attempts=3)
        channel = FakeChannel()

        consumer.on_message(channel, SimpleNamespace(delivery_tag=9), properties(rejected_before=2), self.body())

        self.assertEqual(channel.acks, [9])
        (exchange, routing_key, _, props), = channel.published
        self.assertEqual((exchange, routing_key), (self.topology.dead_letter_exchange, self.topology.dead_letter_queue))
        self.assertIn("exhausted retries", props.headers[FAILURE_HEADER])

    def test_invalid_contract_is_dead_lettered_without_retry(self):
        channel = FakeChannel()
        payload = json.loads(self.body())
        del payload["data"]["learning_goal_id"]

        self.consumer.on_message(channel, SimpleNamespace(delivery_tag=3), properties(), json.dumps(payload).encode())

        self.assertEqual(channel.acks, [3])
        self.assertEqual(channel.nacks, [])
        self.assertEqual(channel.published[0][1], self.topology.dead_letter_queue)
        self.assertEqual(self.attempts(), 0)

    def test_undecodable_body_is_dead_lettered(self):
        channel = FakeChannel()

        self.consumer.on_message(channel, SimpleNamespace(delivery_tag=4), properties(), b"not json")

        self.assertEqual(channel.acks, [4])
        self.assertEqual(channel.published[0][1], self.topology.dead_letter_queue)

    def test_message_that_cannot_be_parked_is_retried_instead_of_dropped(self):
        channel = FakeChannel(fail_publish=True)

        self.consumer.on_message(channel, SimpleNamespace(delivery_tag=5), properties(), b"not json")

        self.assertEqual(channel.acks, [])
        self.assertEqual(channel.nacks, [(5, False)])

    def test_redelivery_after_commit_but_failed_ack_is_idempotent(self):
        body = self.body()
        first = FakeChannel(fail_ack_once=True)
        with self.assertRaises(ConnectionError):
            self.consumer.on_message(first, SimpleNamespace(delivery_tag=1), properties(), body)
        revision_after_commit = self.store.load(self.path_id).version

        redelivered = FakeChannel()
        self.consumer.on_message(redelivered, SimpleNamespace(delivery_tag=2), properties(), body)

        self.assertEqual(redelivered.acks, [2])
        self.assertEqual(self.attempts(), 1)
        self.assertEqual(self.store.load(self.path_id).version, revision_after_commit)


if __name__ == "__main__":
    unittest.main()
