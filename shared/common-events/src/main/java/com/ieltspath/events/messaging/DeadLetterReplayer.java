package com.ieltspath.events.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Moves messages from a dead-letter queue back to the queue they were parked from, once an operator has fixed the cause.
 * Only the routes a service declares can be replayed. Each message is taken without auto-acknowledgement and acknowledged
 * only after the broker has confirmed the republished copy; a failure leaves it in the dead-letter queue.
 */
public class DeadLetterReplayer {
    public static final int MAX_PER_CALL = 500;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbit;
    private final AmqpAdmin admin;
    private final List<DeadLetterRoute> routes;
    private final DefaultMessagePropertiesConverter converter = new DefaultMessagePropertiesConverter();

    public DeadLetterReplayer(RabbitTemplate rabbit, AmqpAdmin admin, List<DeadLetterRoute> routes) {
        this.rabbit = rabbit;
        this.admin = admin;
        this.routes = List.copyOf(routes);
    }

    public List<DeadLetterRoute> routes() {
        return routes;
    }

    /** The number of messages waiting in each declared dead-letter queue. */
    public List<DeadLetterCount> counts() {
        return routes.stream()
                .map(route -> new DeadLetterCount(route.deadLetterQueue(), route.queue(), messagesIn(route.deadLetterQueue())))
                .toList();
    }

    /**
     * @param deadLetterQueue a dead-letter queue declared by this service
     * @param max             how many messages to move at most, from 1 to {@value #MAX_PER_CALL}
     * @throws IllegalArgumentException when the queue is not a declared dead-letter queue or {@code max} is out of range
     */
    public ReplayResult replay(String deadLetterQueue, int max) {
        if (max < 1 || max > MAX_PER_CALL) {
            throw new IllegalArgumentException("max must be between 1 and " + MAX_PER_CALL);
        }
        DeadLetterRoute route = routes.stream()
                .filter(candidate -> candidate.deadLetterQueue().equals(deadLetterQueue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not a declared dead-letter queue: " + deadLetterQueue));
        int moved = 0;
        while (moved < max && Boolean.TRUE.equals(rabbit.execute(channel -> replayOne(channel, route)))) {
            moved++;
        }
        return new ReplayResult(deadLetterQueue, moved, messagesIn(deadLetterQueue));
    }

    /** @return true when one message was moved, false when the dead-letter queue was empty. */
    private Boolean replayOne(Channel channel, DeadLetterRoute route) throws Exception {
        GetResponse response = channel.basicGet(route.deadLetterQueue(), false);
        if (response == null) {
            return false;
        }
        long deliveryTag = response.getEnvelope().getDeliveryTag();
        try {
            MessageProperties properties = converter.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8");
            properties.getHeaders().remove(MessageHeaders.RETRY_COUNT);
            CorrelationData correlation = new CorrelationData(String.valueOf(properties.getMessageId()));
            rabbit.send("", route.queue(), new Message(response.getBody(), properties), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker did not accept the replayed message");
            }
            channel.basicAck(deliveryTag, false);
            return true;
        } catch (Exception failure) {
            channel.basicNack(deliveryTag, false, true);
            throw failure;
        }
    }

    private long messagesIn(String queue) {
        Properties properties = admin.getQueueProperties(queue);
        if (properties == null) {
            return 0;
        }
        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count instanceof Number number ? number.longValue() : 0;
    }
}
