package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.service.ExaminerAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Lets the learner's examiner list grey out the examiners who cannot take another submission today. */
@RestController
@RequestMapping("/api/assessments/examiners")
@PreAuthorize("hasRole('CUSTOMER')")
public class ExaminerAvailabilityController {
    static final int MAX_IDS = 50;

    private final ExaminerAssignmentService assignments;

    public ExaminerAvailabilityController(ExaminerAssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping("/availability")
    public List<ExaminerAssignmentService.Availability> availability(@RequestParam("ids") List<UUID> ids) {
        if (ids.isEmpty() || ids.size() > MAX_IDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must hold from 1 to " + MAX_IDS + " examiners");
        }
        return assignments.availability(ids);
    }
}
