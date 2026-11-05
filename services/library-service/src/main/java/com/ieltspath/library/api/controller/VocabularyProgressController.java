package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.dto.request.WeeklyGoalRequest;
import com.ieltspath.library.api.dto.response.VocabularyAchievementsResponse;
import com.ieltspath.library.application.result.*;
import com.ieltspath.library.application.usecase.VocabularyProgressUseCase;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.CefrLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/learning-support/flashcards/notebook")
@RequiredArgsConstructor
public class VocabularyProgressController {
    private final CurrentUserProvider currentUser;
    private final VocabularyProgressUseCase progress;

    @GetMapping("/weekly-goal")
    public WeeklyGoalResult weeklyGoal(@RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone) {
        return progress.weeklyGoal(currentUser.requireUserId(), zone(timezone));
    }

    @PutMapping("/weekly-goal")
    public WeeklyGoalResult setWeeklyGoal(@Valid @RequestBody WeeklyGoalRequest request,
                                          @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone) {
        return progress.setWeeklyGoal(currentUser.requireUserId(), request.targetWords(), zone(timezone));
    }

    @GetMapping("/vocabulary-map")
    public VocabularyMapResult vocabularyMap(@RequestParam(required = false) String currentCefr) {
        CefrLevel current = null;
        if (currentCefr != null) {
            try { current = CefrLevel.valueOf(currentCefr.toUpperCase()); }
            catch (IllegalArgumentException exception) { throw new InvalidDataException("currentCefr must be A1..C2"); }
        }
        return progress.vocabularyMap(currentUser.requireUserId(), current);
    }

    @GetMapping("/topics-progress")
    public TopicProgressResult topicsProgress(@RequestParam(required = false) UUID setId) {
        return progress.topicsProgress(currentUser.requireUserId(), setId);
    }

    @GetMapping("/sets/{setId}/progress")
    public SetProgressResult setProgress(@PathVariable UUID setId) {
        return progress.setProgress(currentUser.requireUserId(), setId);
    }

    @GetMapping("/recent-result")
    public ResponseEntity<RecentResult> recentResult() {
        RecentResult result = progress.recentResult(currentUser.requireUserId());
        return result == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    @GetMapping("/achievements")
    public VocabularyAchievementsResponse achievements(@RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone) {
        return VocabularyAchievementsResponse.from(progress.achievements(currentUser.requireUserId(), zone(timezone)));
    }

    private ZoneId zone(String value) {
        try {
            if (!ZoneId.getAvailableZoneIds().contains(value)) throw new InvalidDataException("timezone must be an IANA timezone");
            return ZoneId.of(value);
        } catch (DateTimeException exception) {
            throw new InvalidDataException("timezone must be an IANA timezone");
        }
    }
}
