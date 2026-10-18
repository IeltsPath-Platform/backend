package com.ieltspath.community.infrastructure;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.ResolveContentReportCommand;
import com.ieltspath.community.application.usecase.ResolveContentReportUseCase;
import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityConflictException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportDecision;
import com.ieltspath.community.domain.vo.ReportStatus;
import com.ieltspath.community.infrastructure.adapter.ContentReportRepositoryAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
class ContentReportResolutionIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;

    @BeforeAll static void migrate() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test void hideActionsAllPendingReportsForTheSameTargetAndDismissLeavesOtherReportsOpen() {
        UUID target = UUID.randomUUID(), admin = UUID.randomUUID();
        UUID hideId = insertReport(UUID.randomUUID(), ReportContentType.POST, target);
        UUID siblingId = insertReport(UUID.randomUUID(), ReportContentType.POST, target);
        UUID dismissId = insertReport(UUID.randomUUID(), ReportContentType.COMMENT, UUID.randomUUID());
        UUID dismissSiblingId = insertReport(UUID.randomUUID(), ReportContentType.COMMENT,
                jdbc.queryForObject("SELECT content_id FROM community_reports WHERE id=?", UUID.class, dismissId));
        var post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, "reported", "reported body");
        var posts = mock(PostRepository.class);
        var comments = mock(CommentRepository.class);
        when(posts.findById(target)).thenReturn(Optional.of(post));
        when(posts.save(post)).thenReturn(post);
        ContentReportRepository reports = new ContentReportRepositoryAdapter(jdbc);
        var useCase = new ResolveContentReportUseCase(reports, posts, comments);

        transactions.executeWithoutResult(status -> useCase.execute(new CommunityActor(admin, true),
                new ResolveContentReportCommand(hideId, ReportDecision.HIDE_CONTENT)));
        transactions.executeWithoutResult(status -> useCase.execute(new CommunityActor(admin, true),
                new ResolveContentReportCommand(dismissId, ReportDecision.DISMISS)));

        assertEquals(ReportStatus.ACTIONED, reportStatus(hideId));
        assertEquals(ReportStatus.ACTIONED, reportStatus(siblingId));
        assertEquals(ReportStatus.DISMISSED, reportStatus(dismissId));
        assertEquals(ReportStatus.PENDING, reportStatus(dismissSiblingId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM community_reports WHERE content_type='COMMENT' AND content_id=(SELECT content_id FROM community_reports WHERE id=?) AND status='PENDING'", Integer.class, dismissId));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM community_reports WHERE content_type='POST' AND content_id=? AND reviewed_by=? AND reviewed_at IS NOT NULL", Integer.class, target, admin));
    }

    @Test void concurrentAdminsSerializeHideAndLeaveNoPendingReports() throws Exception {
        UUID target = UUID.randomUUID();
        UUID firstReport = insertReport(UUID.randomUUID(), ReportContentType.POST, target);
        UUID secondReport = insertReport(UUID.randomUUID(), ReportContentType.POST, target);
        var firstLocked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var adapter = new ContentReportRepositoryAdapter(jdbc);
        ContentReportRepository lockingRepository = new ContentReportRepository() {
            @Override public ContentReport save(ContentReport report) { return adapter.save(report); }
            @Override public Optional<ContentReport> findByIdForUpdate(UUID id) { return adapter.findByIdForUpdate(id); }
            @Override public Optional<ContentReport> findById(UUID id) { return adapter.findById(id); }
            @Override public List<ContentReport> findPendingByContentForUpdate(ReportContentType type, UUID id) {
                if (Thread.currentThread().getName().equals("second-admin")) secondStarted.countDown();
                var locked = adapter.findPendingByContentForUpdate(type, id);
                if (Thread.currentThread().getName().equals("first-admin")) {
                    firstLocked.countDown();
                    await(releaseFirst);
                }
                return locked;
            }
            @Override public int actionPendingForContent(ReportContentType type, UUID id, UUID admin, Instant at) {
                return adapter.actionPendingForContent(type, id, admin, at);
            }
            @Override public ContentReport update(ContentReport report) { return adapter.update(report); }
            @Override public com.ieltspath.community.domain.vo.CommunityPage<com.ieltspath.community.domain.vo.ContentReportQueueEntry> findPending(int page, int size) {
                return adapter.findPending(page, size);
            }
        };
        var post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, "reported", "reported body");
        var posts = mock(PostRepository.class);
        var comments = mock(CommentRepository.class);
        when(posts.findById(target)).thenReturn(Optional.of(post));
        when(posts.save(any())).thenAnswer(call -> call.getArgument(0));
        var useCase = new ResolveContentReportUseCase(lockingRepository, posts, comments);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                Thread.currentThread().setName("first-admin");
                return transactions.execute(status -> useCase.execute(new CommunityActor(UUID.randomUUID(), true),
                        new ResolveContentReportCommand(firstReport, ReportDecision.HIDE_CONTENT)));
            });
            assertTrue(firstLocked.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> {
                Thread.currentThread().setName("second-admin");
                return transactions.execute(status -> useCase.execute(new CommunityActor(UUID.randomUUID(), true),
                        new ResolveContentReportCommand(secondReport, ReportDecision.HIDE_CONTENT)));
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            releaseFirst.countDown();
            assertEquals(ReportStatus.ACTIONED, first.get(10, TimeUnit.SECONDS).status());
            ExecutionException conflict = assertThrows(ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS));
            assertInstanceOf(CommunityConflictException.class, conflict.getCause());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
        assertEquals(ReportStatus.ACTIONED, reportStatus(firstReport));
        assertEquals(ReportStatus.ACTIONED, reportStatus(secondReport));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM community_reports WHERE content_type='POST' AND content_id=? AND status='PENDING'", Integer.class, target));
    }

    private static UUID insertReport(UUID reporter, ReportContentType type, UUID target) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO community_reports(id,reporter_id,content_type,content_id,reason,status,created_at) VALUES(?,?,?,?,?,'PENDING',now())",
                id, reporter, type.name(), target, "Reason");
        return id;
    }

    private static ReportStatus reportStatus(UUID id) {
        return ReportStatus.valueOf(jdbc.queryForObject("SELECT status FROM community_reports WHERE id=?", String.class, id));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for concurrent transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
