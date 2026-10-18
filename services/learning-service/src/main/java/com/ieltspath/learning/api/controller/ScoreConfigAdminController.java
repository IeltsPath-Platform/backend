package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.UpdateScoreConfigRequest;
import com.ieltspath.learning.application.result.ScoreConfigResult;
import com.ieltspath.learning.application.usecase.ManageScoreConfigUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Score weights and thresholds per course; without {@code courseId} the default every other course follows. */
@RestController
@RequestMapping("/api/learning/admin/score-config")
@PreAuthorize("hasRole('ADMIN')")
public class ScoreConfigAdminController {
    private final CurrentUserProvider currentUser;
    private final ManageScoreConfigUseCase config;

    public ScoreConfigAdminController(CurrentUserProvider currentUser, ManageScoreConfigUseCase config) {
        this.currentUser = currentUser;
        this.config = config;
    }

    @GetMapping
    public ScoreConfigResult get(@RequestParam(name = "courseId", required = false) UUID courseId) {
        return config.get(courseId);
    }

    @PutMapping
    public ScoreConfigResult update(@RequestParam(name = "courseId", required = false) UUID courseId,
                                    @Valid @RequestBody UpdateScoreConfigRequest request) {
        return config.update(currentUser.requireUserId(), courseId, request.processWeight(), request.midWeight(),
                request.finalWeight(), request.practiceShare(), request.practiceTestShare(), request.passThreshold(),
                request.midRemediationBelow());
    }
}
