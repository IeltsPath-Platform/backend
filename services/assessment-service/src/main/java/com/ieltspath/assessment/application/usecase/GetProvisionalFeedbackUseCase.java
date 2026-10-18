package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class GetProvisionalFeedbackUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final GateEssayJobStore jobs;
    private final ObjectMapper json;
    public GetProvisionalFeedbackUseCase(AssessmentAttemptRepository attempts,AssessmentResultRepository results,
                                         GateEssayJobStore jobs,ObjectMapper json) {
        this.attempts=attempts;this.results=results;this.jobs=jobs;this.json=json;
    }
    @Transactional(readOnly=true)
    public Feedback execute(UUID user,UUID attemptId) {
        var attempt=attempts.findByIdAndUserId(attemptId,user).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getAttemptType()!=AttemptType.COURSE_GATE) throw new AssessmentNotFoundException("Provisional course feedback not found");
        boolean pending=results.findLatestByAttemptId(attemptId).map(r->!r.isCompleted()).orElse(true);
        List<EssayFeedback> feedback=new ArrayList<>();
        for (var job:jobs.aiJobs(attemptId)) {
            if (!"COMPLETED".equals(job.status()) || job.feedback()==null) continue;
            try {
                JsonNode node=json.readTree(job.feedback());
                List<String> comments=new ArrayList<>(),focus=new ArrayList<>();
                if (node.path("criteria").isArray()) for (var criterion:node.path("criteria"))
                    if (criterion.path("comment").isTextual()) comments.add(criterion.get("comment").textValue());
                if (node.path("focus").isArray()) for (var text:node.path("focus")) if(text.isTextual()) focus.add(text.textValue());
                feedback.add(new EssayFeedback(job.attemptItemId(),node.path("summary").isTextual()?node.get("summary").textValue():null,comments,focus));
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // Malformed feedback is unavailable; never return raw model output.
            }
        }
        return new Feedback(pending,pending?"Đang chờ giám khảo. Nhận xét tạm thời, chưa phải kết quả chính thức.":"Giám khảo đã chốt kết quả. Nhận xét này là nhận xét tạm thời.",feedback);
    }
    public record Feedback(boolean pendingExaminer,String message,List<EssayFeedback> essays) {
        @Override public String toString() { return "Feedback[pendingExaminer=" + pendingExaminer + "]"; }
    }
    public record EssayFeedback(UUID attemptItemId,String summary,List<String> comments,List<String> focus) {
        @Override public String toString() { return "EssayFeedback[attemptItemId=" + attemptItemId + "]"; }
    }
}
