package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.LearnerTimezones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The learner's XP progress and settings. The timezone decides when the day's XP cap resets. */
@RestController
@RequestMapping("/api/learning/me")
@RequiredArgsConstructor
public class GamificationController {
    private final CurrentUserProvider currentUser;
    private final GamificationRecorder gamification;
    private final LearnerTimezones timezones;

    @GetMapping("/progress")
    public GamificationRecorder.Progress progress() {
        return gamification.progress(currentUser.requireUserId());
    }

    @GetMapping("/settings")
    public Settings settings() {
        return new Settings(timezones.timezoneOf(currentUser.requireUserId()));
    }

    @PutMapping("/settings")
    public Settings updateSettings(@Valid @RequestBody SettingsRequest request) {
        UUID userId = currentUser.requireUserId();
        timezones.save(userId, request.timezone());
        return new Settings(timezones.timezoneOf(userId));
    }

    public record SettingsRequest(@NotBlank String timezone) {}

    public record Settings(String timezone) {}
}
