package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.ChooseExaminerRequest;
import com.ieltspath.assessment.application.service.ExaminerAssignmentService;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The learner's side of human grading: who grades my submissions, and choosing or replacing that examiner. */
@RestController
@RequestMapping("/api/assessments/grading-jobs")
@PreAuthorize("hasRole('CUSTOMER')")
public class ExaminerSelectionController {
    private final CurrentUserProvider currentUser;
    private final ExaminerAssignmentService assignments;

    public ExaminerSelectionController(CurrentUserProvider currentUser, ExaminerAssignmentService assignments) {
        this.currentUser = currentUser;
        this.assignments = assignments;
    }

    /** The learner's own human grading jobs with their state (waiting for a choice, waiting, late, done). */
    @GetMapping("/mine")
    public List<ExaminerAssignmentService.MyJobView> mine() {
        return assignments.mine(currentUser.requireUserId());
    }

    /**
     * Chooses the examiner of a job that waits for a choice, or replaces the examiner of a late job. The whole attempt
     * moves to the new examiner and the work starts again with them.
     */
    @PostMapping("/{id}/examiner")
    public ExaminerAssignmentService.MyJobView choose(@PathVariable("id") UUID id,
                                                      @Valid @RequestBody ChooseExaminerRequest request) {
        return assignments.choose(currentUser.requireUserId(), id, request.examinerId());
    }
}
