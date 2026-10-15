package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import com.ieltspath.library.domain.repository.VocabularyReviewRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class VocabularyReviewResult {
    private VocabularyReviewResult() {}
    public record Progress(UUID vocabularySenseId, VocabularyProgress.State state, long stateVersion,
                           Instant nextReviewAt, boolean active) {
        public static Progress from(VocabularyProgress p) {
            return new Progress(p.vocabularySenseId(),p.state(),p.stateVersion(),p.nextReviewAt(),p.active());
        }
    }
    public record Item(UUID vocabularySenseId, long stateVersion, String lemma, String meaning,
                       String exampleSentence, boolean answered) {}
    public record Session(UUID id, Instant expiresAt, String recallSource, List<Item> items) {
        public static Session from(VocabularyReviewRepository.Session s) {
            return new Session(s.id(),s.expiresAt(),"SELF_RATED", s.items().stream().map(i -> new Item(
                i.vocabularySenseId(),i.stateVersion(),i.lemma(),i.meaning(),i.exampleSentence(),i.answered())).toList());
        }
    }
    public record Bucket(VocabularyProgress.State state, long count) {}
    public record Statistics(String availability, Instant generatedAt, LocalDate dataFrom, String countingUnit,
            int scheduleVersion, String recallSource, long total, long retained, long dueCount,
            Double retainedPercent, List<Bucket> buckets) {}
    public record StudyContext(UUID sourceId, UUID courseId) {}
}
