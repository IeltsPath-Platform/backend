package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.*;
import java.time.Instant;
import java.util.*;

public interface StudyAttemptRepository {
    record AttemptRow(UUID id,UUID userId,UUID topicId,UUID unitId,int revision,String mode,String status,Instant startedAt,
        Instant expiresAt,UUID startRequestId,String startHash,int gradedCount,int firstTryCount,Completion completion) {}
    record StepRow(StudyStepSnapshot snapshot,Instant completedAt,Boolean firstTryCorrect) {}
    record Submission(UUID attemptId,UUID stepId,String hash,StepResult result) {}
    Optional<AttemptRow> find(UUID user,UUID attempt);
    Optional<AttemptRow> findStart(UUID user,UUID request);
    void rememberStart(UUID user,UUID request,UUID attempt,String hash);
    Optional<AttemptRow> active(UUID user,UUID unit);
    List<StepRow> steps(UUID attempt);
    void create(AttemptRow attempt,List<StudyStepSnapshot> steps,Instant now);
    void expire(UUID attempt);
    Optional<Submission> submission(UUID user,UUID request);
    void applyAnswer(UUID attempt,UUID step,boolean correct,boolean first,boolean graded,Instant now);
    void appendSubmission(UUID user,UUID request,String hash,StudyAnswer answer,boolean first,StepResult result,Instant now);
    boolean recordProgress(AttemptRow attempt,int accuracy,Instant now);
    void complete(UUID attempt,Completion result,Instant now);
}
