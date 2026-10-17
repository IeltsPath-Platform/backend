package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.HumanGradingRequestStore;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanGradingRequestStore implements HumanGradingRequestStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcHumanGradingRequestStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> submittedTopicEssay(UUID userId, UUID attemptId, UUID itemId) {
        return jdbc.query("""
                SELECT l.id
                FROM assessment_attempts a
                JOIN attempt_sections s ON s.attempt_id = a.id
                JOIN attempt_items i ON i.attempt_section_id = s.id
                JOIN learner_submissions l ON l.attempt_item_id = i.id
                WHERE a.id = :attemptId AND a.user_id = :userId AND a.attempt_type = 'TOPIC_GATE'
                  AND a.status = 'SUBMITTED' AND i.id = :itemId AND l.user_id = :userId
                  AND l.status = 'SUBMITTED' AND l.skill = 'WRITING' AND l.text_payload IS NOT NULL
                ORDER BY l.submitted_at DESC, l.id DESC LIMIT 1
                """, Map.of("attemptId", attemptId, "userId", userId, "itemId", itemId),
                (rs, row) -> rs.getObject("id", UUID.class)).stream().findFirst();
    }
}
