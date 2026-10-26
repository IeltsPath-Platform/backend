package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.application.result.NotebookSummaryResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VocabularyNotebookJdbcAdapter implements VocabularyNotebookReader {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public PageResult<NotebookEntryResult> findEntries(UUID userId, NotebookQuery query, Instant now) {
        StringBuilder where = new StringBuilder(" where f.user_id=:user and f.status='ACTIVE' and f.source_type='VOCABULARY_SENSE'");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("user", userId).addValue("now", java.sql.Timestamp.from(now));
        if (query.q() != null && !query.q().isBlank()) {
            where.append(" and (v.lemma ilike :q or s.vietnamese_meaning ilike :q or coalesce(s.english_definition,'') ilike :q)");
            params.addValue("q", "%" + query.q().trim() + "%");
        }
        if (query.levels() != null && !query.levels().isEmpty()) {
            List<Integer> boxes = query.levels().stream().flatMap(level -> level == UnderstandingLevel.MASTERED
                    ? java.util.stream.Stream.of(4, 5) : java.util.stream.Stream.of(level.targetBox())).toList();
            where.append(" and f.leitner_box in (:boxes)"); params.addValue("boxes", boxes);
        }
        if (Boolean.TRUE.equals(query.due())) where.append(" and (f.next_review_at is null or f.next_review_at<=:now)");
        if (Boolean.FALSE.equals(query.due())) where.append(" and f.next_review_at>:now");
        String sort = switch (query.sort()) {
            case "DUE" -> "f.next_review_at asc nulls first, f.id asc";
            case "ALPHABET" -> "v.lemma asc, f.id asc";
            case "WEAKEST" -> "f.leitner_box asc, f.next_review_at asc nulls first, f.id asc";
            default -> "f.created_at desc, f.id desc";
        };
        Long total = jdbc.queryForObject("select count(*) from flashcards f join vocabulary_senses s on s.id=f.vocabulary_sense_id join vocabulary_items v on v.id=s.vocabulary_item_id" + where,
                params, Long.class);
        String sql = "select f.id flashcard_id,f.vocabulary_sense_id,s.vocabulary_item_id,v.lemma,v.ipa,v.pronunciation_audio_reference,s.part_of_speech,s.vietnamese_meaning,s.english_definition,coalesce(f.example_sentence,s.example_sentence) example_sentence,s.image_url,f.leitner_box,f.review_count,f.last_reviewed_at,f.next_review_at,f.created_at,n.body note "
                + "from flashcards f join vocabulary_senses s on s.id=f.vocabulary_sense_id join vocabulary_items v on v.id=s.vocabulary_item_id "
                + "left join lateral (select body from notes where user_id=f.user_id and source_type='VOCABULARY_SENSE' and source_reference_id=f.vocabulary_sense_id and status='ACTIVE' order by updated_at desc,id desc limit 1) n on true"
                + where + " order by " + sort + " limit :limit offset :offset";
        params.addValue("limit", query.size()).addValue("offset", (long) query.page() * query.size());
        List<NotebookEntryResult> entries = jdbc.query(sql, params, this::entry);
        return new PageResult<>(entries, query.page(), query.size(), total == null ? 0 : total);
    }

    @Override
    public java.util.Optional<NotebookEntryResult> findEntry(UUID userId, UUID flashcardId) {
        String sql = "select f.id flashcard_id,f.vocabulary_sense_id,s.vocabulary_item_id,v.lemma,v.ipa,v.pronunciation_audio_reference,s.part_of_speech,s.vietnamese_meaning,s.english_definition,coalesce(f.example_sentence,s.example_sentence) example_sentence,s.image_url,f.leitner_box,f.review_count,f.last_reviewed_at,f.next_review_at,f.created_at,n.body note "
                + "from flashcards f join vocabulary_senses s on s.id=f.vocabulary_sense_id join vocabulary_items v on v.id=s.vocabulary_item_id "
                + "left join lateral (select body from notes where user_id=f.user_id and source_type='VOCABULARY_SENSE' and source_reference_id=f.vocabulary_sense_id and status='ACTIVE' order by updated_at desc,id desc limit 1) n on true "
                + "where f.user_id=:user and f.id=:card and f.status='ACTIVE' and f.source_type='VOCABULARY_SENSE'";
        var rows = jdbc.query(sql, new MapSqlParameterSource().addValue("user", userId).addValue("card", flashcardId), this::entry);
        return rows.stream().findFirst();
    }

    @Override
    public NotebookSummaryResult summarize(UUID userId, Instant now, Instant today, Instant tomorrow) {
        String sql = "select count(*) total, count(*) filter(where leitner_box=1) unknown, count(*) filter(where leitner_box=2) familiar, count(*) filter(where leitner_box=3) learning, count(*) filter(where leitner_box in (4,5)) mastered, count(*) filter(where next_review_at is null or next_review_at<=:now) due_now, count(*) filter(where created_at>=:seven) added_last7, count(*) filter(where last_reviewed_at>=:today and last_reviewed_at<:tomorrow) reviewed_today, min(next_review_at) filter(where next_review_at>:now) next_due from flashcards where user_id=:user and status='ACTIVE' and source_type='VOCABULARY_SENSE'";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("user", userId).addValue("now", java.sql.Timestamp.from(now))
                .addValue("seven", java.sql.Timestamp.from(now.minusSeconds(7 * 86400L)))
                .addValue("today", java.sql.Timestamp.from(today)).addValue("tomorrow", java.sql.Timestamp.from(tomorrow));
        return jdbc.queryForObject(sql, p, (rs, row) -> new NotebookSummaryResult(rs.getLong("total"), rs.getLong("unknown"),
                rs.getLong("familiar"), rs.getLong("learning"), rs.getLong("mastered"), rs.getLong("due_now"),
                rs.getLong("added_last7"), rs.getLong("reviewed_today"), rs.getTimestamp("next_due") == null ? null : rs.getTimestamp("next_due").toInstant()));
    }

    @Override
    public List<UUID> findSavedSenseIds(UUID userId, Collection<UUID> senseIds) {
        if (senseIds == null || senseIds.isEmpty()) return List.of();
        return jdbc.query("select distinct vocabulary_sense_id from flashcards where user_id=:user and source_type='VOCABULARY_SENSE' and status<>'DELETED' and vocabulary_sense_id in (:ids)",
                new MapSqlParameterSource().addValue("user", userId).addValue("ids", senseIds), (rs, row) -> rs.getObject(1, UUID.class));
    }

    private NotebookEntryResult entry(ResultSet rs, int row) throws SQLException {
        int box = rs.getInt("leitner_box");
        return new NotebookEntryResult(rs.getObject("flashcard_id", UUID.class), rs.getObject("vocabulary_sense_id", UUID.class),
                rs.getObject("vocabulary_item_id", UUID.class), rs.getString("lemma"), rs.getString("ipa"),
                rs.getString("pronunciation_audio_reference"), rs.getString("part_of_speech"), rs.getString("vietnamese_meaning"),
                rs.getString("english_definition"), rs.getString("example_sentence"), rs.getString("image_url"),
                UnderstandingLevel.fromBox(box), box, rs.getInt("review_count"), instant(rs, "last_reviewed_at"),
                instant(rs, "next_review_at"), instant(rs, "created_at"), rs.getString("note"));
    }
    private Instant instant(ResultSet rs, String name) throws SQLException {
        var timestamp = rs.getTimestamp(name); return timestamp == null ? null : timestamp.toInstant();
    }
}
