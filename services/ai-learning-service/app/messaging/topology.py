"""RabbitMQ topology owned by the AI Learning consumer of AssessmentCompleted.v2.

Assessment publishes to the ``assessment.events`` topic exchange. AI Learning owns:

* ``ai-learning.assessment-completed.v2``        main queue, dead-letters to the retry exchange
* ``ai-learning.assessment-completed.v2.retry``  TTL queue that dead-letters back to the main queue
* ``ai-learning.assessment-completed.v2.dlq``    parking queue for contract errors and exhausted retries
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class AssessmentCompletedTopology:
    exchange: str = "assessment.events"
    routing_key: str = "assessment.completed.v2"
    queue: str = "ai-learning.assessment-completed.v2"
    retry_exchange: str = "ai-learning.assessment-completed.retry"
    retry_queue: str = "ai-learning.assessment-completed.v2.retry"
    dead_letter_exchange: str = "ai-learning.assessment-completed.dlx"
    dead_letter_queue: str = "ai-learning.assessment-completed.v2.dlq"
    retry_delay_ms: int = 30_000

    def declare(self, channel: Any) -> None:
        # Same attributes as Assessment's TopicExchange(durable=true, autoDelete=false).
        channel.exchange_declare(exchange=self.exchange, exchange_type="topic", durable=True)
        channel.exchange_declare(exchange=self.retry_exchange, exchange_type="direct", durable=True)
        channel.exchange_declare(exchange=self.dead_letter_exchange, exchange_type="direct", durable=True)

        # A rejected (nack, requeue=False) delivery goes to the retry queue.
        channel.queue_declare(
            queue=self.queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange": self.retry_exchange,
                "x-dead-letter-routing-key": self.retry_queue,
            },
        )
        channel.queue_bind(queue=self.queue, exchange=self.exchange, routing_key=self.routing_key)

        # After the delay the retry queue dead-letters the message back to the main queue.
        channel.queue_declare(
            queue=self.retry_queue,
            durable=True,
            arguments={
                "x-message-ttl": self.retry_delay_ms,
                "x-dead-letter-exchange": "",
                "x-dead-letter-routing-key": self.queue,
            },
        )
        channel.queue_bind(queue=self.retry_queue, exchange=self.retry_exchange, routing_key=self.retry_queue)

        channel.queue_declare(queue=self.dead_letter_queue, durable=True)
        channel.queue_bind(
            queue=self.dead_letter_queue,
            exchange=self.dead_letter_exchange,
            routing_key=self.dead_letter_queue,
        )
