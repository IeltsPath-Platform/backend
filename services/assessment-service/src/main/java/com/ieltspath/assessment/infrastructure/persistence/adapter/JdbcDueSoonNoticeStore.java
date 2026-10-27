package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.DueSoonNoticeStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDueSoonNoticeStore implements DueSoonNoticeStore {
    private static final String OPEN_HUMAN_JOB = """
            j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE' AND j.status IN ('QUEUED','PROCESSING')
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDueSoonNoticeStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<DueSoonJob> claimUnreminded(Instant chosenBefore, Instant chosenAfter, int limit) {
        return jdbc.query("""
                SELECT j.id,j.requested_examiner_id,j.skill,j.requested_at,s.attempt_id
                FROM grading_jobs j
                LEFT JOIN learner_submissions l ON l.id=j.submission_id
                LEFT JOIN attempt_items i ON i.id=l.attempt_item_id
                LEFT JOIN attempt_sections s ON s.id=i.attempt_section_id
                WHERE\s""" + OPEN_HUMAN_JOB + """
                  AND j.requested_examiner_id IS NOT NULL AND j.due_soon_notified_at IS NULL
                  AND j.requested_at<=:before AND j.requested_at>:after
                ORDER BY j.requested_at,j.id LIMIT :limit FOR UPDATE OF j SKIP LOCKED
                """, new MapSqlParameterSource("before", Timestamp.from(chosenBefore))
                        .addValue("after", Timestamp.from(chosenAfter)).addValue("limit", limit),
                (row, index) -> new DueSoonJob(row.getObject("id", UUID.class),
                        row.getObject("requested_examiner_id", UUID.class), row.getObject("attempt_id", UUID.class),
                        row.getString("skill"), row.getTimestamp("requested_at").toInstant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markReminded(Collection<UUID> jobIds, Collection<UUID> attemptIds, Instant at) {
        if (jobIds.isEmpty()) return;
        var args = new MapSqlParameterSource("ids", jobIds).addValue("at", Timestamp.from(at));
        String siblings = "";
        if (!attemptIds.isEmpty()) {
            args.addValue("attempts", attemptIds);
            siblings = """
                     OR j.submission_id IN (SELECT l.id FROM learner_submissions l
                         JOIN attempt_items i ON i.id=l.attempt_item_id
                         JOIN attempt_sections s ON s.id=i.attempt_section_id WHERE s.attempt_id IN (:attempts))
                    """;
        }
        jdbc.update("UPDATE grading_jobs j SET due_soon_notified_at=:at WHERE " + OPEN_HUMAN_JOB
                + " AND j.due_soon_notified_at IS NULL AND j.requested_examiner_id IS NOT NULL AND (j.id IN (:ids)"
                + siblings + ")", args);
    }
}
