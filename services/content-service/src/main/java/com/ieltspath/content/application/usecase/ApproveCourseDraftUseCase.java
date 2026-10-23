package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.CourseDraftErrorsException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis.Issue;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Approves a course draft: it must pass the analysis without errors, and then the course is created in one
 * transaction by {@link CourseMaterializer}. Any failure rolls all of it back and the draft stays {@code READY}.
 */
@Service
public class ApproveCourseDraftUseCase {

    private final CourseDraftUseCases draftAccess;
    private final AiDraftRepository drafts;
    private final CourseDraftCodec codec;
    private final CourseDraftAnalysisService analysis;
    private final CourseMaterializer materializer;
    private final Clock clock;

    public ApproveCourseDraftUseCase(CourseDraftUseCases draftAccess, AiDraftRepository drafts, CourseDraftCodec codec,
                                     CourseDraftAnalysisService analysis, CourseMaterializer materializer,
                                     Clock clock) {
        this.draftAccess = draftAccess;
        this.drafts = drafts;
        this.codec = codec;
        this.analysis = analysis;
        this.materializer = materializer;
        this.clock = clock;
    }

    @Transactional
    public Approved execute(UUID reviewer, UUID draftId) {
        AiDraft draft = draftAccess.ownReadyDraft(reviewer, draftId);
        CourseDraftStructure plan = codec.read(draft.getResultJson());
        requireSomethingToCreate(plan);
        CourseDraftAnalysisService.Checked checked = analysis.analyze(plan);
        requireNoErrors(checked.analysis());

        CourseMaterializer.Created created = materializer.create(plan, checked.facts().resolvedQuestionRefs());

        draft.approve(reviewer, clock.instant());
        drafts.save(draft);
        return new Approved(draftId, created.courseId(), created.topicIds(), created.lessonIds(),
                created.finalTestPackageIds());
    }

    private static void requireSomethingToCreate(CourseDraftStructure plan) {
        List<Issue> missing = new ArrayList<>();
        if (plan.course() == null) missing.add(new Issue("COURSE_MISSING", null, null));
        if (plan.topics().isEmpty()) missing.add(new Issue("TOPIC_MISSING", null, null));
        if (!missing.isEmpty()) throw new CourseDraftErrorsException(missing, false);
    }

    private static void requireNoErrors(CourseDraftAnalysis result) {
        if (result.valid()) return;
        List<Issue> conflicts = result.conflicts();
        if (!conflicts.isEmpty()) throw new CourseDraftErrorsException(conflicts, true);
        throw new CourseDraftErrorsException(result.errors(), false);
    }

    /** What approval created. */
    public record Approved(UUID draftId, UUID courseId, List<UUID> topicIds, List<UUID> lessonIds,
                           List<UUID> finalTestPackageIds) {
    }
}
