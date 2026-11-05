package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.result.StudyHeartbeatResult;
import com.ieltspath.learning.domain.aggregate.StudySession;
import com.ieltspath.learning.domain.aggregate.StudySession.Interval;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import com.ieltspath.learning.domain.repository.StudySessionRepository;
import com.ieltspath.learning.domain.repository.StudySessionRepository.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecordStudyHeartbeatUseCase {
    private final StudySessionRepository sessions;
    private final Clock clock;

    @Transactional
    public StudyHeartbeatResult execute(UUID user,UUID id,long epoch,long sequence,List<Interval> intervals,boolean close) {
        if(epoch<1 || sequence<1 || intervals==null || intervals.size()>16) throw new IllegalArgumentException("Invalid heartbeat");
        sessions.lockUser(user);
        var session=sessions.find(user,id).orElseThrow(()->new StudyTrackingException("SESSION_NOT_FOUND"));
        String hash=hash(epoch,sequence,intervals,close);
        var receipt=sessions.receipt(id,epoch,sequence);
        if(receipt.isPresent()) {
            if(!receipt.get().payloadHash().equals(hash)) throw new StudyTrackingException("PAYLOAD_CONFLICT");
            return StudyHeartbeatResult.from(receipt.get());
        }
        var accepted=session.accept(epoch,sequence,intervals,
                StudySession.databasePrecision(clock.instant()),close);
        var next=accepted.session();
        var result=new Receipt(id,epoch,sequence,hash,accepted.acceptedMs(),next.totalAcceptedMs(),next.leaseExpiresAt(),next.status());
        sessions.record(next,result,accepted.segments());
        return StudyHeartbeatResult.from(result);
    }
    private String hash(long epoch,long sequence,List<Interval> intervals,boolean close) {
        StringBuilder canonical=new StringBuilder().append(epoch).append(':').append(sequence).append(':').append(close);
        for(var interval:intervals) canonical.append(':').append(interval.startOffsetMs()).append('-').append(interval.endOffsetMs());
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
