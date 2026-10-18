package com.ieltspath.access.infrastructure.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Parks a message whose handling failed for a reason that may pass (database or broker briefly unavailable) on a delayed
 * retry queue that feeds the original queue again, and refuses it once the retries are used up so the broker
 * dead-letters it. The attempt count travels in a header of our own: the broker's {@code x-death} count is not carried
 * over when a client republishes a message.
 */
public class MessageRetry {
    static final String ATTEMPTS_HEADER = "x-retry-count";

    private final RabbitTemplate rabbit;
    private final String retryQueue;
    private final int maxRetries;

    public MessageRetry(RabbitTemplate rabbit, String retryQueue, int maxRetries) {
        this.rabbit = rabbit;
        this.retryQueue = retryQueue;
        this.maxRetries = maxRetries;
    }

    /**
     * Returns normally once the message is on the retry queue, so the caller acknowledges the original delivery.
     *
     * @throws AmqpRejectAndDontRequeueException when the message has already been retried the allowed number of times
     */
    public void retryLater(Message message, RuntimeException cause) {
        int retries = retries(message);
        if (retries >= maxRetries) {
            throw new AmqpRejectAndDontRequeueException("Gave up after " + retries + " retries", cause);
        }
        message.getMessageProperties().setHeader(ATTEMPTS_HEADER, retries + 1);
        rabbit.send("", retryQueue, message);
    }

    static int retries(Message message) {
        Object count = message.getMessageProperties().getHeader(ATTEMPTS_HEADER);
        return count instanceof Number number ? number.intValue() : 0;
    }
}
