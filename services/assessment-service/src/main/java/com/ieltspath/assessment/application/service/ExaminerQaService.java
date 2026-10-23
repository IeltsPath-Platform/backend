package com.ieltspath.assessment.application.service;

import com.ieltspath.assessment.application.port.ExaminerQaRepository;
import com.ieltspath.assessment.application.port.UserDirectory;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ExaminerQaService {
    private final ExaminerQaRepository repository;
    private final UserDirectory users;
    private final double sampleRate;

    public ExaminerQaService(ExaminerQaRepository repository, UserDirectory users,
                             @Value("${assessment.qa.sample-rate:0.10}") double sampleRate) {
        if (sampleRate < 0 || sampleRate > 1 || Double.isNaN(sampleRate)) {
            throw new IllegalArgumentException("assessment.qa.sample-rate must be between 0 and 1");
        }
        this.repository = repository;
        this.users = users;
        this.sampleRate = sampleRate;
    }

    @Transactional
    public void sampleCompletedEssays(Collection<UUID> submissionIds) {
        if (sampleRate == 0) return;
        for (UUID submissionId : submissionIds) {
            if (ThreadLocalRandom.current().nextDouble() < sampleRate) {
                repository.enqueueSample(submissionId);
            }
        }
    }

    @Transactional
    public void submitReview(UUID gradingJobId, UUID reviewerId, BigDecimal band, String criteria, String note) {
        if (band == null || band.signum() < 0 || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new InvalidAssessmentStateException("Reviewer band must be from 0 to 9 in half bands");
        }
        if (criteria == null || criteria.isBlank()) {
            throw new InvalidAssessmentStateException("Reviewer criteria are required");
        }
        repository.saveReview(gradingJobId, reviewerId, band, criteria, note);
    }

    @Transactional(readOnly = true)
    public java.util.List<ExaminerQaRepository.ExaminerQuality> quality() {
        return repository.quality();
    }

    /**
     * The quality table with each examiner's name. Names come from one lookup for the whole table; when it fails the
     * table is still returned and the names are null.
     */
    @Transactional(readOnly = true)
    public java.util.List<NamedExaminerQuality> qualityWithNames() {
        java.util.List<ExaminerQaRepository.ExaminerQuality> rows = repository.quality();
        java.util.Map<UUID, String> names = users.displayNames(
                rows.stream().map(ExaminerQaRepository.ExaminerQuality::examinerId).toList());
        return rows.stream().map(row -> new NamedExaminerQuality(row.examinerId(), names.get(row.examinerId()),
                row.reviewedEssays(), row.averageAbsoluteBandDifference(), row.overOneBandRate())).toList();
    }

    public record NamedExaminerQuality(UUID examinerId, String examinerName, long reviewedEssays,
                                       BigDecimal averageAbsoluteBandDifference, BigDecimal overOneBandRate) {}
}
