package com.ieltspath.learning.infrastructure.adapter;

import com.ieltspath.learning.domain.aggregate.PracticeSession;
import com.ieltspath.learning.domain.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PracticeSessionJdbcAdapter implements PracticeSessionRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<PracticeSession> SESSION = (row, index) -> new PracticeSession(
            row.getObject("id", UUID.class), row.getObject("user_id", UUID.class), row.getString("kind"),
            row.getObject("request_id", UUID.class), row.getTimestamp("created_at").toInstant(),
            row.getTimestamp("expires_at").toInstant());

    @Override
    public Optional<PracticeSession> findByRequest(UUID userId, UUID requestId) {
        return jdbc.query("SELECT * FROM practice_sessions WHERE user_id=? AND request_id=?", SESSION, userId, requestId)
                .stream().findFirst();
    }

    @Override
    public Optional<PracticeSession> findOwned(UUID id, UUID userId) {
        return jdbc.query("SELECT * FROM practice_sessions WHERE id=? AND user_id=?", SESSION, id, userId)
                .stream().findFirst();
    }

    @Override
    public void save(PracticeSession session) {
        jdbc.update("INSERT INTO practice_sessions(id,user_id,kind,request_id,created_at,expires_at) VALUES (?,?,?,?,?,?)",
                session.id(), session.userId(), session.kind(), session.requestId(),
                Timestamp.from(session.createdAt()), Timestamp.from(session.expiresAt()));
    }
}
