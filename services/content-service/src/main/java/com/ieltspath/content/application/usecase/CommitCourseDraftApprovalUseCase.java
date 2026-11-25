package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.VocabularySenseLookup.SenseText;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class CommitCourseDraftApprovalUseCase {
    private final CourseDraftUseCases draftAccess;
    private final AiDraftRepository drafts;
    private final CourseMaterializer materializer;
    private final Clock clock;

    public CommitCourseDraftApprovalUseCase(CourseDraftUseCases draftAccess, AiDraftRepository drafts,
                                             CourseMaterializer materializer, Clock clock) {
        this.draftAccess = draftAccess;
        this.drafts = drafts;
        this.materializer = materializer;
        this.clock = clock;
    }

    @Transactional
    public ApproveCourseDraftUseCase.Approved execute(UUID reviewer, UUID draftId, String expectedResult,
                                                       CourseDraftStructure plan,
                                                       CourseDraftAnalysisService.Checked checked,
                                                       Map<String, UUID> newWordSenses,
                                                       Map<UUID, SenseText> chosenSenses) {
        AiDraft draft = draftAccess.ownReadyDraftForUpdate(reviewer, draftId);
        if (!expectedResult.equals(draft.getResultJson())) {
            throw new InvalidContentStateException("Bản nháp đã thay đổi trong lúc duyệt. Hãy tải lại rồi thử lại.");
        }
        CourseMaterializer.Created created = materializer.create(plan, checked.facts().resolvedQuestionRefs(),
                newWordSenses, chosenSenses, reviewer);
        draft.approve(reviewer, clock.instant());
        drafts.save(draft);
        return new ApproveCourseDraftUseCase.Approved(draftId, created.courseId(), created.topicIds(),
                created.lessonIds(), created.finalTestPackageIds());
    }
}
