package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.LearningProgressStatistics;
import com.ieltspath.learning.application.result.StudyContextResult;
import com.ieltspath.learning.application.usecase.GetLearningProgressStatisticsUseCase;
import com.ieltspath.learning.application.usecase.GetStudyContextUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning/statistics")
@PreAuthorize("hasRole('CUSTOMER')")
public class LearningStatisticsController {
    private final CurrentUserProvider currentUser;
    private final GetLearningProgressStatisticsUseCase progress;
    private final GetStudyContextUseCase context;
    public LearningStatisticsController(CurrentUserProvider currentUser,
            GetLearningProgressStatisticsUseCase progress, GetStudyContextUseCase context) {
        this.currentUser = currentUser;
        this.progress = progress;
        this.context = context;
    }

    @GetMapping("/progress")
    public LearningProgressStatistics progress(@RequestParam(name = "courseId", required = false) UUID courseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "4") int size) {
        return progress.execute(currentUser.requireUserId(), courseId, page, size);
    }

    @GetMapping("/study-context")
    public StudyContextResult context(@RequestParam("sourceType") String sourceType,
            @RequestParam("sourceId") UUID sourceId) {
        return context.execute(currentUser.requireUserId(), sourceType, sourceId);
    }
}
