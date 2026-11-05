package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.FlashcardReviewEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlashcardReviewEventJdbcAdapter implements FlashcardReviewEventWriter {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void append(UUID userId, UUID flashcardId, UUID vocabularySenseId, UUID sessionId,
                       boolean correct, Instant reviewedAt) {
        jdbc.update("insert into flashcard_review_events(user_id,flashcard_id,vocabulary_sense_id,session_id,correct,reviewed_at) "
                        + "values(:user,:card,:sense,:session,:correct,:reviewedAt)",
                new MapSqlParameterSource().addValue("user", userId).addValue("card", flashcardId)
                        .addValue("sense", vocabularySenseId).addValue("session", sessionId).addValue("correct", correct)
                        .addValue("reviewedAt", Timestamp.from(reviewedAt)));
    }
}
