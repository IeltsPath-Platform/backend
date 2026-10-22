package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.CourseScoreResult;
import com.ieltspath.learning.application.usecase.GetCourseScoreUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class CourseScoreController {
    private final CurrentUserProvider currentUser;
    private final GetCourseScoreUseCase score;

    public CourseScoreController(CurrentUserProvider currentUser, GetCourseScoreUseCase score) {
        this.currentUser = currentUser;
        this.score = score;
    }

    /** The caller's own score in the course; the learner is never a request parameter. */
    @GetMapping("/courses/{courseId}/score")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CourseScoreResult mine(@PathVariable("courseId") UUID courseId) {
        return score.execute(currentUser.requireUserId(), courseId);
    }

    @GetMapping("/admin/learners/{userId}/courses/{courseId}/score")
    @PreAuthorize("hasRole('EXAMINER')")
    public CourseScoreResult learner(@PathVariable("userId") UUID userId, @PathVariable("courseId") UUID courseId) {
        return score.execute(userId, courseId);
    }
}
