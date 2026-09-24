"""RabbitMQ integration check for the AssessmentCompleted.v2 consumer topology.

Set AI_LEARNING_TEST_AMQP_URL and AI_LEARNING_TEST_DATABASE_URL to disposable
RabbitMQ and PostgreSQL instances to run it.
"""

import json
import os
import time
import unittest
from unittest import mock
from uuid import uuid4

from psycopg2 import OperationalError

from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.formal_result_applier import FormalResultApplier
from app.application.path_service import PathService
from app.messaging.assessment_consumer import FAILURE_HEADER, AssessmentCompletedConsumer
from app.messaging.topology import AssessmentCompletedTopology
from app.persistence.postgres_learning_store import PostgresLearningStore

from tests.formal_assessment_support import VOCABULARY_KP, curriculum, event, item, mapping
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip


class AssessmentRabbitMqTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        amqp_url = os.environ.get("AI_LEARNING_TEST_AMQP_URL")
        if not amqp_url:
            raise unittest.SkipTest("AI_LEARNING_TEST_AMQP_URL is not configured")
        import pika

        cls.pika = pika
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        suffix = uuid4().hex
        # Unique names keep the test from touching a shared environment's real queues.
        cls.topology = AssessmentCompletedTopology(
            exchange=f"test.assessment.events.{suffix}",
            queue=f"test.ai-learning.assessment-completed.{suffix}",
            retry_exchange=f"test.ai-learning.retry.{suffix}",
            retry_queue=f"test.ai-learning.assessment-completed.retry.{suffix}",
            dead_letter_exchange=f"test.ai-learning.dlx.{suffix}",
            dead_letter_queue=f"test.ai-learning.assessment-completed.dlq.{suffix}",
            retry_delay_ms=500,
        )
        cls.connection = pika.BlockingConnection(pika.URLParameters(amqp_url))
        cls.channel = cls.connection.channel()
        cls.channel.confirm_delivery()
        cls.topology.declare(cls.channel)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "channel"):
            for queue in (cls.topology.queue, cls.topology.retry_queue, cls.topology.dead_letter_queue):
                cls.channel.queue_delete(queue=queue)
            for exchange in (cls.topology.exchange, cls.topology.retry_exchange, cls.topology.dead_letter_exchange):
                cls.channel.exchange_delete(exchange=exchange)
            cls.connection.close()
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def setUp(self):
        store = PostgresLearningStore(self.schema.url)
        self.paths = PathService(store)
        self.user_id, self.goal_id = str(uuid4()), str(uuid4())
        self.path_id, _ = self.paths.ensure_path(self.user_id, self.goal_id, curriculum())
        self.consumer = AssessmentCompletedConsumer(
            FormalAssessmentIngestionService(store, self.paths), self.topology, max_delivery_attempts=2)

    def publish(self, body: bytes):
        self.channel.basic_publish(
            exchange=self.topology.exchange,
            routing_key=self.topology.routing_key,
            body=body,
            properties=self.pika.BasicProperties(content_type="application/json", message_id=str(uuid4()),
                                                 type="AssessmentCompleted.v2", delivery_mode=2),
            mandatory=True,
        )

    def consume_one(self, queue: str, timeout: float = 10.0):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            method, properties, body = self.channel.basic_get(queue=queue, auto_ack=False)
            if method is not None:
                return method, properties, body
            time.sleep(0.1)
        self.fail(f"No message arrived on {queue}")

    def test_published_event_is_consumed_committed_and_acknowledged(self):
        self.publish(json.dumps(event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=str(uuid4()),
                                      items=[item([mapping(VOCABULARY_KP)], is_correct=True)])).encode())

        method, properties, body = self.consume_one(self.topology.queue)
        self.consumer.on_message(self.channel, method, properties, body)

        progress = PostgresLearningStore(self.schema.url).get_owned_progress(self.path_id, self.user_id)
        self.assertEqual(len(progress.quiz_attempts), 1)
        self.assertEqual(self.channel.queue_declare(queue=self.topology.queue, passive=True).method.message_count, 0)

    def test_invalid_contract_reaches_the_dead_letter_queue(self):
        payload = event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=str(uuid4()),
                        items=[item([mapping(VOCABULARY_KP)], is_correct=True)])
        del payload["data"]["learning_goal_id"]
        self.publish(json.dumps(payload).encode())

        method, properties, body = self.consume_one(self.topology.queue)
        self.consumer.on_message(self.channel, method, properties, body)

        dead_method, dead_properties, _ = self.consume_one(self.topology.dead_letter_queue)
        self.channel.basic_ack(dead_method.delivery_tag)
        self.assertIn("contract", dead_properties.headers[FAILURE_HEADER])

    def test_result_before_the_path_exists_is_parked_not_retried(self):
        self.publish(json.dumps(event(user_id=self.user_id, goal_id=str(uuid4()), attempt_id=str(uuid4()),
                                      items=[item([mapping(VOCABULARY_KP)], is_correct=True)])).encode())

        method, properties, body = self.consume_one(self.topology.queue)
        self.consumer.on_message(self.channel, method, properties, body)

        self.assertEqual(self.schema.query("SELECT count(*) FROM pending_formal_assessment_results")[0][0], 1)
        time.sleep(self.topology.retry_delay_ms / 1000 * 2)
        for queue in (self.topology.queue, self.topology.retry_queue, self.topology.dead_letter_queue):
            self.assertEqual(self.channel.queue_declare(queue=queue, passive=True).method.message_count, 0, queue)

    def test_transient_failure_is_redelivered_through_the_retry_queue(self):
        """Used "no path yet" as the transient failure; that is now parked, so the database fails instead."""
        payload = event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=str(uuid4()),
                        items=[item([mapping(VOCABULARY_KP)], is_correct=True)])
        self.publish(json.dumps(payload).encode())

        with mock.patch.object(FormalResultApplier, "apply_to_path",
                               side_effect=OperationalError("database unavailable")):
            method, properties, body = self.consume_one(self.topology.queue)
            self.consumer.on_message(self.channel, method, properties, body)

            redelivered_method, redelivered_properties, redelivered_body = self.consume_one(self.topology.queue)
            deaths = redelivered_properties.headers["x-death"]
            self.assertTrue(any(d["queue"] == self.topology.queue and d["reason"] == "rejected" for d in deaths))
            # Second failure exhausts max_delivery_attempts=2 and parks the message.
            self.consumer.on_message(self.channel, redelivered_method, redelivered_properties, redelivered_body)
        dead_method, _, _ = self.consume_one(self.topology.dead_letter_queue)
        self.channel.basic_ack(dead_method.delivery_tag)

if __name__ == "__main__":
    unittest.main()
