package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.EssayHistoryResult;
import com.ieltspath.learning.application.result.EssayHistoryResult.CriterionBand;
import com.ieltspath.learning.application.result.EssayHistoryResult.Version;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** The chain of graded versions an essay was revised through, with what changed between them. */
@Service
public class GetEssayHistoryUseCase {
    private final WritingSubmissionRepository essays;

    public GetEssayHistoryUseCase(WritingSubmissionRepository essays) {
        this.essays = essays;
    }

    @Transactional(readOnly = true)
    public EssayHistoryResult execute(UUID userId, UUID submissionId) {
        WritingSubmission any = essays.findOwned(submissionId, userId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));
        WritingSubmission root = any;
        while (root.previousSubmissionId() != null) {
            root = essays.findOwned(root.previousSubmissionId(), userId).orElseThrow();
        }

        List<WritingSubmission> graded = new ArrayList<>();
        if (root.status() == com.ieltspath.learning.domain.vo.WritingSubmissionStatus.GRADED) graded.add(root);
        WritingSubmission cursor = root;
        while (true) {
            Optional<WritingSubmission> next = essays.findGradedRevision(cursor.id());
            if (next.isEmpty()) break;
            cursor = next.get();
            graded.add(cursor);
        }

        List<Version> versions = new ArrayList<>();
        WritingSubmission before = null;
        for (WritingSubmission submission : graded) {
            versions.add(version(submission, before));
            before = submission;
        }
        List<String> resolved = List.of();
        List<String> remaining = List.of();
        if (graded.size() >= 2) {
            Set<String> older = errorCodes(graded.get(graded.size() - 2));
            Set<String> newer = errorCodes(graded.get(graded.size() - 1));
            resolved = older.stream().filter(code -> !newer.contains(code)).toList();
            remaining = older.stream().filter(newer::contains).toList();
        }
        WritingSubmission newest = graded.isEmpty() ? null : graded.getLast();
        boolean canRevise = newest != null && newest.nextRevisionNo().isPresent()
                && !essays.hasActiveRevision(newest.id());
        return new EssayHistoryResult(root.id(), List.copyOf(versions), resolved, remaining, canRevise,
                canRevise ? newest.nextRevisionNo().getAsInt() : null, canRevise ? newest.id() : null);
    }

    private static Version version(WritingSubmission submission, WritingSubmission before) {
        Map<String, BigDecimal> earlier = before == null ? Map.of() : before.grade().criteria().stream()
                .collect(Collectors.toMap(WritingGrade.Criterion::code, WritingGrade.Criterion::band, (a, b) -> a));
        List<CriterionBand> criteria = submission.grade().criteria().stream().map(criterion -> new CriterionBand(
                criterion.code(), criterion.band(),
                earlier.containsKey(criterion.code()) ? criterion.band().subtract(earlier.get(criterion.code())) : null))
                .toList();
        return new Version(submission.id(), submission.revisionNo(), submission.wordCount(),
                submission.overallBand(),
                before == null ? null : submission.overallBand().subtract(before.overallBand()), criteria,
                List.copyOf(errorCodes(submission)));
    }

    private static Set<String> errorCodes(WritingSubmission submission) {
        Set<String> codes = new LinkedHashSet<>();
        for (WritingGrade.Correction correction : submission.grade().corrections()) {
            if (correction.errorCode() != null) codes.add(correction.errorCode());
        }
        return codes;
    }
}
