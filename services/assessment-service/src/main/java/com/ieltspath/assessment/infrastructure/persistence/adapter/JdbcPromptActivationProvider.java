package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.PromptActivationProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPromptActivationProvider implements PromptActivationProvider {
    private final JdbcTemplate jdbc;
    public JdbcPromptActivationProvider(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public String activatedVersion(String task) {
        return jdbc.query("SELECT prompt_version FROM ai_prompt_activations WHERE task = ?", rs ->
                rs.next() ? rs.getString(1) : null, task);
    }
    @Override public Activation activation(String task) {
        return jdbc.query("""
                SELECT a.prompt_version, r.model, r.reasoning_effort
                FROM ai_prompt_activations a LEFT JOIN ai_eval_runs r ON r.id = a.run_id
                WHERE a.task = ?
                """, rs -> rs.next()
                ? new Activation(rs.getString(1), rs.getString(2), rs.getString(3)) : null, task);
    }
}
