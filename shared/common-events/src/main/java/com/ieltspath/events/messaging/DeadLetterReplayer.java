package com.ieltspath.events.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ReturnListener;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Moves messages from a dead-letter queue back to the queue they were parked from, once an operator has fixed the cause.
 * Only the routes a service declares can be replayed. Each replay uses its own channel with publisher confirms, so it does
 * not depend on how the service configures its template. A message is taken without auto-acknowledgement and acknowledged
 * only after the broker confirms the republished copy; any failure puts it back in the dead-letter queue.
 */
public class DeadLetterReplayer {
    public static final int MAX_PER_CALL = 500;
    private static final long CONFIRM_TIMEOUT_MILLIS = 5_000;

    private final ConnectionFactory connections;
    private final AmqpAdmin admin;
    private final List<DeadLetterRoute> routes;
    private final DefaultMessagePropertiesConverter converter = new DefaultMessagePropertiesConverter();

    public DeadLetterReplayer(ConnectionFactory connections, AmqpAdmin admin, List<DeadLetterRoute> routes) {
        this.connections = connections;
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
        // Only the messages waiting now are taken. A message that fails again goes straight back to the dead-letter queue,
        // and taking it again in the same call would replay the same failure over and over.
        int available = (int) Math.min(max, messagesIn(deadLetterQueue));
        int moved = 0;
        try (var connection = connections.createConnection()) {
            Channel channel = connection.createChannel(false);
            try {
                channel.confirmSelect();
                while (moved < available && replayOne(channel, route)) {
                    moved++;
                }
            } finally {
                if (channel.isOpen()) {
                    channel.close();
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Dead-letter replay stopped after " + moved + " messages", failure);
        }
        return new ReplayResult(deadLetterQueue, moved, messagesIn(deadLetterQueue));
    }

    /** @return true when one message was moved, false when the dead-letter queue was empty. */
    private boolean replayOne(Channel channel, DeadLetterRoute route) throws Exception {
        GetResponse response = channel.basicGet(route.deadLetterQueue(), false);
        if (response == null) {
            return false;
        }
        long deliveryTag = response.getEnvelope().getDeliveryTag();
        try {
            MessageProperties properties = converter.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8");
            properties.getHeaders().remove(MessageHeaders.RETRY_COUNT);
            // RabbitMQ counts dead-letter deaths in x-death; a replay starts a fresh count for both retry styles.
            properties.getHeaders().keySet().removeIf(key -> key.equals("x-death") || key.startsWith("x-first-death-") || key.startsWith("x-last-death-"));
            AMQP.BasicProperties basic = converter.fromMessageProperties(properties, "UTF-8");
            AtomicBoolean returned = new AtomicBoolean(false);
            ReturnListener onReturn = (replyCode, replyText, exchange, routingKey, unused, body) -> returned.set(true);
            channel.addReturnListener(onReturn);
            try {
                channel.basicPublish(route.exchange(), route.routingKey(), true, basic, response.getBody());
                if (!channel.waitForConfirms(CONFIRM_TIMEOUT_MILLIS) || returned.get()) {
                    throw new IllegalStateException("Broker did not accept the replayed message");
                }
            } finally {
                channel.removeReturnListener(onReturn);
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
