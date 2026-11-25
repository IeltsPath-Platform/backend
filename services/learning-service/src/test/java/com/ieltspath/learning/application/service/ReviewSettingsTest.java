package com.ieltspath.learning.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSettingsTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(SettingsConfiguration.class);

    @Test
    void rejectsAnUnblockThresholdBelowFourAtStartup() {
        contextRunner.withPropertyValues("learning.review.unblock-after-failed-sets=3")
                .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("learning.review.unblock-after-failed-sets=2")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void usesFourWhenNothingIsConfiguredAndAcceptsALargerValue() {
        contextRunner.run(context -> assertThat(context.getBean(ReviewSettings.class).unblockAfterFailedSets())
                .isEqualTo(4));
        contextRunner.withPropertyValues("learning.review.unblock-after-failed-sets=6")
                .run(context -> assertThat(context.getBean(ReviewSettings.class).unblockAfterFailedSets())
                        .isEqualTo(6));
    }

    @EnableConfigurationProperties(ReviewSettings.class)
    static class SettingsConfiguration {}
}
