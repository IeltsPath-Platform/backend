package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import org.springframework.stereotype.Component;

/**
 * Whether a learner can be offered AI Speaking grading right now: the feature is switched on and both services it
 * depends on are configured. Offering it without them would let a learner record a whole test that can never be graded.
 */
@Component
public class SpeakingAvailability {
    private final SpeakingPracticeSettings settings;
    private final SpeechAnalysisClient speech;
    private final SpeakingGrader grader;

    public SpeakingAvailability(SpeakingPracticeSettings settings, SpeechAnalysisClient speech, SpeakingGrader grader) {
        this.settings = settings;
        this.speech = speech;
        this.grader = grader;
    }

    public boolean isAvailable() {
        return settings.enabled() && speech.available() && grader.available();
    }
}
