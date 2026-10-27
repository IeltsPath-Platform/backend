package com.ieltspath.assessment.infrastructure.messaging;

import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.GradingCompletedTopology;
import com.ieltspath.events.assessment.GradingDueSoonTopology;
import com.ieltspath.events.assessment.GradingOverdueTopology;
import com.ieltspath.events.assessment.GradingRequestedTopology;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Assessment owns the exchange it publishes to; consumers declare and bind their own queues.
 */
@Configuration
@EnableScheduling
public class AssessmentMessagingConfiguration {

    @Bean
    TopicExchange assessmentEventsExchange(
            @Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * The four grading notice queues Assessment also declares (the exception to the rule that a consumer owns its queues): the
     * notices must reach a queue even if Notification has never started. Notification declares the very same queues from the
     * same helpers, and both bind them to {@link AssessmentMessaging#EXCHANGE}, so a different exchange name in configuration
     * can never separate a queue from the messages meant for it. This one is for the late-examiner notices.
     */
    @Bean
    Declarables gradingOverdueTopology() {
        return GradingOverdueTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** Same reason, and the same fixed exchange, for the examiner's "will be late soon" reminders. */
    @Bean
    Declarables gradingDueSoonTopology() {
        return GradingDueSoonTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** Same reason, and the same fixed exchange, for the examiner's "a learner chose you" notices. */
    @Bean
    Declarables gradingRequestedTopology() {
        return GradingRequestedTopology.declarables(AssessmentMessaging.EXCHANGE);
    }

    /** Same reason, and the same fixed exchange, for the learner's "your essay was graded" notices. */
    @Bean
    Declarables gradingCompletedTopology() {
        return GradingCompletedTopology.declarables(AssessmentMessaging.EXCHANGE);
    }
}
