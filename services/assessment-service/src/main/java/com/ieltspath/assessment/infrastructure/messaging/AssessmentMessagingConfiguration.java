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
     * The one queue Assessment also declares: the late-examiner notices must reach a queue even if Notification has never
     * started. Notification declares the very same queue from the same helper.
     */
    @Bean
    Declarables gradingOverdueTopology(
            @Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}") String exchangeName) {
        return GradingOverdueTopology.declarables(exchangeName);
    }

    /** Same reason for the examiner's "will be late soon" reminders. */
    @Bean
    Declarables gradingDueSoonTopology(
            @Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}") String exchangeName) {
        return GradingDueSoonTopology.declarables(exchangeName);
    }

    /** Same reason for the examiner's "a learner chose you" notices. */
    @Bean
    Declarables gradingRequestedTopology(
            @Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}") String exchangeName) {
        return GradingRequestedTopology.declarables(exchangeName);
    }

    /** Same reason for the learner's "your essay was graded" notices. */
    @Bean
    Declarables gradingCompletedTopology(
            @Value("${assessment.messaging.exchange:" + AssessmentMessaging.EXCHANGE + "}") String exchangeName) {
        return GradingCompletedTopology.declarables(exchangeName);
    }
}
