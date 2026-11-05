package com.ieltspath.learning.application.port;

import java.time.ZoneId;
import java.util.UUID;

/** The timezone a learner's study days are counted in. It is chosen on the profile, which the User Service keeps. */
public interface StudyTimezones {
    /** Never fails: when the profile cannot be read the last timezone used, then the default, is returned. */
    ZoneId zoneOf(UUID userId);
}
