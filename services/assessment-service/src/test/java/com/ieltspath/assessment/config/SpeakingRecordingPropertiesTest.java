package com.ieltspath.assessment.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakingRecordingPropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRetentionAndSigningLimitsWithinTheSupportedRange() {
        var properties = new SpeakingRecordingProperties("speaking-recordings", 1, 900);

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void rejectsNonPositiveRetention() {
        var properties = new SpeakingRecordingProperties("speaking-recordings", 0, 900);

        assertThat(validator.validate(properties)).extracting(v -> v.getPropertyPath().toString()).contains("retentionDays");
    }

    @Test
    void rejectsSigningTtlOutsideOneThroughNineHundredSeconds() {
        var tooShort = new SpeakingRecordingProperties("speaking-recordings", 90, 0);
        var tooLong = new SpeakingRecordingProperties("speaking-recordings", 90, 901);

        assertThat(validator.validate(tooShort)).extracting(v -> v.getPropertyPath().toString()).contains("recordingUrlTtlSeconds");
        assertThat(validator.validate(tooLong)).extracting(v -> v.getPropertyPath().toString()).contains("recordingUrlTtlSeconds");
    }
}
