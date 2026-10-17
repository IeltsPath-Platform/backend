package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import java.time.*;
import java.util.UUID;

final class StudyTimezone {
    private StudyTimezone() {}
    static ZoneId forUser(LearnerProfileRepository profiles, UUID userId) {
        String name=profiles.findByUserId(userId).map(p->p.getTimezone()).orElse("Asia/Ho_Chi_Minh");
        try { return ZoneId.of(name); }
        catch(DateTimeException | NullPointerException e) { return ZoneId.of("Asia/Ho_Chi_Minh"); }
    }
}
