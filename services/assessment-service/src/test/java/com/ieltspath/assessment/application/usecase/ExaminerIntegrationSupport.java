package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.*;
import com.ieltspath.assessment.application.port.ContentPackageProvider;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.domain.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.when;

@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway", "assessment.outbox.relay.enabled=false",
        "assessment.llm-grading.enabled=false", "assessment.llm-grading.daily-limit=100"})
abstract class ExaminerIntegrationSupport {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired StartAssessmentAttemptUseCase start;
    @Autowired CreateLearnerSubmissionUseCase submissions;
    @Autowired SubmitAssessmentAttemptUseCase submit;
    @Autowired GradeGateEssayJobUseCase gradeJobs;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired com.ieltspath.assessment.application.port.GateEssayJobStore essayJobs;
    @Autowired com.ieltspath.assessment.application.port.EssayGradeComparisonStore comparisons;
    @MockitoBean com.ieltspath.assessment.application.port.ErrorTypeCatalog errorTypes;
    @MockitoBean com.ieltspath.assessment.application.port.ExaminerDirectory examinerDirectory;
    @MockitoBean ContentPackageProvider content;
    @MockitoBean EssayGradingPort grader;
    final UUID learner = UUID.randomUUID();
    final UUID examiner = UUID.randomUUID();

    String internalToken(UUID subject, String role) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer("urn:test:gateway").subject(subject.toString())
                .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .claim("roles", List.of(role)).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        token.sign(new MACSigner(KEY));
        return token.serialize();
    }

    UUID courseEssay() {
        return courseEssay(true);
    }
    UUID courseEssay(boolean sendEssay) {
        UUID version = UUID.randomUUID();
        var item = new ContentPackageProvider.Item(UUID.randomUUID(), 1, "Discuss public transport", List.of(),
                Map.of("type", "ESSAY", "task", "TASK_2", "minWords", 250, "passBand", 5.5), "",
                BigDecimal.ONE, List.of(new ContentPackageProvider.KnowledgePointWeight(UUID.randomUUID(), BigDecimal.ONE)));
        when(content.findPackageVersion(version)).thenReturn(Optional.of(new ContentPackageProvider.PackageVersion(
                version, "COURSE_TEST", List.of(new ContentPackageProvider.Section(UUID.randomUUID(), "Writing",
                "WRITING", "Write an essay", 1, null, null, List.of(item))))));
        UUID attempt = start.execute(new StartAssessmentAttemptCommand(learner, version, AttemptMode.STANDARD,
                AttemptChannel.WEB)).id();
        if (sendEssay) submissions.execute(new CreateLearnerSubmissionCommand(learner, item(attempt), "{}", Skill.WRITING,
                "My essay about public transport.", null, "essay-" + attempt));
        submit.execute(new SubmitAssessmentAttemptCommand(learner, attempt));
        return attempt;
    }
    UUID item(UUID attempt) {
        return jdbc.queryForObject("SELECT i.id FROM attempt_items i JOIN attempt_sections s ON s.id=i.attempt_section_id WHERE s.attempt_id=?", UUID.class, attempt);
    }
    /**
     * The examiner job of the attempt, with {@link #examiner} as the examiner the learner chose. In the product the learner
     * makes that choice; tests about grading, not about choosing, let the default examiner be chosen here.
     */
    UUID humanJob(UUID attempt) {
        UUID job = unchosenHumanJob(attempt);
        jdbc.update("UPDATE grading_jobs SET requested_examiner_id=?,requested_at=CURRENT_TIMESTAMP WHERE id=? AND requested_examiner_id IS NULL", examiner, job);
        return job;
    }
    /** The examiner job as the system created it: nobody chosen yet. A finalised result is sampled for QA at random, which adds a second HUMAN job. */
    UUID unchosenHumanJob(UUID attempt) {
        return jdbc.queryForObject("SELECT j.id FROM grading_jobs j JOIN learner_submissions l ON l.id=j.submission_id JOIN attempt_items i ON i.id=l.attempt_item_id JOIN attempt_sections s ON s.id=i.attempt_section_id WHERE s.attempt_id=? AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'') <> 'QA_SAMPLE'", UUID.class, attempt);
    }
    int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
}
