package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.ExaminerHistoryStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** What the examiner graded, to read again: the list, newest first, and one finished review with the essay it was about. */
@RestController
@RequestMapping("/api/assessments/examiners/me/completed")
@PreAuthorize("hasRole('EXAMINER')")
public class ExaminerHistoryController {
    static final int MAX_SIZE = 100;

    private final CurrentUserProvider currentUser;
    private final ExaminerHistoryStore history;

    public ExaminerHistoryController(CurrentUserProvider currentUser, ExaminerHistoryStore history) {
        this.currentUser = currentUser;
        this.history = history;
    }

    @GetMapping
    public ExaminerHistoryStore.Page completed(@RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or more and size from 1 to " + MAX_SIZE);
        }
        return history.completed(currentUser.requireUserId(), page, size);
    }

    /** Another examiner's job, an unfinished one and an unknown id all answer 404. */
    @GetMapping("/{jobId}")
    public ExaminerHistoryStore.GradedReview review(@PathVariable("jobId") UUID jobId) {
        return history.review(currentUser.requireUserId(), jobId)
                .orElseThrow(() -> new AssessmentNotFoundException("Graded review not found"));
    }
}
