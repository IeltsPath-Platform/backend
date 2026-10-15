package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import com.ieltspath.library.domain.repository.VocabularyReviewRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Repository
public class JdbcVocabularyReviewRepository implements VocabularyReviewRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcVocabularyReviewRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void lockUser(UUID user) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))",Map.of("key","vocabulary:"+user),rs -> {});
    }
    public boolean isAvailableSense(UUID sense) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
              WHERE s.id=:sense AND s.status='ACTIVE' AND i.status='ACTIVE')
            """,Map.of("sense",sense),Boolean.class));
    }
    public Optional<VocabularyProgress> findProgress(UUID user,UUID sense) {
        return jdbc.query("SELECT * FROM learner_vocabulary_progress WHERE user_id=:user AND vocabulary_sense_id=:sense",
                Map.of("user",user,"sense",sense),(rs,n) -> new VocabularyProgress(user,sense,
                    VocabularyProgress.State.valueOf(rs.getString("state")),rs.getLong("state_version"),rs.getBoolean("active"),
                    instant(rs,"next_review_at"),instant(rs,"last_reviewed_at"),instant(rs,"created_at"))).stream().findFirst();
    }
    public void save(VocabularyProgress p) {
        jdbc.update("""
            INSERT INTO learner_vocabulary_progress(user_id,vocabulary_sense_id,state,state_version,active,next_review_at,last_reviewed_at,created_at)
            VALUES(:user,:sense,:state,:version,:active,:due,:reviewed,:created)
            ON CONFLICT(user_id,vocabulary_sense_id) DO UPDATE SET state=excluded.state,state_version=excluded.state_version,
              active=excluded.active,next_review_at=excluded.next_review_at,last_reviewed_at=excluded.last_reviewed_at
            """,new MapSqlParameterSource().addValue("user",p.userId()).addValue("sense",p.vocabularySenseId())
            .addValue("state",p.state().name()).addValue("version",p.stateVersion()).addValue("active",p.active())
            .addValue("due",Timestamp.from(p.nextReviewAt())).addValue("reviewed",p.lastReviewedAt()==null?null:Timestamp.from(p.lastReviewedAt()))
            .addValue("created",Timestamp.from(p.createdAt())));
    }
    public List<QueueItem> dueItems(UUID user,Instant now,int limit) {
        return jdbc.query("""
            SELECT p.vocabulary_sense_id,p.state_version,i.lemma,COALESCE(s.vietnamese_meaning,s.english_definition) AS meaning,s.example_sentence,false AS answered
            FROM learner_vocabulary_progress p JOIN vocabulary_senses s ON s.id=p.vocabulary_sense_id
            JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
            WHERE p.user_id=:user AND p.active AND p.next_review_at<=:now AND s.status='ACTIVE' AND i.status='ACTIVE'
            ORDER BY p.next_review_at,p.vocabulary_sense_id LIMIT :limit
            """,Map.of("user",user,"now",Timestamp.from(now),"limit",limit),this::item);
    }
    public Optional<Session> findSessionByRequest(UUID user,UUID request) {
        return session("request_id",user,request);
    }
    public Optional<Session> findSession(UUID user,UUID id) { return session("id",user,id); }
    public boolean hasReviewableItems(UUID user,UUID session,Instant now) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM vocabulary_review_sessions r
              JOIN vocabulary_review_items q ON q.session_id=r.id
              JOIN learner_vocabulary_progress p ON p.user_id=r.user_id AND p.vocabulary_sense_id=q.vocabulary_sense_id
              JOIN vocabulary_senses s ON s.id=p.vocabulary_sense_id JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
              WHERE r.id=:session AND r.user_id=:user AND r.expires_at>:now AND NOT q.answered
                AND p.active AND p.state_version=q.state_version AND p.next_review_at<=:now
                AND s.status='ACTIVE' AND i.status='ACTIVE')
            """,Map.of("session",session,"user",user,"now",Timestamp.from(now)),Boolean.class));
    }
    private Optional<Session> session(String key,UUID user,UUID id) {
        var sessions=jdbc.query("SELECT * FROM vocabulary_review_sessions WHERE user_id=:user AND "+key+"=:id",
            Map.of("user",user,"id",id),(rs,n) -> new Session(rs.getObject("id",UUID.class),user,
            rs.getObject("request_id",UUID.class),rs.getInt("queue_limit"),instant(rs,"created_at"),instant(rs,"expires_at"),List.of()));
        if(sessions.isEmpty()) return Optional.empty();
        var s=sessions.getFirst();
        var items=jdbc.query("SELECT * FROM vocabulary_review_items WHERE session_id=:id ORDER BY sort_order",
                Map.of("id",s.id()),this::item);
        return Optional.of(new Session(s.id(),s.userId(),s.requestId(),s.limit(),s.createdAt(),s.expiresAt(),items));
    }
    public void saveSession(Session s) {
        jdbc.update("""
            INSERT INTO vocabulary_review_sessions(id,user_id,request_id,queue_limit,created_at,expires_at)
            VALUES(:id,:user,:request,:limit,:created,:expires)
            """,Map.of("id",s.id(),"user",s.userId(),"request",s.requestId(),"limit",s.limit(),
            "created",Timestamp.from(s.createdAt()),"expires",Timestamp.from(s.expiresAt())));
        SqlParameterSource[] rows=IntStream.range(0,s.items().size()).mapToObj(n -> {
            var i=s.items().get(n);
            return new MapSqlParameterSource().addValue("session",s.id()).addValue("sense",i.vocabularySenseId())
                .addValue("version",i.stateVersion()).addValue("order",n).addValue("lemma",i.lemma())
                .addValue("meaning",i.meaning()).addValue("example",i.exampleSentence());
        }).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
            INSERT INTO vocabulary_review_items(session_id,vocabulary_sense_id,state_version,sort_order,lemma,meaning,example_sentence)
            VALUES(:session,:sense,:version,:order,:lemma,:meaning,:example)
            """,rows);
    }
    public Optional<Receipt> findReceipt(UUID user,UUID request) {
        return jdbc.query("SELECT * FROM vocabulary_review_events WHERE user_id=:user AND request_id=:request",
            Map.of("user",user,"request",request),(rs,n) -> new Receipt(user,request,rs.getObject("session_id",UUID.class),
            rs.getObject("vocabulary_sense_id",UUID.class),rs.getLong("expected_version"),VocabularyProgress.Outcome.valueOf(rs.getString("outcome")),
            VocabularyProgress.State.valueOf(rs.getString("resulting_state")),rs.getLong("resulting_version"),
            instant(rs,"next_review_at"),instant(rs,"reviewed_at"))).stream().findFirst();
    }
    public void saveReceipt(Receipt r) {
        jdbc.update("""
            INSERT INTO vocabulary_review_events(user_id,request_id,session_id,vocabulary_sense_id,expected_version,outcome,
                recall_source,schedule_version,resulting_state,resulting_version,next_review_at,reviewed_at)
            VALUES(:user,:request,:session,:sense,:expected,:outcome,'SELF_RATED',1,:state,:version,:due,:now)
            """,new MapSqlParameterSource().addValue("user",r.userId()).addValue("request",r.requestId()).addValue("session",r.sessionId())
            .addValue("sense",r.vocabularySenseId()).addValue("expected",r.expectedVersion()).addValue("outcome",r.outcome().name())
            .addValue("state",r.state().name()).addValue("version",r.stateVersion()).addValue("due",Timestamp.from(r.nextReviewAt()))
            .addValue("now",Timestamp.from(r.reviewedAt())));
        jdbc.update("UPDATE vocabulary_review_items SET answered=true WHERE session_id=:session AND vocabulary_sense_id=:sense",
                Map.of("session",r.sessionId(),"sense",r.vocabularySenseId()));
    }
    public Summary summarize(UUID user,Instant now) {
        return new Summary(jdbc.query("""
            SELECT p.state,count(*) AS count,count(*) FILTER(WHERE p.state<>'NEW' AND p.next_review_at<=:now) AS due_count,
                   min(p.created_at) AS data_from
            FROM learner_vocabulary_progress p JOIN vocabulary_senses s ON s.id=p.vocabulary_sense_id
            JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
            WHERE p.user_id=:user AND p.active AND s.status='ACTIVE' AND i.status='ACTIVE' GROUP BY p.state
            """,Map.of("user",user,"now",Timestamp.from(now)),(rs,n) -> new StateCount(
            VocabularyProgress.State.valueOf(rs.getString("state")),rs.getLong("count"),rs.getLong("due_count"),instant(rs,"data_from"))));
    }
    private QueueItem item(ResultSet rs,int n) throws SQLException {
        return new QueueItem(rs.getObject("vocabulary_sense_id",UUID.class),rs.getLong("state_version"),rs.getString("lemma"),
                rs.getString("meaning"),rs.getString("example_sentence"),rs.getBoolean("answered"));
    }
    private static Instant instant(ResultSet rs,String column) throws SQLException {
        Timestamp value=rs.getTimestamp(column); return value==null?null:value.toInstant();
    }
}
