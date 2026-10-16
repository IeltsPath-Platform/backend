package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.StudySourceVerifier;
import com.ieltspath.user.application.result.StudySessionResult;
import com.ieltspath.user.domain.aggregate.StudySession;
import com.ieltspath.user.domain.exception.StudyTrackingException;
import com.ieltspath.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StartStudySessionUseCase {
    private final StudySessionRepository sessions;
    private final LearnerProfileRepository profiles;
    private final StudySourceVerifier verifier;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public StudySessionResult execute(UUID user,UUID client,String type,UUID source,boolean takeover) {
        Objects.requireNonNull(client); Objects.requireNonNull(source);
        // A replay is read-only and never resurrects, renews, or revalidates a closed session.
        var replay=sessions.findByClient(user,client);
        if(replay.isPresent()) return replay(replay.get(),type,source);
        var context=verifier.verify(type,source);
        if(!source.equals(context.sourceId())) throw new StudyTrackingException("SOURCE_UNAVAILABLE");
        // Network validation is deliberately complete before acquiring any database lock.
        return transactions.execute(tx -> {
            sessions.lockUser(user);
            var concurrentReplay=sessions.findByClient(user,client);
            if(concurrentReplay.isPresent()) return replay(concurrentReplay.get(),type,source);
            var now=StudySession.databasePrecision(clock.instant());
            sessions.findOpen(user).ifPresent(previous -> {
                if(now.isBefore(previous.leaseExpiresAt()) && !takeover) throw new StudyTrackingException("SESSION_IN_USE");
                sessions.save(previous.closeAt(now));
            });
            var session=StudySession.start(user,client,type,source,context.courseId(),StudyTimezone.forUser(profiles,user).getId(),now);
            sessions.save(session);
            return StudySessionResult.from(session,now);
        });
    }
    private StudySessionResult replay(StudySession s,String type,UUID source) {
        if(!s.sourceType().equals(type) || !s.sourceId().equals(source)) throw new StudyTrackingException("PAYLOAD_CONFLICT");
        return StudySessionResult.from(s,StudySession.databasePrecision(clock.instant()));
    }
}
