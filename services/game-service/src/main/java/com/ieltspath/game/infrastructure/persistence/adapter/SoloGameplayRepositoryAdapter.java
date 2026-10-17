package com.ieltspath.game.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.port.SoloGameplayRepository;
import com.ieltspath.game.application.result.SoloReply;
import com.ieltspath.game.domain.exception.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import java.sql.*;
import java.time.*;
import java.util.*;

@Repository
public class SoloGameplayRepositoryAdapter implements SoloGameplayRepository {
    private final JdbcTemplate sql;
    private final ObjectMapper json;

    public SoloGameplayRepositoryAdapter(JdbcTemplate sql, ObjectMapper json) {
        this.sql = sql;
        this.json = json;
    }

    private String encode(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize gameplay result", e);
        }
    }

    private Map<String, Object> decode(String s) {
        try {
            return json.readValue(s, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid saved gameplay response", e);
        }
    }

    private Instant instant(ResultSet r, String column) throws SQLException {
        return r.getTimestamp(column) == null ? null : r.getTimestamp(column).toInstant();
    }

    public State lock(UUID matchId, UUID userId) {
        return sql
                .query(
                        """
SELECT m.*,p.id AS player_id,p.user_id,p.status AS player_status,p.lives_remaining,p.score,p.time_score,p.bonus_score,
 p.current_streak,p.best_streak,p.words_processed,p.words_solved,p.total_solve_time_ms,p.player_display_name
FROM game_matches m JOIN game_match_players p ON p.match_id=m.id
WHERE m.id=? AND p.user_id=? AND NOT m.is_delete AND NOT p.is_delete AND m.game_type='WORDLY' FOR UPDATE OF m,p
""",
                        (r, n) -> {
                            UUID player = r.getObject("player_id", UUID.class);
                            Word word =
                                    sql
                                            .query(
                                                    """
SELECT pw.*,w.sequence_number,w.answer FROM game_player_words pw JOIN game_match_words w ON w.id=pw.match_word_id
WHERE pw.match_player_id=? AND NOT pw.is_delete AND NOT w.is_delete ORDER BY w.sequence_number DESC LIMIT 1 FOR UPDATE OF pw
""",
                                                    (w, i) ->
                                                            new Word(
                                                                    w.getObject("id", UUID.class),
                                                                    w.getInt("sequence_number"),
                                                                    w.getString("answer"),
                                                                    w.getString("status"),
                                                                    w.getInt("attempts_used"),
                                                                    instant(w, "started_at"),
                                                                    instant(w, "deadline_at")),
                                                    player)
                                            .stream()
                                            .findFirst()
                                            .orElse(null);
                            return new State(
                                    matchId,
                                    r.getObject("room_id", UUID.class),
                                    r.getString("status"),
                                    r.getString("player_mode"),
                                    r.getString("play_mode"),
                                    r.getObject("word_limit", Integer.class),
                                    r.getObject("word_time_limit_seconds") != null,
                                    r.getInt("initial_lives"),
                                    instant(r, "started_at"),
                                    instant(r, "ended_at"),
                                    r.getString("end_reason"),
                                    player,
                                    userId,
                                    r.getString("player_display_name"),
                                    r.getString("player_status"),
                                    r.getInt("lives_remaining"),
                                    r.getLong("score"),
                                    r.getLong("time_score"),
                                    r.getLong("bonus_score"),
                                    r.getInt("current_streak"),
                                    r.getInt("best_streak"),
                                    r.getInt("words_processed"),
                                    r.getInt("words_solved"),
                                    r.getLong("total_solve_time_ms"),
                                    word);
                        },
                        matchId,
                        userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new GameMatchNotFoundException(matchId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Receipt receive(UUID matchId, UUID wordId, UUID userId, UUID requestId, String guess) {
        var matches =
                sql.queryForList(
                        """
SELECT m.player_mode,EXISTS(SELECT 1 FROM game_match_word_pool pool WHERE pool.match_id=m.id) AS has_pool FROM game_matches m JOIN game_match_players p ON p.match_id=m.id JOIN game_player_words pw ON pw.match_player_id=p.id
WHERE m.id=? AND p.user_id=? AND pw.id=? AND m.game_type='WORDLY' AND NOT m.is_delete AND NOT p.is_delete AND NOT pw.is_delete FOR SHARE OF m
""",
                        matchId,
                        userId,
                        wordId);
        if (matches.isEmpty()) throw new GameMatchNotFoundException(matchId);
        if (!Boolean.TRUE.equals(matches.get(0).get("has_pool")))
            throw new SoloGameplayException(
                    409,
                    "VOCABULARY_SNAPSHOT_MISSING",
                    "Create a new match with a dictionary snapshot");
        sql.update(
                "INSERT INTO game_guess_receipts(request_id,match_id,player_word_id,user_id,guess)"
                    + " VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                requestId,
                matchId,
                wordId,
                userId,
                guess);
        Receipt receipt = receipt(requestId);
        if (!receipt.matchId().equals(matchId)
                || !receipt.wordId().equals(wordId)
                || !receipt.userId().equals(userId)
                || !receipt.guess().equals(guess))
            throw new SoloGameplayException(
                    409, "REQUEST_ID_CONFLICT", "Request ID was used for a different guess");
        return receipt;
    }

    private Receipt receipt(ResultSet r) throws SQLException {
        return new Receipt(
                r.getObject("request_id", UUID.class),
                r.getObject("match_id", UUID.class),
                r.getObject("player_word_id", UUID.class),
                r.getObject("user_id", UUID.class),
                r.getString("guess"),
                instant(r, "received_at"),
                r.getObject("http_status") == null
                        ? null
                        : new SoloReply(r.getInt("http_status"), decode(r.getString("response"))));
    }

    public Receipt receipt(UUID id) {
        return sql
                .query(
                        "SELECT * FROM game_guess_receipts WHERE request_id=?",
                        (r, n) -> receipt(r),
                        id)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    public List<Receipt> pending(UUID match) {
        return sql.query(
                "SELECT * FROM game_guess_receipts WHERE match_id=? AND http_status IS NULL ORDER"
                    + " BY received_at,request_id LIMIT 100",
                (r, n) -> receipt(r),
                match);
    }

    public void respond(UUID id, SoloReply reply) {
        sql.update(
                "UPDATE game_guess_receipts SET http_status=?,response=?::jsonb WHERE request_id=?"
                    + " AND http_status IS NULL",
                reply.status(),
                encode(reply.body()),
                id);
    }

    public boolean valid(UUID match, String guess) {
        return Boolean.TRUE.equals(
                sql.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM game_match_word_pool WHERE match_id=? AND"
                            + " answer=?)",
                        Boolean.class,
                        match,
                        guess));
    }

    public void guess(
            UUID word,
            UUID request,
            int number,
            String guess,
            List<String> letters,
            boolean correct,
            Instant received) {
        sql.update(
                "INSERT INTO"
                    + " game_guesses(id,player_word_id,request_id,attempt_number,guess,letter_results,is_correct,received_at)"
                    + " VALUES (gen_random_uuid(),?,?,?,?,?::jsonb,?,?)",
                word,
                request,
                number,
                guess,
                encode(letters),
                correct,
                Timestamp.from(received));
    }

    public void closeWord(
            UUID word,
            String status,
            int attempts,
            Instant finished,
            Long solveMs,
            long base,
            long bonus) {
        sql.update(
                "UPDATE game_player_words SET"
                    + " status=?,attempts_used=?,finished_at=?,solve_time_ms=?,time_points=?,bonus_points=?"
                    + " WHERE id=? AND status='ACTIVE'",
                status,
                attempts,
                finished == null ? null : Timestamp.from(finished),
                solveMs,
                base,
                bonus,
                word);
    }

    public void progress(
            State s,
            int lives,
            long score,
            long base,
            long bonus,
            int streak,
            int best,
            int processed,
            int solved,
            long solveMs,
            Instant scoreReceivedAt) {
        sql.update(
                "UPDATE game_match_players SET"
                    + " lives_remaining=?,score_reached_at=CASE WHEN score<>? THEN ? ELSE score_reached_at END,score=?,time_score=?,bonus_score=?,current_streak=?,best_streak=?,words_processed=?,words_solved=?,total_solve_time_ms=?"
                    + " WHERE id=?",
                lives,
                score,
                Timestamp.from(scoreReceivedAt),
                score,
                base,
                bonus,
                streak,
                best,
                processed,
                solved,
                solveMs,
                s.playerId());
    }

    public void advance(State s, Instant now) {
        int sequence = s.word() == null ? 1 : s.word().sequence() + 1;
        var ids =
                sql.queryForList(
                        "SELECT id FROM game_match_words WHERE match_id=? AND sequence_number=? AND"
                            + " NOT is_delete",
                        UUID.class,
                        s.matchId(),
                        sequence);
        UUID word;
        if (ids.isEmpty()) {
            Integer size =
                    sql.queryForObject(
                            "SELECT count(*)::integer FROM game_match_word_pool WHERE match_id=?",
                            Integer.class,
                            s.matchId());
            if (size == null || size == 0)
                throw new SoloGameplayException(
                        409,
                        "VOCABULARY_SNAPSHOT_MISSING",
                        "Create a new match with a vocabulary snapshot");
            word = UUID.randomUUID();
            sql.update(
                    "INSERT INTO"
                        + " game_match_words(id,match_id,sequence_number,source_word_id,answer,meaning)"
                        + " SELECT ?,match_id,?,source_word_id,answer,meaning FROM"
                        + " game_match_word_pool WHERE match_id=? AND position=?",
                    word,
                    sequence,
                    s.matchId(),
                    com.ieltspath.game.domain.vo.WordlyRoundRules.poolPosition(sequence, size));
        } else word = ids.get(0);
        sql.update(
                "INSERT INTO"
                    + " game_player_words(id,match_id,match_player_id,match_word_id,started_at,deadline_at)"
                    + " VALUES (gen_random_uuid(),?,?,?,?,?)",
                s.matchId(),
                s.playerId(),
                word,
                Timestamp.from(now),
                s.timed() ? Timestamp.from(now.plusSeconds(180)) : null);
    }

    public void finish(
            State s, String playerStatus, String reason, Instant now, boolean closeRoom) {
        if ("MULTIPLAYER".equals(s.playerMode())) {
            finishMultiplayer(s, playerStatus, now, closeRoom);
            return;
        }
        sql.update(
                "UPDATE game_match_players SET status=?,finished_at=?,rank=1 WHERE id=?",
                playerStatus,
                Timestamp.from(now),
                s.playerId());
        sql.update(
                "UPDATE game_matches SET status='COMPLETED',end_reason=?,ended_at=? WHERE id=? AND"
                    + " status='IN_PROGRESS'",
                reason,
                Timestamp.from(now),
                s.matchId());
        if (closeRoom) {
            sql.update(
                    "UPDATE game_rooms SET status='CLOSED',is_delete=true,deleted_at=?,updated_at=?"
                        + " WHERE id=?",
                    Timestamp.from(now),
                    Timestamp.from(now),
                    s.roomId());
            sql.update(
                    "UPDATE game_room_members SET status='LEFT',left_at=? WHERE room_id=? AND NOT"
                        + " is_delete",
                    Timestamp.from(now),
                    s.roomId());
        } else {
            sql.update(
                    "UPDATE game_rooms SET status='WAITING',updated_at=? WHERE id=? AND NOT"
                        + " is_delete",
                    Timestamp.from(now),
                    s.roomId());
            sql.update(
                    "UPDATE game_room_members SET status='JOINED',ready_at=NULL WHERE room_id=? AND"
                        + " NOT is_delete AND status IN ('READY','JOINED')",
                    s.roomId());
        }
    }

    private void finishMultiplayer(State s, String playerStatus, Instant now, boolean left) {
        sql.update("UPDATE game_match_players SET status=?,finished_at=?,disconnected_at=NULL,reconnect_deadline_at=NULL WHERE id=?",
                playerStatus, Timestamp.from(now), s.playerId());
        if (left) sql.update("UPDATE game_room_members SET status='LEFT',left_at=?,ready_at=NULL WHERE room_id=? AND user_id=? AND NOT is_delete",
                Timestamp.from(now), s.roomId(), s.userId());
        var counts = sql.queryForMap("""
            SELECT count(*) FILTER (WHERE status IN ('ACTIVE','DISCONNECTED')) AS playing,
                count(*) FILTER (WHERE lives_remaining>0 AND status<>'FORFEITED') AS surviving
            FROM game_match_players WHERE match_id=? AND NOT is_delete
            """, s.matchId());
        long playing = ((Number) counts.get("playing")).longValue();
        long surviving = ((Number) counts.get("surviving")).longValue();
        if (playing > 0 && surviving > 1) return;
        String endReason = surviving == 1 ? "LAST_PLAYER_STANDING" : "ALL_PLAYERS_FINISHED";
        sql.update("""
            UPDATE game_player_words SET status='CANCELLED',finished_at=GREATEST(started_at,?),solve_time_ms=NULL
            WHERE match_id=? AND status='ACTIVE' AND NOT is_delete
            """, Timestamp.from(now), s.matchId());
        sql.update("""
            UPDATE game_match_players SET status='FINISHED',finished_at=?,disconnected_at=NULL,reconnect_deadline_at=NULL
            WHERE match_id=? AND status IN ('ACTIVE','DISCONNECTED') AND NOT is_delete
            """, Timestamp.from(now), s.matchId());
        sql.update("""
            WITH ranked AS (SELECT id,row_number() OVER (
                ORDER BY score DESC,total_solve_time_ms ASC,score_reached_at ASC,joined_at ASC,id ASC) AS position
                FROM game_match_players WHERE match_id=? AND NOT is_delete)
            UPDATE game_match_players p SET rank=ranked.position FROM ranked WHERE p.id=ranked.id
            """, s.matchId());
        sql.update("UPDATE game_matches SET status='COMPLETED',end_reason=?,ended_at=? WHERE id=? AND status='IN_PROGRESS'",
                endReason, Timestamp.from(now), s.matchId());
        boolean hostLeft = Boolean.TRUE.equals(sql.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM game_rooms r JOIN game_room_members rm ON rm.room_id=r.id AND rm.user_id=r.host_user_id
                WHERE r.id=? AND rm.status='LEFT')
            """, Boolean.class, s.roomId()));
        if (hostLeft) {
            sql.update("UPDATE game_rooms SET status='CLOSED',is_delete=true,deleted_at=?,updated_at=? WHERE id=?",
                    Timestamp.from(now), Timestamp.from(now), s.roomId());
            sql.update("UPDATE game_room_members SET status='LEFT',left_at=COALESCE(left_at,?),ready_at=NULL WHERE room_id=? AND NOT is_delete",
                    Timestamp.from(now), s.roomId());
        } else {
            sql.update("UPDATE game_rooms SET status='WAITING',updated_at=? WHERE id=? AND NOT is_delete", Timestamp.from(now), s.roomId());
            sql.update("UPDATE game_room_members SET status='JOINED',ready_at=NULL WHERE room_id=? AND status IN ('READY','JOINED','DISCONNECTED') AND NOT is_delete", s.roomId());
        }
    }

    public String closureMessage(UUID roomId) {
        return sql.query("""
            SELECT COALESCE(rm.display_name,r.host_user_id::text) AS name FROM game_rooms r
            LEFT JOIN game_room_members rm ON rm.room_id=r.id AND rm.user_id=r.host_user_id
            WHERE r.id=? AND r.status='CLOSED'
            """, (r,n) -> "Chủ phòng " + r.getString("name") + " đã đóng phòng", roomId).stream().findFirst().orElse(null);
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Map<String, Object> result(UUID matchId, UUID userId) {
        var rows =
                sql.queryForList(
                        """
SELECT m.*,p.status AS player_status,p.id AS player_id,p.user_id,p.score,p.time_score,p.bonus_score,p.lives_remaining,
 p.words_processed,p.words_solved,p.best_streak,p.total_solve_time_ms,COALESCE(p.player_display_name,rm.display_name) AS display_name,p.rank
FROM game_matches m JOIN game_match_players p ON p.match_id=m.id LEFT JOIN game_room_members rm ON rm.id=p.room_member_id
WHERE m.id=? AND p.user_id=? AND m.game_type='WORDLY' AND NOT m.is_delete AND NOT p.is_delete
""",
                        matchId,
                        userId);
        if (rows.isEmpty()) throw new GameMatchNotFoundException(matchId);
        var r = rows.get(0);
        if (!"COMPLETED".equals(r.get("status")))
            throw new SoloGameplayException(409, "MATCH_NOT_FINISHED", "Match has not finished");
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("initialLives", r.get("initial_lives"));
        settings.put("playMode", r.get("play_mode"));
        settings.put("wordLimit", r.get("word_limit"));
        settings.put("timeEnabled", r.get("word_time_limit_seconds") != null);
        settings.put("wordTimeLimitSeconds", r.get("word_time_limit_seconds"));
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("userId", userId);
        player.put("displayName", r.get("display_name"));
        player.put("status", r.get("player_status"));
        player.put("rank", r.get("rank"));
        player.put("livesRemaining", r.get("lives_remaining"));
        player.put("wordsProcessed", r.get("words_processed"));
        player.put("wordsSolved", r.get("words_solved"));
        player.put("bestStreak", r.get("best_streak"));
        player.put("baseScore", r.get("time_score"));
        player.put("bonusScore", r.get("bonus_score"));
        player.put("totalScore", r.get("score"));
        player.put("totalSolveTimeMs", r.get("total_solve_time_ms"));
        player.put(
                "wordsFailed",
                sql.queryForObject(
                        "SELECT count(*) FROM game_player_words WHERE match_player_id=? AND"
                            + " status='FAILED' AND NOT is_delete",
                        Long.class,
                        r.get("player_id")));
        player.put(
                "wordsTimedOut",
                sql.queryForObject(
                        "SELECT count(*) FROM game_player_words WHERE match_player_id=? AND"
                            + " status='TIMED_OUT' AND NOT is_delete",
                        Long.class,
                        r.get("player_id")));
        var start = asInstant(r.get("started_at"));
        var end = asInstant(r.get("ended_at"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchId", matchId);
        result.put("gameType", "WORDLY");
        result.put("playerMode", r.get("player_mode"));
        result.put("status", "COMPLETED");
        result.put("endReason", r.get("end_reason"));
        result.put("settings", settings);
        result.put("player", player);
        result.put("startedAt", start);
        result.put("endedAt", end);
        result.put("durationMs", Math.max(0, Duration.between(start, end).toMillis()));
        if ("MULTIPLAYER".equals(r.get("player_mode"))) {
            result.put("players", sql.query("""
                SELECT user_id,player_display_name,player_slot_number,rank,status,lives_remaining,
                    score,total_solve_time_ms,words_processed,words_solved,current_streak
                FROM game_match_players WHERE match_id=? AND NOT is_delete ORDER BY rank,id
                """, (row,n) -> new com.ieltspath.game.application.result.WordlyRankingResult.Player(
                    row.getObject("user_id",UUID.class),row.getString("player_display_name"),
                    row.getObject("player_slot_number",Integer.class),row.getObject("rank",Integer.class),
                    row.getString("status"),row.getInt("lives_remaining"),row.getLong("score"),
                    row.getLong("total_solve_time_ms"),row.getInt("words_processed"),row.getInt("words_solved"),
                    row.getInt("current_streak")), matchId));
        }
        return result;
    }

    private Instant asInstant(Object value) {
        return value instanceof Timestamp ts
                ? ts.toInstant()
                : ((java.time.OffsetDateTime) value).toInstant();
    }

    public UUID owner(UUID matchId) {
        return sql.queryForObject(
                "SELECT user_id FROM game_match_players WHERE match_id=? AND NOT is_delete",
                UUID.class,
                matchId);
    }

    public List<UUID> participants(UUID matchId) {
        return sql.queryForList("SELECT user_id FROM game_match_players WHERE match_id=? AND NOT is_delete ORDER BY joined_at,id", UUID.class, matchId);
    }

    public void disconnected(UUID matchId, UUID userId, Instant at) {
        sql.update("""
            UPDATE game_match_players SET status='DISCONNECTED',disconnected_at=?,reconnect_deadline_at=?
            WHERE match_id=? AND user_id=? AND status='ACTIVE' AND NOT is_delete
            """, Timestamp.from(at), Timestamp.from(at.plusSeconds(30)), matchId, userId);
    }

    public boolean reconnectExpired(UUID matchId, UUID userId, Instant at) {
        return Boolean.TRUE.equals(sql.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM game_match_players WHERE match_id=? AND user_id=?
                AND status='DISCONNECTED' AND reconnect_deadline_at<=? AND NOT is_delete)
            """, Boolean.class, matchId, userId, Timestamp.from(at)));
    }

    public void reconnect(UUID matchId, UUID userId, Instant at) {
        sql.update("""
            UPDATE game_match_players SET status='ACTIVE',disconnected_at=NULL,reconnect_deadline_at=NULL
            WHERE match_id=? AND user_id=? AND status='DISCONNECTED' AND reconnect_deadline_at>? AND NOT is_delete
            """, matchId, userId, Timestamp.from(at));
    }

    public List<UUID> due(int limit) {
        return sql.queryForList(
                """
SELECT DISTINCT m.id FROM game_matches m JOIN game_match_players p ON p.match_id=m.id
LEFT JOIN game_player_words pw ON pw.match_player_id=p.id AND pw.status='ACTIVE' AND NOT pw.is_delete
WHERE m.game_type='WORDLY' AND NOT m.is_delete AND NOT p.is_delete AND EXISTS(SELECT 1 FROM game_match_word_pool pool WHERE pool.match_id=m.id)
 AND ((m.status='IN_PROGRESS' AND (pw.deadline_at<=clock_timestamp() OR (p.status='DISCONNECTED' AND p.reconnect_deadline_at<=clock_timestamp()))) OR EXISTS(SELECT 1 FROM game_guess_receipts g WHERE g.match_id=m.id AND g.http_status IS NULL))
LIMIT ?
""",
                UUID.class,
                limit);
    }
}
