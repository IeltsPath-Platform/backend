package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.vo.StaffRecords.QuestionStat;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class WorkItemSyncJobTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> "dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ==");
    }

    @Autowired WorkItemSyncJob job;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired com.ieltspath.learning.application.service.WorkItemSync sync;
    @MockitoBean StaffInsightRepository insight;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void setupSources() {
        when(insight.openRemediation()).thenReturn(List.of());
        when(insight.openFlags()).thenReturn(List.of());
        when(insight.contentGapKnowledgePoints()).thenReturn(List.of());
        when(insight.questionStats(anyInt(), anyInt())).thenReturn(List.of());
        when(insight.edgeStats()).thenReturn(List.of());
        when(insight.addWorkItem(any(), any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void doesNotHoldTheAdvisoryLockWhileCallingContent() throws Exception {
        CountDownLatch contentCallStarted = new CountDownLatch(1);
        CountDownLatch releaseContentCall = new CountDownLatch(1);
        UUID questionVersionId = UUID.randomUUID();
        when(insight.questionStats(anyInt(), anyInt())).thenReturn(
                List.of(new QuestionStat(questionVersionId, 20, 0, null, List.of())));
        doAnswer(invocation -> {
            contentCallStarted.countDown();
            assertTrue(releaseContentCall.await(10, TimeUnit.SECONDS), "the test releases the content call");
            return Map.of();
        }).when(content).questionDifficulties(anyCollection());

        CompletableFuture<Integer> run = CompletableFuture.supplyAsync(job::runOnce);
        try {
            assertTrue(contentCallStarted.await(10, TimeUnit.SECONDS), "work item sync reached the Content client");
            assertTrue(canAcquireWorkItemSyncLock(), "another instance can acquire the lock while HTTP is pending");
        } finally {
            releaseContentCall.countDown();
        }
        run.get(10, TimeUnit.SECONDS);
    }

    @Test
    void appliesOnlyOneRoundWhenTwoInstancesRace() throws Exception {
        UUID knowledgePointId = UUID.randomUUID();
        Set<UUID> inserted = ConcurrentHashMap.newKeySet();
        when(insight.contentGapKnowledgePoints()).thenReturn(List.of(knowledgePointId));
        when(insight.addWorkItem(Queue.CONTENT_AUTHOR, WorkKind.CONTENT_GAP, null, knowledgePointId,
                "{\"knowledgePointId\":\"" + knowledgePointId + "\"}")).thenAnswer(invocation ->
                inserted.add(invocation.getArgument(3)));
        WorkItemSyncJob secondInstance = new WorkItemSyncJob(sync, jdbc, transactionManager, true);

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(job::runOnce);
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(secondInstance::runOnce);
        int totalAdded = Math.max(0, first.get(10, TimeUnit.SECONDS)) + Math.max(0, second.get(10, TimeUnit.SECONDS));

        assertEquals(1, totalAdded);
        assertEquals(1, inserted.size());
    }

    private boolean canAcquireWorkItemSyncLock() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT pg_try_advisory_lock(hashtext('learning:work-item-sync'))")) {
            try (var result = statement.executeQuery()) {
                result.next();
                boolean acquired = result.getBoolean(1);
                if (acquired) {
                    try (var unlock = connection.createStatement()) {
                        unlock.execute("SELECT pg_advisory_unlock(hashtext('learning:work-item-sync'))");
                    }
                }
                return acquired;
            }
        }
    }
}
