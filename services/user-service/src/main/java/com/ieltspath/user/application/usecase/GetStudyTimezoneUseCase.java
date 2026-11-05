package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

/**
 * The timezone a learner's study days are counted in, for the Learning Service. A learner without a profile, or whose
 * stored name is not a timezone, gets the product default.
 */
@Service
@RequiredArgsConstructor
public class GetStudyTimezoneUseCase {
    private static final String DEFAULT = "Asia/Ho_Chi_Minh";

    private final LearnerProfileRepository profiles;

    @Transactional(readOnly = true)
    public String execute(UUID userId) {
        return profiles.findByUserId(userId).map(profile -> profile.getTimezone()).filter(GetStudyTimezoneUseCase::valid)
                .orElse(DEFAULT);
    }

    private static boolean valid(String name) {
        if (name == null || name.isBlank()) return false;
        try {
            ZoneId.of(name);
            return true;
        } catch (DateTimeException invalid) {
            return false;
        }
    }
}
