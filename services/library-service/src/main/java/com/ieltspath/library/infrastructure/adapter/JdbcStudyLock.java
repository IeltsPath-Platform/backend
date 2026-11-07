package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.StudyLock;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;

@Component @RequiredArgsConstructor
public class JdbcStudyLock implements StudyLock {
    private final NamedParameterJdbcTemplate jdbc;
    public void lock(UUID userId) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Study writes require a transaction");
        jdbc.queryForList("select pg_advisory_xact_lock(hashtext('vocab-study:' || :userId))",Map.of("userId",userId.toString()));
    }
}
