package com.ieltspath.game.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.repository.RocketRoundRepository;
import com.ieltspath.game.domain.vo.RocketOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RocketRoundRepositoryAdapter implements RocketRoundRepository {
    private static final String ROUND_COLUMNS = """
        id, match_id, sequence, source_word_id, meaning, options::text AS options, correct_option_id, status,
        opens_at, deadline_at, closed_at, close_reason""";
    private static final String ANSWER_COLUMNS = """
        round_id, player_id, option_id, correct, points, received_at, response_ms, request_id""";

    private final JdbcTemplate sql;
    private final ObjectMapper json;
    private final EntityManager entities;

    /** Without JPA, for tests that only use JDBC. */
    public RocketRoundRepositoryAdapter(JdbcTemplate sql, ObjectMapper json) {
        this(sql, json, null);
    }

    @Autowired
    public RocketRoundRepositoryAdapter(JdbcTemplate sql, ObjectMapper json, EntityManager entities) {
        this.sql = sql;
        this.json = json;
        this.entities = entities;
    }

    /** Rounds reference the match and players saved through JPA in the same transaction; write those first. */
    private void flushPendingJpaWrites() {
        if (entities != null && entities.isJoinedToTransaction()) entities.flush();
    }

    private record StoredOption(UUID optionId, UUID sourceWordId, String word) {}

    @Override
    public void createAll(List<RocketRound> rounds) {
        flushPendingJpaWrites();
        sql.batchUpdate("""
            INSERT INTO game_rocket_rounds(id,match_id,sequence,source_word_id,meaning,options,correct_option_id,status,opens_at)
            VALUES (?,?,?,?,?,?::jsonb,?,?,?)
            """, rounds, rounds.size(), (ps, round) -> {
                ps.setObject(1, round.id());
                ps.setObject(2, round.matchId());
                ps.setInt(3, round.sequence());
                ps.setObject(4, round.sourceWordId());
                ps.setString(5, round.meaning());
                ps.setString(6, encode(round.options()));
                ps.setObject(7, round.correctOptionId());
                ps.setString(8, round.status().name());
                ps.setTimestamp(9, timestamp(round.opensAt()));
            });
    }

    @Override
    public void save(RocketRound round) {
        int updated = sql.update("""
            UPDATE game_rocket_rounds SET status=?,opens_at=?,deadline_at=?,closed_at=?,close_reason=?
            WHERE id=? AND NOT is_delete
            """, round.status().name(), timestamp(round.opensAt()), timestamp(round.deadlineAt()),
                timestamp(round.closedAt()), round.closeReason() == null ? null : round.closeReason().name(), round.id());
        if (updated != 1) throw new IllegalStateException("Rocket round " + round.id() + " does not exist");
        List<RocketRound.Answer> answers = List.copyOf(round.answers().values());
        sql.batchUpdate("""
            INSERT INTO game_rocket_answers(id,round_id,match_id,player_id,option_id,correct,points,received_at,response_ms,request_id)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (round_id, player_id) DO NOTHING
            """, answers, answers.size(), (ps, answer) -> {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, round.id());
                ps.setObject(3, round.matchId());
                ps.setObject(4, answer.playerId());
                ps.setObject(5, answer.optionId());
                ps.setBoolean(6, answer.correct());
                ps.setInt(7, answer.points());
                ps.setTimestamp(8, timestamp(answer.receivedAt()));
                ps.setLong(9, answer.responseMs());
                ps.setString(10, answer.requestId());
            });
    }

    @Override
    public List<RocketRound> findByMatch(UUID matchId) {
        Map<UUID, List<RocketRound.Answer>> answers = new HashMap<>();
        sql.query("SELECT " + ANSWER_COLUMNS + " FROM game_rocket_answers WHERE match_id=? AND NOT is_delete ORDER BY created_at",
                rs -> {
                    answers.computeIfAbsent(rs.getObject("round_id", UUID.class), id -> new ArrayList<>()).add(answer(rs));
                }, matchId);
        return sql.query("SELECT " + ROUND_COLUMNS + " FROM game_rocket_rounds WHERE match_id=? AND NOT is_delete ORDER BY sequence",
                (rs, n) -> round(rs, answers.getOrDefault(rs.getObject("id", UUID.class), List.of())), matchId);
    }

    @Override
    public Optional<RocketRound> findForUpdate(UUID roundId) {
        List<RocketRound.Answer> answers = sql.query(
                "SELECT " + ANSWER_COLUMNS + " FROM game_rocket_answers WHERE round_id=? AND NOT is_delete ORDER BY created_at",
                (rs, n) -> answer(rs), roundId);
        return sql.query("SELECT " + ROUND_COLUMNS + " FROM game_rocket_rounds WHERE id=? AND NOT is_delete FOR UPDATE",
                (rs, n) -> round(rs, answers), roundId).stream().findFirst();
    }

    private RocketRound round(ResultSet rs, List<RocketRound.Answer> answers) throws SQLException {
        String reason = rs.getString("close_reason");
        return RocketRound.reconstitute(rs.getObject("id", UUID.class), rs.getObject("match_id", UUID.class),
                rs.getInt("sequence"), rs.getObject("source_word_id", UUID.class), rs.getString("meaning"),
                decode(rs.getString("options")), rs.getObject("correct_option_id", UUID.class),
                RocketRound.Status.valueOf(rs.getString("status")), instant(rs, "opens_at"), instant(rs, "deadline_at"),
                instant(rs, "closed_at"), reason == null ? null : RocketRound.CloseReason.valueOf(reason), answers);
    }

    private static RocketRound.Answer answer(ResultSet rs) throws SQLException {
        return new RocketRound.Answer(rs.getObject("player_id", UUID.class), rs.getObject("option_id", UUID.class),
                rs.getBoolean("correct"), rs.getInt("points"), instant(rs, "received_at"), rs.getLong("response_ms"),
                rs.getString("request_id"));
    }

    private String encode(List<RocketOption> options) {
        try {
            return json.writeValueAsString(options.stream()
                    .map(option -> new StoredOption(option.optionId(), option.sourceWordId(), option.word())).toList());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize Rocket options", e);
        }
    }

    private List<RocketOption> decode(String text) {
        try {
            return json.readValue(text, new TypeReference<List<StoredOption>>() {}).stream()
                    .map(option -> new RocketOption(option.optionId(), option.sourceWordId(), option.word())).toList();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid stored Rocket options", e);
        }
    }

    private static Timestamp timestamp(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
