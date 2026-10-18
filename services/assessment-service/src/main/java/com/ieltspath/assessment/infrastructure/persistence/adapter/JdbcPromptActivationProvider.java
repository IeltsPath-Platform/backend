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
}
