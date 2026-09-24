"""Consume AssessmentCompleted.v2 and apply it to DeepTutor learning paths.

Delivery contract:

* ACK only after the PostgreSQL transaction for the path committed.
* Transient failure: NACK without requeue, so the message waits in the retry queue.
* Contract error, or a message that failed ``max_delivery_attempts`` times: publish
  it to the dead-letter queue (with publisher confirm), then ACK the original.
* A redelivery after commit but before ACK is absorbed by the applied-version
  check and deterministic evidence identities.

Run with ``python -m app.messaging.assessment_consumer``.
"""

from __future__ import annotations

import json
import logging
import time
from typing import Any

import pika
from pika.exceptions import AMQPConnectionError

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.config import ConsumerSettings
from app.messaging.topology import AssessmentCompletedTopology
from app.persistence.postgres_learning_store import PostgresLearningStore

logger = logging.getLogger(__name__)

FAILURE_HEADER = "x-ai-learning-failure"
_MAX_REASON_LENGTH = 500


class AssessmentCompletedConsumer:
    def __init__(
        self,
        ingestion: FormalAssessmentIngestionService,
        topology: AssessmentCompletedTopology,
        max_delivery_attempts: int,
    ) -> None:
        self._ingestion = ingestion
        self._topology = topology
        self._max_delivery_attempts = max_delivery_attempts

    def on_message(self, channel: Any, method: Any, properties: Any, body: bytes) -> None:
        try:
            command = FormalEvidenceAdapter.to_command(json.loads(body))
        except (ValueError, UnicodeDecodeError) as exc:
            # ContractError and JSONDecodeError are ValueErrors: retrying cannot fix them.
            logger.warning("Rejecting invalid AssessmentCompleted.v2 message %s: %s", properties.message_id, exc)
            self._dead_letter(channel, method, properties, body, f"contract: {exc}")
            return

        try:
            outcome = self._ingestion.ingest(command)
        except Exception as exc:  # noqa: BLE001 - every non-contract failure is retried, then parked
            failures = self._previous_failures(properties) + 1
            logger.exception(
                "AssessmentCompleted.v2 %s failed (delivery %s of %s)",
                command.event_id, failures, self._max_delivery_attempts,
            )
            if failures >= self._max_delivery_attempts:
                self._dead_letter(channel, method, properties, body, f"exhausted retries: {exc}")
            else:
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            return

        logger.info(
            "AssessmentCompleted.v2 %s %s on path %s (version %s, %s evidence)",
            command.event_id, outcome.status, outcome.path_id, outcome.result_version,
            len(outcome.recorded_evidence),
        )
        channel.basic_ack(delivery_tag=method.delivery_tag)

    def _previous_failures(self, properties: Any) -> int:
        """Rejections from the main queue recorded by RabbitMQ in ``x-death``."""
        for death in (properties.headers or {}).get("x-death") or []:
            if death.get("queue") == self._topology.queue and death.get("reason") == "rejected":
                return int(death.get("count", 0))
        return 0

    def _dead_letter(self, channel: Any, method: Any, properties: Any, body: bytes, reason: str) -> None:
        headers = dict(properties.headers or {})
        headers[FAILURE_HEADER] = reason[:_MAX_REASON_LENGTH]
        try:
            channel.basic_publish(
                exchange=self._topology.dead_letter_exchange,
                routing_key=self._topology.dead_letter_queue,
                body=body,
                properties=pika.BasicProperties(
                    content_type=properties.content_type,
                    message_id=properties.message_id,
                    type=properties.type,
                    headers=headers,
                    delivery_mode=pika.DeliveryMode.Persistent,
                ),
                mandatory=True,
            )
        except Exception:  # noqa: BLE001 - never drop the message when it cannot be parked
            logger.exception("Could not park message %s; it will be retried", properties.message_id)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            return
        channel.basic_ack(delivery_tag=method.delivery_tag)


def build_consumer(settings: ConsumerSettings) -> tuple[AssessmentCompletedConsumer, AssessmentCompletedTopology]:
    store = PostgresLearningStore(settings.database_url.get_secret_value())
    topology = AssessmentCompletedTopology(
        exchange=settings.assessment_exchange, retry_delay_ms=settings.retry_delay_ms
    )
    ingestion = FormalAssessmentIngestionService(store, PathService(store))
    return AssessmentCompletedConsumer(ingestion, topology, settings.max_delivery_attempts), topology


def run(settings: ConsumerSettings) -> None:
    consumer, topology = build_consumer(settings)
    parameters = pika.URLParameters(settings.amqp_url.get_secret_value())
    while True:
        try:
            connection = pika.BlockingConnection(parameters)
            channel = connection.channel()
            channel.confirm_delivery()
            topology.declare(channel)
            # One unacknowledged message at a time: each is committed before the next.
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=topology.queue, on_message_callback=consumer.on_message)
            logger.info("Consuming %s", topology.queue)
            channel.start_consuming()
        except AMQPConnectionError:
            logger.warning("RabbitMQ connection lost; reconnecting in 5 seconds", exc_info=True)
            time.sleep(5)
        except KeyboardInterrupt:
            return


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    run(ConsumerSettings())
