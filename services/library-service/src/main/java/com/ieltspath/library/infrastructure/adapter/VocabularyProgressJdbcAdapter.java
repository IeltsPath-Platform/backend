package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.VocabularyProgressReader;
import com.ieltspath.library.application.result.RecentResult;
import com.ieltspath.library.application.result.SetProgressResult;
import com.ieltspath.library.application.result.TopicProgressResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VocabularyProgressJdbcAdapter implements VocabularyProgressReader {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public int weeklyTarget(UUID userId, int defaultTarget) {
        return jdbc.queryForObject("select coalesce((select target_words from vocabulary_weekly_goals where user_id=:user),:default)",
                new MapSqlParameterSource().addValue("user", userId).addValue("default", defaultTarget), Integer.class);
    }

    @Override
    @Transactional
    public void saveWeeklyTarget(UUID userId, int target) {
        jdbc.update("insert into vocabulary_weekly_goals(user_id,target_words,updated_at) values(:user,:target,current_timestamp) "
                        + "on conflict(user_id) do update set target_words=excluded.target_words,updated_at=excluded.updated_at",
                new MapSqlParameterSource().addValue("user", userId).addValue("target", target));
    }

    @Override
    public long weeklyProgress(UUID userId, Instant start, Instant endExclusive) {
        String sql = "select count(distinct sense_id) from ("
                + "select vocabulary_sense_id sense_id from flashcards where user_id=:user and source_type='VOCABULARY_SENSE' and status='ACTIVE' and created_at>=:start and created_at<:end "
                + "union all select vocabulary_sense_id from flashcard_review_events where user_id=:user and vocabulary_sense_id is not null and reviewed_at>=:start and reviewed_at<:end"
                + ") activity";
        return jdbc.queryForObject(sql, new MapSqlParameterSource().addValue("user", userId)
                .addValue("start", Timestamp.from(start)).addValue("end", Timestamp.from(endExclusive)), Long.class);
    }

    @Override
    public List<LocalDate> reviewDays(UUID userId, LocalDate start, LocalDate endExclusive, ZoneId timezone) {
        Instant from = start.atStartOfDay(timezone).toInstant();
        Instant to = endExclusive.atStartOfDay(timezone).toInstant();
        return jdbc.query("select distinct (reviewed_at at time zone :zone)::date from flashcard_review_events "
                        + "where user_id=:user and reviewed_at>=:start and reviewed_at<:end order by 1",
                new MapSqlParameterSource().addValue("zone", timezone.getId()).addValue("user", userId)
                        .addValue("start", Timestamp.from(from)).addValue("end", Timestamp.from(to)),
                (rs, row) -> rs.getObject(1, LocalDate.class));
    }

    @Override
    public List<BandCounts> vocabularyMap(UUID userId, Instant now) {
        String sql = "with levels(cefr,ord) as (values ('A1',1),('A2',2),('B1',3),('B2',4),('C1',5),('C2',6)), "
                + "catalog as (select s.id,s.cefr_level,f.leitner_box,f.review_count,f.next_review_at from vocabulary_senses s "
                + "join vocabulary_items i on i.id=s.vocabulary_item_id and i.status='ACTIVE' and s.status='ACTIVE' "
                + "left join flashcards f on f.vocabulary_sense_id=s.id and f.user_id=:user and f.source_type='VOCABULARY_SENSE' and f.status='ACTIVE' "
                + "where s.cefr_level is not null) "
                + "select l.cefr,count(c.id) total,count(c.id) filter(where coalesce(c.review_count,0)>0 or coalesce(c.leitner_box,1)>=2) learned, "
                + "count(c.id) filter(where c.leitner_box in (2,3)) learning, "
                + "count(c.id) filter(where c.next_review_at is not null and c.next_review_at<=:now) needs_review "
                + "from levels l left join catalog c on c.cefr_level=l.cefr group by l.cefr,l.ord order by l.ord";
        return jdbc.query(sql, new MapSqlParameterSource().addValue("user", userId).addValue("now", Timestamp.from(now)),
                (rs, row) -> new BandCounts(rs.getString("cefr"), rs.getLong("total"), rs.getLong("learned"),
                        rs.getLong("learning"), rs.getLong("needs_review")));
    }

    @Override
    public List<TopicCounts> topicsProgress(UUID userId, UUID setId, Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("user", userId)
                .addValue("now", Timestamp.from(now));
        String setFilter = "";
        if (setId != null) {
            setFilter = " and exists(select 1 from vocabulary_set_items si join vocabulary_sets vs on vs.id=si.set_id and vs.status='ACTIVE' where si.set_id=:setId and si.sense_id=s.id)";
            params.addValue("setId", setId);
        }
        String sql = "with review_by_sense as (select vocabulary_sense_id,max(reviewed_at) last_review from flashcard_review_events "
                + "where user_id=:user and vocabulary_sense_id is not null group by vocabulary_sense_id), "
                + "topic_senses as (select t.id topic_id,t.name,s.id sense_id,coalesce(f.review_count,0) review_count,coalesce(f.leitner_box,1) leitner_box,r.last_review "
                + "from vocabulary_topics t join vocabulary_senses s on s.topic_id=t.id and s.status='ACTIVE' "
                + "join vocabulary_items i on i.id=s.vocabulary_item_id and i.status='ACTIVE' "
                + "left join flashcards f on f.vocabulary_sense_id=s.id and f.user_id=:user and f.source_type='VOCABULARY_SENSE' and f.status='ACTIVE' "
                + "left join review_by_sense r on r.vocabulary_sense_id=s.id where t.status='ACTIVE'" + setFilter + ") "
                + "select topic_id,name,count(*) total,count(*) filter(where review_count>0 or leitner_box>=2) learned,"
                + "max(last_review) last_review from topic_senses group by topic_id,name order by max(last_review) desc nulls last,name";
        return jdbc.query(sql, params, (rs, row) -> new TopicCounts(rs.getObject("topic_id", UUID.class), rs.getString("name"),
                rs.getLong("total"), rs.getLong("learned"), instant(rs, "last_review")));
    }

    @Override
    public List<TopicProgressResult.NextWord> nextWords(UUID userId, UUID topicId, UUID setId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("user", userId).addValue("topic", topicId);
        String setFilter = "";
        if (setId != null) {
            setFilter = " and exists(select 1 from vocabulary_set_items si join vocabulary_sets vs on vs.id=si.set_id and vs.status='ACTIVE' where si.set_id=:setId and si.sense_id=s.id)";
            params.addValue("setId", setId);
        }
        String sql = "select s.id sense_id,i.id item_id,i.lemma,s.vietnamese_meaning from vocabulary_senses s "
                + "join vocabulary_items i on i.id=s.vocabulary_item_id and i.status='ACTIVE' where s.topic_id=:topic and s.status='ACTIVE'" + setFilter
                + " and not exists(select 1 from flashcards f where f.user_id=:user and f.vocabulary_sense_id=s.id and f.source_type='VOCABULARY_SENSE' and f.status='ACTIVE' and (f.review_count>0 or f.leitner_box>=2)) "
                + "order by i.normalized_lemma,s.sort_order,s.id limit 5";
        return jdbc.query(sql, params, (rs, row) -> new TopicProgressResult.NextWord(rs.getObject("sense_id", UUID.class),
                rs.getObject("item_id", UUID.class), rs.getString("lemma"), rs.getString("vietnamese_meaning")));
    }

    @Override
    public Optional<SetProgressResult> setProgress(UUID userId, UUID setId, Instant now) {
        String sql = "select st.id,count(i.id) total,count(i.id) filter(where coalesce(f.review_count,0)>0 or coalesce(f.leitner_box,1)>=2) learned, "
                + "count(i.id) filter(where f.leitner_box in (2,3)) learning,count(i.id) filter(where f.next_review_at is not null and f.next_review_at<=:now) needs_review "
                + "from vocabulary_sets st left join vocabulary_set_items si on si.set_id=st.id "
                + "left join vocabulary_senses s on s.id=si.sense_id and s.status='ACTIVE' "
                + "left join vocabulary_items i on i.id=s.vocabulary_item_id and i.status='ACTIVE' "
                + "left join flashcards f on f.vocabulary_sense_id=s.id and f.user_id=:user and f.source_type='VOCABULARY_SENSE' and f.status='ACTIVE' "
                + "where st.id=:set and st.status='ACTIVE' group by st.id";
        List<SetProgressResult> rows = jdbc.query(sql, new MapSqlParameterSource().addValue("user", userId)
                .addValue("set", setId).addValue("now", Timestamp.from(now)), (rs, row) -> {
                    long total = rs.getLong("total"), learned = rs.getLong("learned");
                    return new SetProgressResult(rs.getObject("id", UUID.class), total, learned, rs.getLong("learning"),
                            rs.getLong("needs_review"), total - learned, percent(learned, total));
                });
        return rows.stream().findFirst();
    }

    @Override
    public Optional<RecentResult> recentResult(UUID userId) {
        String sql = "select e.session_id,max(t.name) topic,count(*) total,count(*) filter(where e.correct) correct,max(e.reviewed_at) finished_at "
                + "from flashcard_review_events e left join vocabulary_senses s on s.id=e.vocabulary_sense_id "
                + "left join vocabulary_topics t on t.id=s.topic_id where e.user_id=:user and e.session_id is not null "
                + "group by e.session_id order by max(e.reviewed_at) desc limit 1";
        List<RecentResult> rows = jdbc.query(sql, new MapSqlParameterSource("user", userId), (rs, row) -> {
            long total = rs.getLong("total"), correct = rs.getLong("correct");
            return new RecentResult(rs.getObject("session_id", UUID.class), rs.getString("topic"), correct, total,
                    percent(correct, total), instant(rs, "finished_at"));
        });
        return rows.stream().findFirst();
    }

    @Override
    public AchievementCounts achievementCounts(UUID userId, Instant now) {
        String sql = "with topic_counts as (select t.id,count(s.id) total,count(s.id) filter(where coalesce(f.review_count,0)>0 or coalesce(f.leitner_box,1)>=2) learned "
                + "from vocabulary_topics t join vocabulary_senses s on s.topic_id=t.id and s.status='ACTIVE' "
                + "join vocabulary_items i on i.id=s.vocabulary_item_id and i.status='ACTIVE' "
                + "left join flashcards f on f.vocabulary_sense_id=s.id and f.user_id=:user and f.source_type='VOCABULARY_SENSE' and f.status='ACTIVE' "
                + "where t.status='ACTIVE' group by t.id) "
                + "select (select count(*) from topic_counts where total>0 and learned=total) completed_topics, "
                + "(select count(*) from flashcards where user_id=:user and status='ACTIVE' and source_type='VOCABULARY_SENSE' and leitner_box in (4,5)) mastered_words";
        return jdbc.queryForObject(sql, new MapSqlParameterSource().addValue("user", userId).addValue("now", Timestamp.from(now)),
                (rs, row) -> new AchievementCounts(rs.getLong("completed_topics"), rs.getLong("mastered_words")));
    }

    private int percent(long value, long total) { return total == 0 ? 0 : (int) Math.round(value * 100.0 / total); }
    private Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column); return timestamp == null ? null : timestamp.toInstant();
    }
}
