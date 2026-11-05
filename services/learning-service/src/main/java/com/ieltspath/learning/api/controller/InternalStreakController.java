package com.ieltspath.learning.api.controller;

import com.ieltspath.learning.application.usecase.GetStreaksUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Service-to-service reads; the Gateway does not route {@code /internal/**}. */
@RestController
@RequestMapping("/internal/learning")
@RequiredArgsConstructor
public class InternalStreakController {
    private final GetStreaksUseCase streaks;

    /** The current streak of each listed learner who has one, so a caller can show streaks beside a list of profiles. */
    @PostMapping("/streaks")
    public StreaksResponse streaks(@Valid @RequestBody StreaksRequest request) {
        List<LearnerStreak> found = streaks.execute(request.userIds()).entrySet().stream()
                .map(entry -> new LearnerStreak(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(item -> item.userId().toString())).toList();
        return new StreaksResponse(found);
    }

    public record StreaksRequest(@NotNull @Size(max = 100) List<UUID> userIds) {}

    public record StreaksResponse(List<LearnerStreak> streaks) {}

    public record LearnerStreak(UUID userId, int currentDays) {}
}
