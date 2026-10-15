package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.usecase.VocabularyReviewUseCase;
import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import com.ieltspath.library.domain.exception.ConflictException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
class VocabularyReviewPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate jdbc;
    static VocabularyReviewUseCase reviews;
    static TransactionTemplate tx;
    @BeforeAll static void setup() {
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        reviews=new VocabularyReviewUseCase(new JdbcVocabularyReviewRepository(new NamedParameterJdbcTemplate(ds)));
    }
    private UUID sense() {
        UUID word=UUID.randomUUID(),sense=UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_items(id,lemma,normalized_lemma) VALUES(?, 'word','word')",word);
        jdbc.update("INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence) VALUES(?,?,'NOUN','meaning','example')",sense,word);
        return sense;
    }
    private <T>T transaction(Supplier<T> action) { return tx.execute(s->action.get()); }
    @Test void addDedupeReplayArchiveAndDueSummaryUsePersistentState() {
        UUID user=UUID.randomUUID(),sense=sense(),startId=UUID.randomUUID(),answerId=UUID.randomUUID();
        transaction(()->reviews.study(user,sense,true));transaction(()->reviews.study(user,sense,true));
        assertEquals(1,reviews.statistics(user).total());assertEquals(0,reviews.statistics(user).dueCount());
        var session=transaction(()->reviews.start(user,startId,20));
        assertEquals(1,session.items().size());assertEquals(session.id(),transaction(()->reviews.start(user,startId,20)).id());
        var answer=transaction(()->reviews.answer(user,session.id(),answerId,sense,0,VocabularyProgress.Outcome.REMEMBERED));
        assertEquals(answer,transaction(()->reviews.answer(user,session.id(),answerId,sense,0,VocabularyProgress.Outcome.REMEMBERED)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM vocabulary_review_events WHERE user_id=?",Integer.class,user));
        assertTrue(transaction(()->reviews.start(user,UUID.randomUUID(),20)).items().isEmpty());
        transaction(()->reviews.study(user,sense,false));assertEquals(0,reviews.statistics(user).total());
        var reactivated=transaction(()->reviews.study(user,sense,true));assertEquals(answer.state(),reactivated.state());
        assertEquals(answer.nextReviewAt(),reactivated.nextReviewAt());
    }
    @Test void simultaneousAnswersFromTwoSessionsAdvanceOnlyOnce() throws Exception {
        UUID user=UUID.randomUUID(),sense=sense();transaction(()->reviews.study(user,sense,true));
        var first=transaction(()->reviews.start(user,UUID.randomUUID(),20));
        var second=transaction(()->reviews.start(user,UUID.randomUUID(),20));
        var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results=new ArrayList<>();
            for(UUID session:List.of(first.id(),second.id())) results.add(pool.submit(()->{
                gate.await();
                try { transaction(()->reviews.answer(user,session,UUID.randomUUID(),sense,0,VocabularyProgress.Outcome.REMEMBERED));return true; }
                catch(ConflictException e) { return false; }
            }));
            gate.countDown();
            assertNotEquals(results.get(0).get(15,TimeUnit.SECONDS),results.get(1).get(15,TimeUnit.SECONDS));
        }
        assertEquals(1,jdbc.queryForObject("SELECT state_version FROM learner_vocabulary_progress WHERE user_id=?",Long.class,user));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM vocabulary_review_events WHERE user_id=?",Integer.class,user));
    }
    @Test void inactiveCatalogAndArchivedProgressAreAbsentFromQueueAndSummary() {
        UUID user=UUID.randomUUID(),sense=sense();transaction(()->reviews.study(user,sense,true));
        jdbc.update("UPDATE vocabulary_senses SET status='INACTIVE' WHERE id=?",sense);
        assertEquals(0,reviews.statistics(user).total());
        assertTrue(transaction(()->reviews.start(user,UUID.randomUUID(),20)).items().isEmpty());
    }
}
