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
    JdbcStudyLock.class,StudyRequestHash.class,StudyGamificationJdbcAdapter.class,StudyGamificationService.class,StudyStreakRecorder.class,StudyPlanRefresher.class,com.ieltspath.library.config.StudyGamificationProperties.class,StudyAttemptJdbcIntegrationTest.Config.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties={"spring.flyway.enabled=true","spring.cloud.config.enabled=false","eureka.client.enabled=false"})
class StudyAttemptJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
    @TestConfiguration static class Config {
        @Bean Clock clock(){return Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"),ZoneOffset.UTC);}
        @Bean ObjectMapper mapper(){return new ObjectMapper().findAndRegisterModules();}
    }
    @org.springframework.boot.test.mock.mockito.MockBean com.ieltspath.library.application.port.StudyAccessClient access;
    @Autowired JdbcTemplate jdbc;
    @Autowired StudyUnitAuthoringService author;
    @Autowired StudyAttemptService study;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedJdbc;
    UUID topic,unit,next,user=UUID.randomUUID();List<UUID>senses;
    @BeforeEach void fixture() {
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
    @Test void resumeIdempotencyOwnershipAndUnlocking() {
        var request=UUID.randomUUID();var start=study.start(user,unit,request,"RESUME_OR_START");var a=start.attempt();
        assertTrue(start.created());assertFalse(study.start(user,unit,request,"RESUME_OR_START").created());
        assertEquals(a.attemptId(),study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt().attemptId());
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(StudyRequestException.class,()->study.start(user,next,request,"RESUME_OR_START")).code());
        assertEquals("VOCAB_UNIT_LOCKED",assertThrows(StudyRequestException.class,()->study.start(user,next,UUID.randomUUID(),"RESUME_OR_START")).code());
        assertEquals("ATTEMPT_NOT_FOUND",assertThrows(StudyRequestException.class,()->study.get(UUID.randomUUID(),a.attemptId())).code());
        var first=a.steps().getFirst();UUID step=UUID.fromString(first.get("stepId").toString());UUID answerRequest=UUID.randomUUID();
        var result=study.answer(user,a.attemptId(),step,answerRequest,new StudyAnswer("WORD",true,null,null,null));
        assertEquals(result,study.answer(user,a.attemptId(),step,answerRequest,new StudyAnswer("WORD",true,null,null,null)));
        finish(user,a.attemptId());var complete=study.complete(user,a.attemptId(),UUID.randomUUID());
        assertTrue(complete.firstCompletion());assertEquals(topic,complete.topicId());assertNotNull(complete.completedAt());assertEquals(List.of(next),complete.unlockedUnitIds());
        assertNull(complete.topicProgress().units().getFirst().currentAttemptId());
        assertEquals(complete,study.complete(user,a.attemptId(),UUID.randomUUID()));
        assertEquals(result,study.answer(user,a.attemptId(),step,answerRequest,new StudyAnswer("WORD",true,null,null,null)));
        var replay=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();assertEquals("REPLAY",replay.mode());
        finish(user,replay.attemptId());assertFalse(study.complete(user,replay.attemptId(),UUID.randomUUID()).firstCompletion());
    }
    @Test void invalidAnswerDoesNotPersistWrongFirstTryIsPermanentAndSnapshotsSurviveRevision() {
        var a=study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();
        UUID later=UUID.fromString(a.steps().get(1).get("stepId").toString());
        assertEquals("STEP_OUT_OF_ORDER",assertThrows(StudyRequestException.class,()->study.answer(user,a.attemptId(),later,UUID.randomUUID(),new StudyAnswer("WORD",true,null,null,null))).code());
        while(study.get(user,a.attemptId()).steps().stream().filter(s->s.get("stepId").toString().equals(study.get(user,a.attemptId()).currentStepId().toString())).findFirst().orElseThrow().get("type").equals("WORD")) answerCurrent(user,a.attemptId());
        UUID step=study.get(user,a.attemptId()).currentStepId();
        assertEquals("INVALID_ANSWER",assertThrows(StudyRequestException.class,()->study.answer(user,a.attemptId(),step,UUID.randomUUID(),new StudyAnswer("CHOICE",null,UUID.randomUUID(),null,null))).code());
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_submissions where attempt_id=? and step_id=?",Integer.class,a.attemptId(),step));
        var payload=study.get(user,a.attemptId()).steps().stream().filter(s->s.get("stepId").toString().equals(step.toString())).findFirst().orElseThrow();
        var key=key(a.attemptId(),step);String correct=key.get("correctOptionId").toString();
        var options=(List<Map<String,Object>>)payload.get("options");UUID wrong=UUID.fromString(options.stream().filter(o->!o.get("optionId").toString().equals(correct)).findFirst().orElseThrow().get("optionId").toString());
        var req=UUID.randomUUID();assertEquals("INCORRECT",study.answer(user,a.attemptId(),step,req,new StudyAnswer("CHOICE",null,wrong,null,null)).outcome());
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(StudyRequestException.class,()->study.answer(user,a.attemptId(),step,req,new StudyAnswer("CHOICE",null,UUID.fromString(correct),null,null))).code());
        answerCurrent(user,a.attemptId());
        author.update(unit,user,new Draft("Changed",null,"REVIEW","Mới",senses));author.publish(unit);
        assertEquals(a.steps(),study.get(user,a.attemptId()).steps());
        finish(user,a.attemptId());var complete=study.complete(user,a.attemptId(),UUID.randomUUID());assertEquals(complete.gradedStepCount()-1,complete.firstTryCorrectCount());
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void expirationCommitsAndNewStartReplacesExpiredAttempt() {
        var a=study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();
        jdbc.update("update vocabulary_study_attempts set expires_at='2020-01-01' where id=?",a.attemptId());
        assertEquals("ATTEMPT_EXPIRED",assertThrows(StudyRequestException.class,()->answerCurrent(user,a.attemptId())).code());
        assertEquals("EXPIRED",jdbc.queryForObject("select status from vocabulary_study_attempts where id=?",String.class,a.attemptId()));
        assertNotEquals(a.attemptId(),study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt().attemptId());
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void simultaneousCompleteReturnsOneStoredCompletion() throws Exception {
        var a=study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();finish(user,a.attemptId());
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {var jobs=List.<Callable<Object>>of(()->study.complete(user,a.attemptId(),UUID.randomUUID()),()->study.complete(user,a.attemptId(),UUID.randomUUID()));var results=executor.invokeAll(jobs);assertEquals(results.get(0).get(),results.get(1).get());}
        finally{executor.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("select count(*) from vocabulary_study_unit_progress where user_id=? and unit_id=?",Integer.class,user,unit));
        assertEquals(2,jdbc.queryForObject("select count(*) from vocabulary_study_xp_ledger where user_id=?",Integer.class,user));
        assertEquals(30,jdbc.queryForObject("select total_xp from vocabulary_study_profiles where user_id=?",Integer.class,user));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void simultaneousStartsShareOneAttempt() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            var tasks=List.<Callable<com.ieltspath.library.application.result.StudyAttemptResult.Start>>of(()->study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START"),()->study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START"));
            var results=executor.invokeAll(tasks);var one=results.get(0).get();var two=results.get(1).get();
            assertEquals(one.attempt().attemptId(),two.attempt().attemptId());assertNotEquals(one.created(),two.created());
        } finally {executor.shutdownNow();}
    }
    @Test void progressUsesExactlyThreeSqlQueriesForHundredTopics() {
        var topics=new ArrayList<UUID>();topics.add(topic);
        for(int i=1;i<100;i++) {
            UUID id=UUID.randomUUID();topics.add(id);
            jdbc.update("insert into vocabulary_topics(id,slug,name,name_vi,status) values(?,?,?,'Chủ đề','ACTIVE')",id,"batch-"+id,"Batch");
        }
        var counted=org.mockito.Mockito.spy(namedJdbc);
        var service=new StudyProgressService(new StudyProgressJdbcAdapter(counted),Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"),ZoneOffset.UTC));
        long started=System.nanoTime();var result=service.execute(user,topics);long elapsed=(System.nanoTime()-started)/1_000_000;
        assertEquals(100,result.size());assertEquals(2,result.getFirst().totalUnits());
        org.mockito.Mockito.verify(counted,org.mockito.Mockito.times(3)).query(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyMap(),org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class));
        System.out.println("Vocabulary study progress: 100 topics, 3 SQL queries, "+elapsed+" ms");
        author.archive(unit);author.archive(next);
        var empty=service.execute(user,List.of(topic)).getFirst();assertEquals(0,empty.totalUnits());assertEquals(4,empty.curriculumVersion());
    }
    @Test void archivedUnitAllowsExistingSnapshotButNoNewAttemptAndPayloadNeverLeaksKeys() throws Exception {
        var a=study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();
        String json=mapper.writeValueAsString(a);assertFalse(json.contains("correctOptionId"));assertFalse(json.contains("answer_key"));assertFalse(json.contains("accepted"));
        assertEquals("ATTEMPT_NOT_COMPLETABLE",assertThrows(StudyRequestException.class,()->study.complete(user,a.attemptId(),UUID.randomUUID())).code());
        author.archive(unit);
        assertEquals("VOCAB_UNIT_NOT_FOUND",assertThrows(StudyRequestException.class,()->study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START")).code());
        finish(user,a.attemptId());assertTrue(study.complete(user,a.attemptId(),UUID.randomUUID()).firstCompletion());
    }
    @Test void resumedStartRequestRemainsIdempotentAfterCompletionAndCannotBeReusedForAnotherUnitOrMode() {
        var original=study.start(user,unit,UUID.randomUUID(),"RESUME_OR_START").attempt();UUID resumedRequest=UUID.randomUUID();
        assertEquals(original.attemptId(),study.start(user,unit,resumedRequest,"RESUME_OR_START").attempt().attemptId());
        finish(user,original.attemptId());study.complete(user,original.attemptId(),UUID.randomUUID());
        var retry=study.start(user,unit,resumedRequest,"RESUME_OR_START");assertFalse(retry.created());assertEquals(original.attemptId(),retry.attempt().attemptId());assertEquals("COMPLETED",retry.attempt().status());
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(StudyRequestException.class,()->study.start(user,next,resumedRequest,"RESUME_OR_START")).code());
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(StudyRequestException.class,()->study.start(user,unit,resumedRequest,"REPLAY")).code());
        var replay=study.start(user,unit,UUID.randomUUID(),"REPLAY").attempt();UUID replayResume=UUID.randomUUID();
        assertEquals(replay.attemptId(),study.start(user,unit,replayResume,"RESUME_OR_START").attempt().attemptId());
        finish(user,replay.attemptId());study.complete(user,replay.attemptId(),UUID.randomUUID());
        assertEquals(replay.attemptId(),study.start(user,unit,replayResume,"RESUME_OR_START").attempt().attemptId());
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
}
