package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.SalesStaffReader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcSalesStaffReader implements SalesStaffReader {
    /** The cap keeps one answer small; a company has a few dozen sales, never thousands. */
    private static final int LIMIT = 500;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public List<SalesAccount> findAll() {
        return jdbc.query("""
                SELECT u.id,
                       COALESCE(NULLIF(BTRIM(lp.display_name), ''), u.full_name) AS display_name,
                       (u.status = 'ACTIVE') AS active
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id AND r.name = 'SALES_STAFF'
                LEFT JOIN learner_profiles lp ON lp.user_id = u.id
                ORDER BY display_name, u.id
                LIMIT %d
                """.formatted(LIMIT), (rs, row) -> new SalesAccount(
                rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getBoolean("active")));
    }
}
