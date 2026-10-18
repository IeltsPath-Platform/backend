package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.ExaminerQaRepository;
import com.ieltspath.assessment.application.service.ExaminerQaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assessments/admin/examiner-quality")
@PreAuthorize("hasRole('ADMIN')")
public class ExaminerQualityController {
    private final ExaminerQaService qa;

    public ExaminerQualityController(ExaminerQaService qa) {
        this.qa = qa;
    }

    @GetMapping
    public List<ExaminerQaRepository.ExaminerQuality> quality() {
        return qa.quality();
    }
}
