package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularyReviewResult;
import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.VocabularyReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class VocabularyReviewUseCase {
    private final VocabularyReviewRepository repository;
    public VocabularyReviewUseCase(VocabularyReviewRepository repository) { this.repository = repository; }

    public VocabularyReviewResult.Progress study(UUID user, UUID sense, boolean active) {
        repository.lockUser(user);
        var existing = repository.findProgress(user,sense);
        if (active && !repository.isAvailableSense(sense)) throw new ResourceNotFoundException();
        if (!active && existing.isEmpty()) throw new ResourceNotFoundException();
        Instant now = now();
        var progress = existing.orElseGet(() -> VocabularyProgress.start(user,sense,now)).activate(active);
        repository.save(progress);
        return VocabularyReviewResult.Progress.from(progress);
    }
    public VocabularyReviewResult.Session start(UUID user, UUID request, int limit) {
        if (request == null || limit < 1 || limit > 50) throw new IllegalArgumentException("Invalid review request");
        repository.lockUser(user);
        var prior = repository.findSessionByRequest(user,request);
        if (prior.isPresent()) {
            if (prior.get().limit() != limit) throw new ConflictException();
            return VocabularyReviewResult.Session.from(prior.get());
        }
        Instant now = now();
        var session = new VocabularyReviewRepository.Session(UUID.randomUUID(),user,request,limit,now,
                now.plus(Duration.ofMinutes(30)),repository.dueItems(user,now,limit));
        repository.saveSession(session);
        return VocabularyReviewResult.Session.from(session);
    }
    public VocabularyReviewResult.Progress answer(UUID user, UUID sessionId, UUID request, UUID sense,
                                                  long expectedVersion, VocabularyProgress.Outcome outcome) {
        if (request == null || sense == null || outcome == null || expectedVersion < 0)
            throw new IllegalArgumentException("Invalid answer");
        repository.lockUser(user);
        var receipt = repository.findReceipt(user,request);
        if (receipt.isPresent()) {
            var r = receipt.get();
            if (!r.sessionId().equals(sessionId) || !r.vocabularySenseId().equals(sense)
                    || r.expectedVersion()!=expectedVersion || r.outcome()!=outcome) throw new ConflictException();
            return new VocabularyReviewResult.Progress(sense,r.state(),r.stateVersion(),r.nextReviewAt(),true);
        }
        Instant now = now();
        var session = repository.findSession(user,sessionId).orElseThrow(ResourceNotFoundException::new);
        var item = session.items().stream().filter(i -> i.vocabularySenseId().equals(sense)).findFirst()
                .orElseThrow(ResourceNotFoundException::new);
        if (!now.isBefore(session.expiresAt()) || item.answered() || item.stateVersion()!=expectedVersion)
            throw new ConflictException();
        if (!repository.isAvailableSense(sense)) throw new ResourceNotFoundException();
        var progress = repository.findProgress(user,sense).orElseThrow(ResourceNotFoundException::new)
                .answer(expectedVersion,outcome,now);
        repository.save(progress);
        repository.saveReceipt(new VocabularyReviewRepository.Receipt(user,request,sessionId,sense,expectedVersion,
                outcome,progress.state(),progress.stateVersion(),progress.nextReviewAt(),now));
        return VocabularyReviewResult.Progress.from(progress);
    }
    @Transactional(readOnly=true)
    public VocabularyReviewResult.Statistics statistics(UUID user) {
        Instant now=now();
        var counts=repository.summarize(user,now).buckets();
        Map<VocabularyProgress.State,VocabularyReviewRepository.StateCount> byState=counts.stream()
                .collect(Collectors.toMap(VocabularyReviewRepository.StateCount::state,Function.identity()));
        var buckets=Arrays.stream(VocabularyProgress.State.values()).map(s -> new VocabularyReviewResult.Bucket(s,
                byState.containsKey(s)?byState.get(s).count():0)).toList();
        long total=counts.stream().mapToLong(VocabularyReviewRepository.StateCount::count).sum();
        long retained=byState.containsKey(VocabularyProgress.State.RETAINED)?byState.get(VocabularyProgress.State.RETAINED).count():0;
        var dataFrom=counts.stream().map(VocabularyReviewRepository.StateCount::dataFrom).min(Instant::compareTo)
                .map(t -> t.atZone(ZoneOffset.UTC).toLocalDate()).orElse(null);
        return new VocabularyReviewResult.Statistics("AVAILABLE",now,dataFrom,"VOCABULARY_SENSE",1,"SELF_RATED",
                total,retained,counts.stream().mapToLong(VocabularyReviewRepository.StateCount::dueCount).sum(),
                total==0?null:Math.round(retained*1000.0/total)/10.0,buckets);
    }
    @Transactional(readOnly=true)
    public VocabularyReviewResult.StudyContext context(UUID user, String sourceType, UUID sourceId) {
        if (!"VOCABULARY_REVIEW".equals(sourceType)) throw new IllegalArgumentException("Invalid source type");
        var session=repository.findSession(user,sourceId).orElseThrow(ResourceNotFoundException::new);
        Instant now=now();
        if (!now.isBefore(session.expiresAt()) || !repository.hasReviewableItems(user,sourceId,now))
            throw new ResourceNotFoundException();
        return new VocabularyReviewResult.StudyContext(sourceId,null);
    }
    private static Instant now() { return Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
