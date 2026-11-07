package com.ieltspath.library.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import com.ieltspath.library.application.service.*;
import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.infrastructure.adapter.*;
import com.ieltspath.library.domain.study.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker=true)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({StudyUnitJdbcAdapter.class,VocabularyAudioUrlResolver.class,StudyUnitAuthoringService.class,
    StudyExercisePreview.class,StudyAttemptService.class,StudyProgressService.class,StudyAttemptJdbcAdapter.class,StudyProgressJdbcAdapter.class,
    JdbcStudyLock.class,StudyRequestHash.class,StudyGamificationJdbcAdapter.class,StudyGamificationService.class,StudyStreakRecorder.class,StudyPurchaseCoordinator.class,StudyPlanRefresher.class,com.ieltspath.library.config.StudyGamificationProperties.class,StudyGamificationJdbcIntegrationTest.Config.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties={"spring.flyway.enabled=true","spring.cloud.config.enabled=false","eureka.client.enabled=false"})
class StudyGamificationJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
    @TestConfiguration static class Config {
        @Bean MutableClock clock(){return new MutableClock();}
        @Bean ObjectMapper mapper(){return new ObjectMapper().findAndRegisterModules();}
    }
    @org.springframework.boot.test.mock.mockito.MockBean com.ieltspath.library.application.port.StudyAccessClient access;
    @Autowired com.ieltspath.library.application.port.StudyGamificationRepository gammaRepo;
    @Autowired StudyGamificationService gamma;
    @Autowired StudyPurchaseCoordinator purchases;
    @Autowired StudyStreakRecorder streak;
    @Autowired StudyPlanRefresher refresher;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @Autowired com.ieltspath.library.application.port.StudyLock studyLock;
    @Autowired MutableClock clock;
    @org.springframework.boot.test.mock.mockito.SpyBean StudyGamificationJdbcAdapter gammaAdapter;
    @Autowired JdbcTemplate jdbc;
    @Autowired StudyUnitAuthoringService author;
    @Autowired StudyAttemptService study;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedJdbc;
    UUID topic,unit,next,user=UUID.randomUUID();List<UUID>senses;
    @BeforeEach void fixture() {
        clock.at=Instant.parse("2026-11-07T17:30:00Z");
        topic=UUID.randomUUID();senses=new ArrayList<>();
        jdbc.update("insert into vocabulary_topics(id,slug,name,name_vi,status) values(?,?,?,'Từ vựng','ACTIVE')",topic,"attempt-"+topic,"Attempts");
        for(int i=0;i<4;i++) {
            UUID item=UUID.randomUUID(),sense=UUID.randomUUID();senses.add(sense);String lemma="word"+i+item.toString().substring(0,4);
            jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')",item,lemma,lemma);
            jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,topic_id,status) values(?,?,'NOUN',?, ?,?,'ACTIVE')",sense,item,"nghĩa "+i,"A "+lemma+".",topic);
        }
        unit=author.create(topic,user,new Draft("First",null,"LEARN","Học từ",senses)).unitId();author.publish(unit);
        next=author.create(topic,user,new Draft("Second",null,"LEARN","Học từ",senses)).unitId();author.publish(next);
    }
    Map<String,Object> key(UUID attempt,UUID step) {try{return mapper.readValue(jdbc.queryForObject("select answer_key::text from vocabulary_study_attempt_steps where attempt_id=? and step_id=?",String.class,attempt,step),Map.class);}catch(Exception e){throw new RuntimeException(e);}}
    void finish(UUID user,UUID attempt){while(study.get(user,attempt).currentStepId()!=null)answerCurrent(user,attempt);}
    void answerCurrent(UUID user,UUID attempt) {
        var a=study.get(user,attempt);UUID step=a.currentStepId();var key=key(attempt,step);
        var payload=a.steps().stream().filter(s->s.get("stepId").toString().equals(step.toString())).findFirst().orElseThrow();
        StudyAnswer answer=switch(payload.get("type").toString()){
            case "WORD"->new StudyAnswer("WORD",true,null,null,null);
            case "CHOICE"->new StudyAnswer("CHOICE",null,UUID.fromString(key.get("correctOptionId").toString()),null,null);
            case "MATCH"->new StudyAnswer("MATCH",null,null,((List<Map<String,String>>)key.get("pairs")).stream().map(p->new StudyAnswer.Pair(UUID.fromString(p.get("leftOptionId")),UUID.fromString(p.get("rightOptionId")))).toList(),null);
            default->new StudyAnswer("FILL_TEXT",null,null,null,((List<String>)key.get("accepted")).getFirst());
        };
        study.answer(user,attempt,step,UUID.randomUUID(),answer);
    }
    @Test void xpGrantedOncePerUnitLocalDatesStayFixedAndReplayStillContinuesStreak() {
        var a=start();finish(user,a.attemptId());var complete=study.complete(user,a.attemptId(),UUID.randomUUID());
        assertEquals(30,complete.reward().xpEarned());assertEquals(30,complete.reward().summary().xp().totalXp());
        assertEquals(complete,study.complete(user,a.attemptId(),UUID.randomUUID()));
        assertEquals(2,jdbc.queryForObject("select count(*) from vocabulary_study_xp_ledger where user_id=?",Integer.class,user));
        assertEquals(LocalDate.of(2026,11,8),jdbc.queryForObject("select min(local_date) from vocabulary_study_xp_ledger where user_id=?",LocalDate.class,user));
        gamma.settings(user,"UTC");assertEquals(LocalDate.of(2026,11,8),jdbc.queryForObject("select min(local_date) from vocabulary_study_xp_ledger where user_id=?",LocalDate.class,user));
        gamma.settings(user,"Asia/Ho_Chi_Minh");clock.at=clock.at.plus(Duration.ofDays(1));
        var replay=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();finish(user,replay.attemptId());
        var repeated=study.complete(user,replay.attemptId(),UUID.randomUUID());assertEquals(0,repeated.reward().xpEarned());assertEquals(2,repeated.reward().summary().streak().currentDays());
        author.update(unit,user,new Draft("Revised",null,"REVIEW","Mới",senses));author.publish(unit);
        var revised=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();finish(user,revised.attemptId());assertEquals(0,study.complete(user,revised.attemptId(),UUID.randomUUID()).reward().xpEarned());
        assertEquals(30,gamma.summary(user).xp().totalXp());
    }
    @Test void incorrectFirstTryLosesOneHeartOnlyAndPreventsPerfectBonusForever() {
        var a=start();advanceToChoice(a.attemptId());UUID request=UUID.randomUUID();
        var first=wrong(a.attemptId(),request);assertTrue(first.heartLost());assertEquals(4,first.hearts().hearts());
        var second=wrong(a.attemptId(),UUID.randomUUID());assertFalse(second.heartLost());assertEquals(4,second.hearts().hearts());
        setHearts(0);assertEquals(first,wrong(a.attemptId(),request));
        assertEquals("VOCAB_NO_HEARTS",assertThrows(StudyRequestException.class,()->answerCurrent(user,a.attemptId())).code());
        clock.at=clock.at.plus(Duration.ofHours(5));answerCurrent(user,a.attemptId());finish(user,a.attemptId());
        var completed=study.complete(user,a.attemptId(),UUID.randomUUID());assertEquals(20,completed.reward().xpEarned());assertEquals(0,completed.reward().xpBreakdown().perfectBonus());
        var replay=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();finish(user,replay.attemptId());assertEquals(0,study.complete(user,replay.attemptId(),UUID.randomUUID()).reward().xpEarned());
    }
    @Test void zeroHeartsAllowsWordsExistingAttemptsAndReplayButBlocksNewGradedStudy() {
        var done=start();finish(user,done.attemptId());study.complete(user,done.attemptId(),UUID.randomUUID());
        UUID original=UUID.randomUUID();var active=study.start(user,next,original,"RESUME_OR_START").attempt();setHearts(0);
        assertEquals(active.attemptId(),study.start(user,next,original,"RESUME_OR_START").attempt().attemptId());assertNotNull(study.get(user,active.attemptId()));
        advanceToChoice(active.attemptId());assertEquals(0,gamma.summary(user).hearts().hearts());
        assertEquals("VOCAB_NO_HEARTS",assertThrows(StudyRequestException.class,()->answerCurrent(user,active.attemptId())).code());
        UUID otherTopic=UUID.randomUUID();jdbc.update("insert into vocabulary_topics(id,slug,name,name_vi,status) values(?,?,'Other','Khác','ACTIVE')",otherTopic,"other-"+otherTopic);
        UUID other=author.create(otherTopic,user,new Draft("Other",null,"LEARN","Mới",senses)).unitId();author.publish(other);
        assertEquals("VOCAB_NO_HEARTS",assertThrows(StudyRequestException.class,()->study.start(user,other,UUID.randomUUID(),"RESUME_OR_START")).code());
        var replay=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();advanceToChoice(replay.attemptId());var wrong=wrong(replay.attemptId(),UUID.randomUUID());assertFalse(wrong.heartLost());assertNull(wrong.hearts());
        clock.at=clock.at.plus(Duration.ofHours(5));assertEquals(1,gamma.summary(user).hearts().hearts());answerCurrent(user,active.attemptId());
    }
    @Test void premiumLevelUpRefillsSevenAndFlashcardActivityNeverCreatesProfileOrXp() {
        tx(()->streak.recordActivity(user,clock.instant()));assertTrue(gammaRepo.profile(user).isEmpty());
        assertEquals(1,gamma.summary(user).streak().currentDays());assertEquals(0,gamma.summary(user).xp().totalXp());assertTrue(gammaRepo.profile(user).isEmpty());
        org.mockito.Mockito.when(access.premium()).thenReturn(true);var a=start();assertEquals(7,gamma.summary(user).hearts().maxHearts());
        jdbc.update("update vocabulary_study_profiles set total_xp=490,level=1,hearts=1 where user_id=?",user);
        finish(user,a.attemptId());var result=study.complete(user,a.attemptId(),UUID.randomUUID());assertTrue(result.reward().leveledUp());assertEquals(7,result.reward().summary().hearts().hearts());assertFalse(result.reward().streakIncreased());
    }
    @Test void refillIsDurableIdempotentAndAccessRunsOutsideTransactions() {
        start();setHearts(2);UUID request=UUID.randomUUID();
        org.mockito.Mockito.when(access.debit(org.mockito.ArgumentMatchers.eq(user),org.mockito.ArgumentMatchers.eq(5L),org.mockito.ArgumentMatchers.eq("HEART_REFILL"),org.mockito.ArgumentMatchers.eq(request),org.mockito.ArgumentMatchers.eq("vocab-study:"+request),org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return new com.ieltspath.library.application.port.StudyAccessClient.PointsDebit(UUID.randomUUID(),37);});
        var result=purchases.purchase(user,request,"HEART_REFILL");assertEquals(5,result.hearts().hearts());assertEquals(5,result.pointsSpent());assertEquals(37,result.pointsBalanceAfter());
        assertEquals(result,purchases.purchase(user,request,"HEART_REFILL"));
        org.mockito.Mockito.verify(access,org.mockito.Mockito.times(1)).debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(StudyRequestException.class,()->purchases.purchase(user,request,"STREAK_FREEZE")).code());
        assertEquals("HEARTS_FULL",assertThrows(StudyRequestException.class,()->purchases.purchase(user,UUID.randomUUID(),"HEART_REFILL")).code());
    }
    @Test void insufficientPointsDeletesIntentButUnavailableRetainsItForRetry() {
        start();setHearts(2);UUID insufficient=UUID.randomUUID();
        org.mockito.Mockito.when(access.debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenThrow(new com.ieltspath.library.application.port.StudyAccessClient.InsufficientPointsException());
        assertEquals("INSUFFICIENT_POINTS",assertThrows(StudyRequestException.class,()->purchases.purchase(user,insufficient,"HEART_REFILL")).code());assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_purchases where request_id=?",Integer.class,insufficient));assertEquals(2,gamma.summary(user).hearts().hearts());
        UUID pending=UUID.randomUUID();org.mockito.Mockito.doThrow(new com.ieltspath.library.application.port.StudyAccessClient.AccessUnavailableException()).when(access).debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        assertEquals("ACCESS_UNAVAILABLE",assertThrows(StudyRequestException.class,()->purchases.purchase(user,pending,"HEART_REFILL")).code());assertEquals("PENDING",jdbc.queryForObject("select status from vocabulary_study_purchases where request_id=?",String.class,pending));assertEquals(2,gamma.summary(user).hearts().hearts());
        org.mockito.Mockito.doReturn(new com.ieltspath.library.application.port.StudyAccessClient.PointsDebit(UUID.randomUUID(),7)).when(access).debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        assertEquals(5,purchases.purchase(user,pending,"HEART_REFILL").hearts().hearts());
    }
    @Test void crashAfterDebitRollsBackEffectAndRetryUsesSameIntentAndPrice() {
        start();setHearts(2);UUID request=UUID.randomUUID(),ledger=UUID.randomUUID();
        org.mockito.Mockito.when(access.debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenReturn(new com.ieltspath.library.application.port.StudyAccessClient.PointsDebit(ledger,22));
        org.mockito.Mockito.doThrow(new IllegalStateException("simulated post-debit failure")).doCallRealMethod().when(gammaAdapter).finishPurchase(org.mockito.ArgumentMatchers.eq(request),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        assertThrows(IllegalStateException.class,()->purchases.purchase(user,request,"HEART_REFILL"));assertEquals(2,gamma.summary(user).hearts().hearts());assertEquals("PENDING",jdbc.queryForObject("select status from vocabulary_study_purchases where request_id=?",String.class,request));
        assertEquals(5,purchases.purchase(user,request,"HEART_REFILL").hearts().hearts());assertEquals(ledger,jdbc.queryForObject("select access_ledger_id from vocabulary_study_purchases where request_id=?",UUID.class,request));
    }
    @Test void freezeRequiresStreakChargesConsecutivePriceAndEnforcesOwnershipAndLimit() {
        assertEquals("NO_STREAK_TO_PROTECT",assertThrows(StudyRequestException.class,()->purchases.purchase(user,UUID.randomUUID(),"STREAK_FREEZE")).code());
        tx(()->streak.recordActivity(user,clock.instant()));org.mockito.Mockito.when(access.debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenReturn(new com.ieltspath.library.application.port.StudyAccessClient.PointsDebit(UUID.randomUUID(),80));
        clock.at=clock.at.plus(Duration.ofDays(1));UUID request=UUID.randomUUID();var first=purchases.purchase(user,request,"STREAK_FREEZE");assertEquals(10,first.pointsSpent());assertTrue(first.streak().freezeAvailable());assertEquals(first,purchases.purchase(user,request,"STREAK_FREEZE"));
        assertEquals("FREEZE_ALREADY_OWNED",assertThrows(StudyRequestException.class,()->purchases.purchase(user,UUID.randomUUID(),"STREAK_FREEZE")).code());
        clock.at=clock.at.plus(Duration.ofDays(1));assertEquals(20,purchases.purchase(user,UUID.randomUUID(),"STREAK_FREEZE").pointsSpent());
        LocalDate today=LocalDate.ofInstant(clock.instant(),ZoneId.of("Asia/Ho_Chi_Minh"));tx(()->gammaRepo.saveStreak(user,new StudyStreakState(1,1,today.minusDays(6),today.minusDays(6),today.minusDays(1),false,null,today.minusDays(1),5,0),clock.instant()));
        assertEquals("LIMIT",gamma.summary(user).streak().freezeBlockedReason());assertEquals("FREEZE_LIMIT",assertThrows(StudyRequestException.class,()->purchases.purchase(user,UUID.randomUUID(),"STREAK_FREEZE")).code());
    }
    @Test void purchaseAndPlanRefreshRejectAnOuterTransactionBeforeAnyHttp() {
        new org.springframework.transaction.support.TransactionTemplate(manager).execute(status->{
            assertThrows(IllegalStateException.class,()->purchases.purchase(user,UUID.randomUUID(),"HEART_REFILL"));
            assertThrows(IllegalStateException.class,()->refresher.ensureFresh(user));return null;
        });
        org.mockito.Mockito.verifyNoInteractions(access);
    }
    @Test void concurrentRetryOfSamePurchaseHasOneEffectAndOneLogicalDebit() throws Exception {
        concurrentPurchases(false);
    }
    @Test void twoDifferentPendingRefillsCanBothDebitBeforeEitherEffectAsAccepted() throws Exception {
        concurrentPurchases(true);
    }
    void concurrentPurchases(boolean distinct) throws Exception {
        start();setHearts(2);UUID first=UUID.randomUUID(),second=distinct?UUID.randomUUID():first;
        var debitCalls=new CountDownLatch(2);var ledgers=new java.util.concurrent.ConcurrentHashMap<String,com.ieltspath.library.application.port.StudyAccessClient.PointsDebit>();var logicalDebits=new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.when(access.debit(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv->{
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());debitCalls.countDown();assertTrue(debitCalls.await(10,TimeUnit.SECONDS));
            return ledgers.computeIfAbsent(inv.getArgument(4),key->{logicalDebits.incrementAndGet();return new com.ieltspath.library.application.port.StudyAccessClient.PointsDebit(UUID.randomUUID(),30);});
        });
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            var tasks=List.<Callable<com.ieltspath.library.application.result.StudyGamificationResult.PurchaseResult>>of(()->purchases.purchase(user,first,"HEART_REFILL"),()->purchases.purchase(user,second,"HEART_REFILL"));
            var results=executor.invokeAll(tasks);assertEquals(results.get(0).get(),results.get(1).get());
        } finally {executor.shutdownNow();}
        assertEquals(distinct?2:1,logicalDebits.get());assertEquals(5,gamma.summary(user).hearts().hearts());
        assertEquals(distinct?2:1,jdbc.queryForObject("select count(*) from vocabulary_study_purchases where user_id=? and status='DONE'",Integer.class,user));
    }
    static class MutableClock extends Clock {
        Instant at=Instant.parse("2026-11-07T17:30:00Z");
        public java.time.ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(java.time.ZoneId zone){return this;}
        public Instant instant(){return at;}
    }
    void tx(Runnable action){new org.springframework.transaction.support.TransactionTemplate(manager).execute(s->{studyLock.lock(user);action.run();return null;});}
    com.ieltspath.library.application.result.StudyAttemptResult.Attempt start(){return study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();}
    void setHearts(int hearts){jdbc.update("update vocabulary_study_profiles set hearts=?,hearts_regen_at=? where user_id=?",hearts,java.sql.Timestamp.from(clock.instant()),user);}
    com.ieltspath.library.application.result.StudyAttemptResult.StepResult wrong(UUID attempt,UUID request) {
        var a=study.get(user,attempt);UUID step=a.currentStepId();var key=key(attempt,step);
        var payload=a.steps().stream().filter(s->s.get("stepId").toString().equals(step.toString())).findFirst().orElseThrow();
        var options=(List<Map<String,Object>>)payload.get("options");String correct=key.get("correctOptionId").toString();
        UUID option=UUID.fromString(options.stream().filter(o->!o.get("optionId").toString().equals(correct)).findFirst().orElseThrow().get("optionId").toString());
        return study.answer(user,attempt,step,request,new StudyAnswer("CHOICE",null,option,null,null));
    }
    void advanceToChoice(UUID attempt){while(study.get(user,attempt).steps().stream().filter(s->s.get("stepId").toString().equals(study.get(user,attempt).currentStepId().toString())).findFirst().orElseThrow().get("type").equals("WORD"))answerCurrent(user,attempt);}
}
