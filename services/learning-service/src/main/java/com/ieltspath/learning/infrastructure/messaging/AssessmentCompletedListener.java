package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.usecase.ApplyAssessmentResultUseCase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies each completed result and acknowledges only after the transaction committed. A transient failure is
 * rejected to the retry queue; a contract violation or the last allowed attempt is copied to the DLQ (with an
 * {@code x-learning-failure} header) before the original is acknowledged. Payloads are never logged.
 */
@Component
public class AssessmentCompletedListener {
    static final String FAILURE_HEADER = "x-learning-failure";
    private static final Logger log = LoggerFactory.getLogger(AssessmentCompletedListener.class);

    private final AssessmentCompletedParser parser;
    private final ApplyAssessmentResultUseCase apply;
    private final int maxDeliveryAttempts;

    public AssessmentCompletedListener(AssessmentCompletedParser parser, ApplyAssessmentResultUseCase apply,
                                       @Value("${learning.messaging.max-delivery-attempts:5}") int maxDeliveryAttempts) {
        this.parser = parser;
        this.apply = apply;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    @RabbitListener(queues = AssessmentMessagingConfig.QUEUE, ackMode = "MANUAL",
            autoStartup = "${learning.messaging.consumer-enabled:true}")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        AssessmentResult result;
        try {
            result = parser.parse(message.getBody());
        } catch (AssessmentCompletedParser.ContractViolationException violation) {
            log.warn("Assessment event violates the contract: messageId={}, reason={}", messageId, violation.getMessage());
            park(message, channel, "contract: " + violation.getMessage());
            return;
        }
        try {
            apply.execute(result);
            channel.basicAck(tag, false);
        } catch (RuntimeException failure) {
            int attempt = previousFailures(message.getMessageProperties()) + 1;
            log.warn("Assessment event failed: eventId={}, attempt={}, errorType={}",
                    result.eventId(), attempt, failure.getClass().getSimpleName());
            if (attempt >= maxDeliveryAttempts) {
                park(message, channel, "exhausted after " + attempt + " attempts: " + failure.getClass().getSimpleName());
            } else {
                channel.basicNack(tag, false, false);
            }
        }
    }

    /** Copies the message to the DLQ with publisher confirmation, then acknowledges; on failure it is retried. */
    private void park(Message message, Channel channel, String reason) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        MessageProperties source = message.getMessageProperties();
        Map<String, Object> headers = new HashMap<>(source.getHeaders());
        headers.put(FAILURE_HEADER, reason);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .messageId(source.getMessageId())
                .type(source.getType())
                .contentType(source.getContentType())
                .contentEncoding(source.getContentEncoding())
                .deliveryMode(2)
                .headers(headers)
                .build();
        try {
            channel.confirmSelect();
            channel.basicPublish(AssessmentMessagingConfig.DEAD_LETTER_EXCHANGE,
                    AssessmentMessagingConfig.DEAD_LETTER_QUEUE, true, properties, message.getBody());
            channel.waitForConfirmsOrDie(5_000);
            channel.basicAck(tag, false);
        } catch (IOException | java.util.concurrent.TimeoutException | InterruptedException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Could not park assessment event: messageId={}, errorType={}",
                    source.getMessageId(), failure.getClass().getSimpleName());
            channel.basicNack(tag, false, false);
        }
    }

    /** Rejections counted by the broker for the main queue; each one went through the retry queue. */
    static int previousFailures(MessageProperties properties) {
        List<Map<String, ?>> deaths = properties.getXDeathHeader();
        if (deaths == null) return 0;
        return deaths.stream()
                .filter(death -> AssessmentMessagingConfig.QUEUE.equals(death.get("queue"))
                        && "rejected".equals(death.get("reason")))
                .mapToInt(death -> death.get("count") instanceof Number count ? count.intValue() : 0)
                .sum();
    }
}
