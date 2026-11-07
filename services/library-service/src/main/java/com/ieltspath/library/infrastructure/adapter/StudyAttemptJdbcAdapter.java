package com.ieltspath.library.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.library.application.port.StudyAttemptRepository;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;

@Repository @RequiredArgsConstructor
public class StudyAttemptJdbcAdapter implements StudyAttemptRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public Optional<AttemptRow> find(UUID user,UUID attempt){return find("id=:key",user,attempt);}
    public Optional<AttemptRow> findStart(UUID user,UUID request) {
        return jdbc.query("select a.*,s.payload_hash request_hash from vocabulary_study_start_requests s join vocabulary_study_attempts a on a.id=s.attempt_id where s.user_id=:user and s.request_id=:request",Map.of("user",user,"request",request),(r,n)->attempt(r,r.getString("request_hash"))).stream().findFirst();
    }
    public void rememberStart(UUID user,UUID request,UUID attempt,String hash) {
        jdbc.update("insert into vocabulary_study_start_requests(user_id,request_id,attempt_id,payload_hash) values(:user,:request,:attempt,:hash)",Map.of("user",user,"request",request,"attempt",attempt,"hash",hash));
    }
    public Optional<AttemptRow> active(UUID user,UUID unit){return find("unit_id=:key and status='IN_PROGRESS'",user,unit);}
    private Optional<AttemptRow> find(String where,UUID user,UUID key) {
        return jdbc.query("select * from vocabulary_study_attempts where user_id=:user and "+where,Map.of("user",user,"key",key),(r,n)->attempt(r)).stream().findFirst();
    }
    public List<StepRow> steps(UUID attempt) {
        return jdbc.query("select * from vocabulary_study_attempt_steps where attempt_id=:id order by step_order",Map.of("id",attempt),(r,n)->
            new StepRow(new StudyStepSnapshot(r.getObject("step_id",UUID.class),r.getInt("step_order"),r.getString("type"),r.getBoolean("graded"),readMap(r.getString("payload")),readMap(r.getString("answer_key"))),StudyUnitJdbcAdapter.instant(r,"completed_at"),r.getObject("first_try_correct",Boolean.class)));
    }
    public void create(AttemptRow a,List<StudyStepSnapshot> steps,Instant now) {
        jdbc.update("""
            insert into vocabulary_study_attempts(id,user_id,topic_id,unit_id,revision,mode,status,started_at,last_activity_at,expires_at,start_request_id,start_payload_hash,graded_step_count)
            values(:id,:user,:topic,:unit,:revision,:mode,'IN_PROGRESS',:now,:now,:expires,:request,:hash,:graded)
            """,Map.ofEntries(Map.entry("id",a.id()),Map.entry("user",a.userId()),Map.entry("topic",a.topicId()),Map.entry("unit",a.unitId()),Map.entry("revision",a.revision()),Map.entry("mode",a.mode()),Map.entry("now",Timestamp.from(now)),Map.entry("expires",Timestamp.from(a.expiresAt())),Map.entry("request",a.startRequestId()),Map.entry("hash",a.startHash()),Map.entry("graded",a.gradedCount())));
        rememberStart(a.userId(),a.startRequestId(),a.id(),a.startHash());
        @SuppressWarnings("unchecked") Map<String,Object>[] batch=steps.stream().map(s->Map.<String,Object>of("attempt",a.id(),"step",s.stepId(),"order",s.order(),"type",s.type(),"graded",s.graded(),"payload",json(s.payload()),"key",json(s.answerKey()))).toArray(Map[]::new);
        jdbc.batchUpdate("insert into vocabulary_study_attempt_steps(attempt_id,step_id,step_order,type,graded,payload,answer_key) values(:attempt,:step,:order,:type,:graded,cast(:payload as jsonb),cast(:key as jsonb))",batch);
    }
    public void expire(UUID attempt){jdbc.update("update vocabulary_study_attempts set status='EXPIRED' where id=:id and status='IN_PROGRESS'",Map.of("id",attempt));}
    public Optional<Submission> submission(UUID user,UUID request) {
        return jdbc.query("select attempt_id,step_id,payload_hash,result from vocabulary_study_submissions where user_id=:user and request_id=:request",Map.of("user",user,"request",request),(r,n)->new Submission(r.getObject("attempt_id",UUID.class),r.getObject("step_id",UUID.class),r.getString("payload_hash"),read(r.getString("result"),StepResult.class))).stream().findFirst();
    }
    public void applyAnswer(UUID attempt,UUID step,boolean correct,boolean first,boolean graded,Instant now) {
        jdbc.update("""
            update vocabulary_study_attempt_steps set completed_at=case when :correct then :now else completed_at end,
              first_try_correct=case when :first and graded then :correct else first_try_correct end
            where attempt_id=:attempt and step_id=:step
            """,Map.of("attempt",attempt,"step",step,"correct",correct,"first",first,"now",Timestamp.from(now)));
        jdbc.update("update vocabulary_study_attempts set first_try_correct_count=first_try_correct_count+:increment,last_activity_at=:now,expires_at=:expires where id=:id",Map.of("id",attempt,"increment",correct&&first&&graded?1:0,"now",Timestamp.from(now),"expires",Timestamp.from(now.plus(Duration.ofDays(14)))));
    }
    public void appendSubmission(UUID user,UUID request,String hash,StudyAnswer answer,boolean first,StepResult result,Instant now) {
        jdbc.update("""
            insert into vocabulary_study_submissions(id,attempt_id,step_id,user_id,request_id,payload_hash,answer,outcome,first_submission,result,created_at)
            values(:id,:attempt,:step,:user,:request,:hash,cast(:answer as jsonb),:outcome,:first,cast(:result as jsonb),:now)
            """,Map.ofEntries(Map.entry("id",result.submissionId()),Map.entry("attempt",result.attemptId()),Map.entry("step",result.stepId()),Map.entry("user",user),Map.entry("request",request),Map.entry("hash",hash),Map.entry("answer",json(answer)),Map.entry("outcome",result.outcome()),Map.entry("first",first),Map.entry("result",json(result)),Map.entry("now",Timestamp.from(now))));
    }
    public boolean recordProgress(AttemptRow a,int accuracy,Instant now) {
        var p=Map.of("user",a.userId(),"unit",a.unitId(),"topic",a.topicId(),"revision",a.revision(),"accuracy",accuracy,"now",Timestamp.from(now));
        boolean first=false;
        if(a.mode().equals("FIRST_COMPLETION"))first=jdbc.update("insert into vocabulary_study_unit_progress(user_id,unit_id,topic_id,first_completed_at,last_completed_at,best_accuracy_percent,completed_revision) values(:user,:unit,:topic,:now,:now,:accuracy,:revision) on conflict(user_id,unit_id) do nothing",p)==1;
        if(!first)jdbc.update("update vocabulary_study_unit_progress set last_completed_at=:now,best_accuracy_percent=greatest(best_accuracy_percent,:accuracy) where user_id=:user and unit_id=:unit",p);
        return first;
    }
    public void complete(UUID attempt,Completion result,Instant now) {
        jdbc.update("update vocabulary_study_attempts set status='COMPLETED',completed_at=:now,last_activity_at=:now,completion=cast(:result as jsonb) where id=:id",Map.of("id",attempt,"now",Timestamp.from(now),"result",json(result)));
    }
    private AttemptRow attempt(ResultSet r)throws SQLException {
        return attempt(r,r.getString("start_payload_hash"));
    }
    private AttemptRow attempt(ResultSet r,String requestHash)throws SQLException {
        String completion=r.getString("completion");
        return new AttemptRow(r.getObject("id",UUID.class),r.getObject("user_id",UUID.class),r.getObject("topic_id",UUID.class),r.getObject("unit_id",UUID.class),r.getInt("revision"),r.getString("mode"),r.getString("status"),r.getTimestamp("started_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getObject("start_request_id",UUID.class),requestHash,r.getInt("graded_step_count"),r.getInt("first_try_correct_count"),completion==null?null:read(completion,Completion.class));
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot serialize study snapshot",e);}}
    private <T>T read(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalStateException("Cannot read study snapshot",e);}}
    private Map<String,Object> readMap(String value){try{return mapper.readValue(value,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException("Cannot read exercise snapshot",e);}}
}
