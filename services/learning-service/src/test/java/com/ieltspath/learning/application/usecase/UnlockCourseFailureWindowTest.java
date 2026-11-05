package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false", "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Testcontainers(disabledWithoutDocker = true)
class UnlockCourseFailureWindowTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired UnlockCourseUseCase unlock;
    @MockitoSpyBean LearnerCourseUnlockRepository unlocks;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AccessClient access;
    @MockitoBean RefreshLearningTopicsUseCase topics;
    @MockitoBean LearningContentClient learningContent;

    private final UUID user = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final UUID request = UUID.randomUUID();
    private final UUID ledger = UUID.randomUUID();

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM learner_course_unlocks WHERE user_id = ?", user);
        when(topics.execute(user)).thenReturn(List.of(topic()));
    }

    @Test
    void debitTimeoutKeepsIntentPendingAndReconcilerActivatesWhenDebited() {
        when(access.debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request), eq(request.toString()), anyString()))
                .thenThrow(new AccessUnavailableException(null)).thenReturn(ledger);

        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(AccessUnavailableException.class);
        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::status).isEqualTo(LearnerCourseUnlock.Status.PENDING);

        var result = unlock.execute(user, course, "READING", request);

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::ledgerEntryId).isEqualTo(ledger);
        verify(access, times(2)).debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request),
                eq(request.toString()), anyString());
    }

    @Test
    void debitTimeoutAndNotDebitedIsCancelledOrRetried() {
        when(access.debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request), eq(request.toString()), anyString()))
                .thenThrow(new AccessUnavailableException(null)).thenThrow(new InsufficientPointsException());

        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(AccessUnavailableException.class);
        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(InsufficientPointsException.class);

        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::status).isEqualTo(LearnerCourseUnlock.Status.CANCELLED);
        verify(access, times(2)).debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request),
                eq(request.toString()), anyString());
    }

    @Test
    void crashAfterDebitBeforeActivateIsActivatedByReconciler() {
        when(access.debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request), eq(request.toString()), anyString()))
                .thenReturn(ledger);
        doThrow(new IllegalStateException("simulated interruption before activation"))
                .doCallRealMethod().when(unlocks).activate(request, ledger);

        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .hasRootCauseMessage("simulated interruption before activation");
        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::status).isEqualTo(LearnerCourseUnlock.Status.PENDING);

        var result = unlock.execute(user, course, "READING", request);

        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(access, times(2)).debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request),
                eq(request.toString()), anyString());
    }

    @Test
    void retryWithSameKeyWhilePendingDoesNotCreateIntent() {
        when(access.debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request), eq(request.toString()), anyString()))
                .thenThrow(new AccessUnavailableException(null));

        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(AccessUnavailableException.class);
        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(AccessUnavailableException.class);

        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::status).isEqualTo(LearnerCourseUnlock.Status.PENDING);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM learner_course_unlocks WHERE request_id = ?",
                Integer.class, request)).isEqualTo(1);
        verify(access, times(2)).debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request),
                eq(request.toString()), anyString());
    }

    @Test
    void accessBreakerOpenFailsFastWithoutDoubleDebit() {
        when(access.debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request), eq(request.toString()), anyString()))
                .thenThrow(new AccessUnavailableException(null));

        assertThatThrownBy(() -> unlock.execute(user, course, "READING", request))
                .isInstanceOf(AccessUnavailableException.class);

        assertThat(unlocks.findByRequestId(request)).get()
                .extracting(LearnerCourseUnlock::status).isEqualTo(LearnerCourseUnlock.Status.PENDING);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM learner_course_unlocks WHERE request_id = ?",
                Integer.class, request)).isEqualTo(1);
        verify(access).debit(eq(user), anyInt(), eq("COURSE_UNLOCK"), eq(request),
                eq(request.toString()), anyString());
    }

    private TopicResult topic() {
        var course = new TopicResult.Course(this.course, "IELTS_6_0", "IELTS 6.0", new BigDecimal("6.0"), null,
                null, Map.of(LearningSkill.READING, 125), 400,
                Set.of(LearningSkill.READING, LearningSkill.LISTENING));
        return new TopicResult(UUID.randomUUID(), "R1", "Reading", 1,
                com.ieltspath.learning.domain.vo.TopicStatus.IN_PROGRESS, 0, null, LearningSkill.READING,
                true, course, false);
    }
}
