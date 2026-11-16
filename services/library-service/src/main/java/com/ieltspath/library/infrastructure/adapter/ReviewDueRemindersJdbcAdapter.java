package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.ReviewDueReminders;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewDueRemindersJdbcAdapter implements ReviewDueReminders {
    private final NamedParameterJdbcTemplate jdbc;

    /** Same "due review" rule as the notebook summary's {@code dueReviewCount}, grouped by learner. */
    @Override
    public List<DueLearner> findDueLearners(Instant now, int minCount, UUID afterUserId, int limit) {
        String sql = """
                select user_id, count(*) due_count
                from flashcards
                where status = 'ACTIVE' and source_type = 'VOCABULARY_SENSE' and user_id > :after
                  and (review_count > 0 or leitner_box >= 2)
                  and (next_review_at is null or next_review_at <= :now)
                group by user_id
                having count(*) >= :min
                order by user_id
                limit :limit
                """;
        var params = new MapSqlParameterSource().addValue("after", afterUserId).addValue("now", Timestamp.from(now))
                .addValue("min", minCount).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, row) ->
                new DueLearner(rs.getObject("user_id", UUID.class), (int) rs.getLong("due_count")));
    }

    @Override
    public boolean queue(UUID eventId, UUID userId, String payload, Instant createdAt) {
        String sql = """
                insert into outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, created_at)
                values (:id, 'Learner', :userId, 'VocabularyReviewDue', cast(:payload as jsonb), 'PENDING', :createdAt)
                on conflict (id) do nothing
                """;
        var params = new MapSqlParameterSource().addValue("id", eventId).addValue("userId", userId)
                .addValue("payload", payload).addValue("createdAt", Timestamp.from(createdAt));
        return jdbc.update(sql, params) == 1;
    }
}
