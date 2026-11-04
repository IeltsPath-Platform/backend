package com.ieltspath.learning.infrastructure.messaging;

import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.learning.application.command.HumanReviewResult;
import com.ieltspath.learning.application.usecase.ApplyHumanReviewResultUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Applies an examiner's finished Writing review to the learner's mastery. A payload that breaks the contract is refused and
 * dead-lettered. Any other failure is treated as temporary and goes through the shared retry policy. Applying is idempotent
 * per essay, so a message handled twice changes nothing the second time. Logs carry ids only, never the payload.
 */
@Component
public class HumanReviewCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(HumanReviewCompletedListener.class);

    private final HumanReviewCompletedParser parser;
    private final ApplyHumanReviewResultUseCase apply;
    private final MessageRetry retry;

    public HumanReviewCompletedListener(HumanReviewCompletedParser parser, ApplyHumanReviewResultUseCase apply,
                                        @Qualifier(HumanReviewCompletedMessagingConfig.RETRY) MessageRetry retry) {
        this.parser = parser;
        this.apply = apply;
        this.retry = retry;
    }

    @RabbitListener(queues = HumanReviewCompletedMessagingConfig.QUEUE,
            containerFactory = HumanReviewCompletedMessagingConfig.CONTAINER_FACTORY,
            autoStartup = "${learning.messaging.consumer-enabled:true}")
    public void onHumanReviewCompleted(Message message) {
        HumanReviewResult result;
        try {
            result = parser.parse(message.getBody());
        } catch (IllegalArgumentException breaksContract) {
            throw new AmqpRejectAndDontRequeueException("Human review completed event breaks the contract", breaksContract);
        }
        try {
            apply.execute(result);
        } catch (IllegalArgumentException breaksContract) {
            log.warn("Human review completed event refused: reviewSubmissionId={}", result.reviewSubmissionId());
            throw new AmqpRejectAndDontRequeueException("Human review completed event cannot be applied", breaksContract);
        } catch (RuntimeException temporary) {
            log.warn("Human review completed event will be retried: reviewSubmissionId={}, errorType={}",
                    result.reviewSubmissionId(), temporary.getClass().getSimpleName());
            retry.retryLater(message, temporary);
        }
    }
}
