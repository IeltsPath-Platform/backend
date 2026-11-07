package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.*;
import com.ieltspath.library.config.StudyGamificationProperties;
import com.ieltspath.library.domain.study.StudyStreakRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class StudyStreakRecorder implements StudyStreakActivity {
    private final StudyGamificationRepository repository;
    private final StudyLock lock;
    private final StudyGamificationProperties properties;
    public void recordActivity(UUID userId,Instant at) {
        // A flashcard review locks its card first, then this user. Unit completion never locks cards, so no lock cycle exists.
        lock.lock(userId);
        String zone=repository.profile(userId).map(StudyGamificationRepository.Profile::timezone).orElse(properties.getDefaultTimezone());
        var date=LocalDate.ofInstant(at,ZoneId.of(zone));
        repository.saveStreak(userId,StudyStreakRules.recordAnswer(repository.streak(userId),date),at);
    }
}
