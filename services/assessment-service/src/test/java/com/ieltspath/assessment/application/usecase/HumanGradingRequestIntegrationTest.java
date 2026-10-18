package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand;
import com.ieltspath.assessment.application.command.StartAssessmentAttemptCommand;
import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.assessment.infrastructure.client.AccessHumanGradingCreditClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HumanGradingRequestIntegrationTest extends ExaminerIntegrationSupport {
    @MockitoBean AccessHumanGradingCreditClient credits;

    @Test
    void topicEssayConsumesCreditAndEnqueuesOneHumanJobAcrossRetries() throws Exception {
        UUID version = UUID.randomUUID();
        UUID contentItem = UUID.randomUUID();
        var item = new ContentPackageProvider.Item(contentItem, 1, "Discuss both views", List.of(),
                Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250), "", BigDecimal.ONE,
                List.of(new ContentPackageProvider.KnowledgePointWeight(UUID.randomUUID(), BigDecimal.ONE)));
        when(content.findPackageVersion(version)).thenReturn(Optional.of(new ContentPackageProvider.PackageVersion(
                version, "TOPIC_TEST", List.of(new ContentPackageProvider.Section(UUID.randomUUID(), "Writing",
                "WRITING", "Write an essay", 1, null, null, List.of(item))))));
        UUID attempt = start.execute(new StartAssessmentAttemptCommand(learner, version, AttemptMode.STANDARD,
                AttemptChannel.WEB)).id();
        UUID attemptItem = item(attempt);
        UUID submission = submissions.execute(new CreateLearnerSubmissionCommand(learner, attemptItem, "{}",
                Skill.WRITING, "An essay long enough for human grading.", null, "topic-essay-" + attempt)).id();
        submit.execute(new SubmitAssessmentAttemptCommand(learner, attempt));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/assessments/attempts/{attempt}/items/{item}/human-grading", attempt, attemptItem)
                            .with(user(learner.toString()).roles("CUSTOMER")))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.submissionId").value(submission.toString()));
        }
        verify(credits, times(1)).consume(submission);
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE submission_id=? AND grading_mode='HUMAN' AND reason='HUMAN_CREDIT'", submission));
        assertEquals(1, count("SELECT count(*) FROM human_reviews h JOIN grading_jobs j ON j.id=h.grading_job_id WHERE j.submission_id=? AND j.reason='HUMAN_CREDIT'", submission));
    }
}
