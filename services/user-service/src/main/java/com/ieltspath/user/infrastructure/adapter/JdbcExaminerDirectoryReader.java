package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.ExaminerDirectoryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcExaminerDirectoryReader implements ExaminerDirectoryReader {
    /** An active user with the EXAMINER role; the profile row is optional and defaults apply without it. */
    private static final String SELECT = """
            SELECT u.id,
                   COALESCE(NULLIF(BTRIM(lp.display_name), ''), u.full_name) AS display_name,
                   lp.avatar_reference, lp.bio, ep.ielts_band,
                   COALESCE(ep.certificates, '[]'::jsonb)::text AS certificates,
                   COALESCE(ep.accepting_submissions, TRUE) AS accepting_submissions,
                   COALESCE(ep.daily_limit, 30) AS daily_limit
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r ON r.id = ur.role_id AND r.name = 'EXAMINER'
            LEFT JOIN learner_profiles lp ON lp.user_id = u.id
            LEFT JOIN examiner_profiles ep ON ep.user_id = u.id
            WHERE u.status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public List<Examiner> findAccepting() {
        return jdbc.query(SELECT + " AND COALESCE(ep.accepting_submissions, TRUE) ORDER BY display_name, u.id",
                JdbcExaminerDirectoryReader::examiner);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Examiner> findActive(UUID userId) {
        return jdbc.query(SELECT + " AND u.id = :id", new MapSqlParameterSource("id", userId),
                JdbcExaminerDirectoryReader::examiner).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Examiner> findActive(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return List.of();
        return jdbc.query(SELECT + " AND u.id IN (:ids)", new MapSqlParameterSource("ids", userIds),
                JdbcExaminerDirectoryReader::examiner);
    }

    @Override
    @Transactional
    public void saveProfile(UUID userId, BigDecimal ieltsBand, String certificatesJson, boolean acceptingSubmissions) {
        jdbc.update("""
                INSERT INTO examiner_profiles(user_id, ielts_band, certificates, accepting_submissions)
                VALUES (:id, :band, CAST(:certificates AS jsonb), :accepting)
                ON CONFLICT (user_id) DO UPDATE SET ielts_band = EXCLUDED.ielts_band,
                    certificates = EXCLUDED.certificates, accepting_submissions = EXCLUDED.accepting_submissions,
                    updated_at = CURRENT_TIMESTAMP
                """, new MapSqlParameterSource("id", userId).addValue("band", ieltsBand, Types.NUMERIC)
                .addValue("certificates", certificatesJson).addValue("accepting", acceptingSubmissions));
    }

    @Override
    @Transactional
    public boolean saveCapacity(UUID userId, int dailyLimit) {
        if (findActive(userId).isEmpty()) return false;
        jdbc.update("""
                INSERT INTO examiner_profiles(user_id, daily_limit) VALUES (:id, :max)
                ON CONFLICT (user_id) DO UPDATE SET daily_limit = EXCLUDED.daily_limit, updated_at = CURRENT_TIMESTAMP
                """, new MapSqlParameterSource("id", userId).addValue("max", dailyLimit));
        return true;
    }

    private static Examiner examiner(ResultSet row, int index) throws SQLException {
        return new Examiner(row.getObject("id", UUID.class), row.getString("display_name"),
                row.getString("avatar_reference"), row.getString("bio"), row.getBigDecimal("ielts_band"),
                row.getString("certificates"), row.getBoolean("accepting_submissions"), row.getInt("daily_limit"));
    }
}
