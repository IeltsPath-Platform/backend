package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.domain.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class GetJobEssayUseCase {
    private final ExaminerQueueStore store;
    public GetJobEssayUseCase(ExaminerQueueStore store) { this.store=store; }
    @Transactional(readOnly=true)
    public ExaminerQueueStore.Essay execute(UUID examiner,UUID jobId) {
        var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy yêu cầu chấm bài."));
        if (!examiner.equals(job.assignedExaminerId())) throw new AssessmentAccessDeniedException("Vui lòng nhận bài chấm trước khi xem nội dung bài viết.");
        return store.essay(jobId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy bài viết."));
    }
}
