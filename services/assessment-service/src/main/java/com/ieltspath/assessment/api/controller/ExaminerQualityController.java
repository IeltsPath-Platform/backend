package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.service.ExaminerQaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assessments/admin/examiner-quality")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class ExaminerQualityController {
    private final ExaminerQaService qa;

    public ExaminerQualityController(ExaminerQaService qa) {
        this.qa = qa;
    }

    /** One row per examiner; {@code examinerName} is null when the User Service could not name them. */
    @GetMapping
    public List<ExaminerQaService.NamedExaminerQuality> quality() {
        return qa.qualityWithNames();
    }
}
