package com.ieltspath.learning.application.service;

/** Settings for tests; production code gets them from {@code learning.speaking.*}. */
public final class SpeakingSettingsFixture {
    private SpeakingSettingsFixture() {
    }

    /** Grading enabled, 5 points, 5 MB recordings, and the same defaults the service ships with. */
    public static SpeakingPracticeSettings enabled() {
        return of(true, 5L * 1024 * 1024);
    }

    public static SpeakingPracticeSettings disabled() {
        return of(false, 5L * 1024 * 1024);
    }

    public static SpeakingPracticeSettings of(boolean enabled, long maxRecordingBytes) {
        return new SpeakingPracticeSettings(enabled, 5, "speaking-recordings", maxRecordingBytes, 1800, 90,
                "Asia/Ho_Chi_Minh", 5, 120, 3, 6.0, 60000, 3, 5, "", 5, 90, 30, 5.0, 4.0, 3.0);
    }
}
